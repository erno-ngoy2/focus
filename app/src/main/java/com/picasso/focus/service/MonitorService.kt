package com.picasso.focus.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.picasso.focus.MainActivity
import com.picasso.focus.R
import com.picasso.focus.data.FocusController
import com.picasso.focus.data.FocusState
import com.picasso.focus.data.Fmt
import com.picasso.focus.data.History
import com.picasso.focus.data.NetSnapshot
import com.picasso.focus.data.Prefs
import com.picasso.focus.net.NetCounters
import com.picasso.focus.net.RootCut
import com.picasso.focus.net.SystemControls
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Service de surveillance à très faible coût :
 *  - sans hotspot et sans mode concentration, il ne fait qu'un contrôle toutes les 45 s sur l'horloge « uptime »
 *    (ne réveille jamais le CPU) + réagit aux diffusions système ;
 *  - les compteurs viennent du noyau (TrafficStats), la précision ne dépend donc pas de la fréquence ;
 *  - écran éteint : échantillonnage toutes les 15 s seulement, aucune mise à jour de notification.
 */
class MonitorService : Service() {
    companion object {
        const val ACTION_EXTEND = "focus.EXTEND"
        const val ACTION_RESET = "focus.RESET"
        const val ACTION_FOCUS_END = "focus.FOCUS_END"
        const val EXTEND_STEP = 500_000_000L

        fun start(ctx: Context) {
            try {
                ctx.startForegroundService(Intent(ctx, MonitorService::class.java))
            } catch (_: Exception) {
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var nm: NotificationManager

    private var screenOn = true
    private var hotspotOn = false
    private var lastAt = 0L
    private var prevMob = NetCounters.Raw(0, 0)
    private var prevTot = NetCounters.Raw(0, 0)
    private var baseRx = 0L
    private var baseTx = 0L
    private var sessionStartMs = 0L
    private var warned = false
    private var alerted = false
    private var lastAlertAt = 0L
    private var lastCheckpoint = 0L
    private var foregrounded = false
    private var focusRunnable: Runnable? = null
    private var tetherIface = ""
    private var tetherIp = ""

    private val tick = object : Runnable {
        override fun run() {
            sample()
            handler.postDelayed(this, interval())
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            when (i.action) {
                Intent.ACTION_SCREEN_ON -> { screenOn = true; reschedule(300) }
                Intent.ACTION_SCREEN_OFF -> { screenOn = false; reschedule(interval()) }
                "android.net.wifi.WIFI_AP_STATE_CHANGED" -> {
                    NetCounters.apEnabled = i.getIntExtra("wifi_state", -1) == 13
                    reschedule(1500)
                }
                "android.net.conn.TETHER_STATE_CHANGED" -> {
                    NetCounters.tetherActive = !i.getStringArrayListExtra("tetherArray").isNullOrEmpty()
                    reschedule(1500)
                }
                else -> reschedule(1500)
            }
        }
    }

    private fun interval(): Long = when {
        hotspotOn -> if (!screenOn) 15_000L else if (FocusState.liveRequested.value) 2_000L else 3_000L
        FocusState.liveRequested.value && screenOn -> 2_000L
        else -> 45_000L
    }

    private fun reschedule(delayMs: Long) {
        handler.removeCallbacks(tick)
        handler.postDelayed(tick, delayMs)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureForeground() // doit passer en premier plan immédiatement, sinon Android tue l'appli
        try { screenOn = (getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive } catch (_: Exception) {}
        for (a in listOf(Intent.ACTION_SCREEN_ON, Intent.ACTION_SCREEN_OFF, "android.net.wifi.WIFI_AP_STATE_CHANGED", "android.net.conn.TETHER_STATE_CHANGED")) {
            try {
                val sticky = ContextCompat.registerReceiver(this, receiver, IntentFilter(a), ContextCompat.RECEIVER_EXPORTED)
                if (sticky != null && a != Intent.ACTION_SCREEN_ON && a != Intent.ACTION_SCREEN_OFF) receiver.onReceive(this, sticky)
            } catch (_: Exception) {
            }
        }
        try { restoreSession() } catch (_: Exception) {}
        scope.launch { FocusState.liveRequested.collect { reschedule(200) } }
        scope.launch { FocusState.session.collect { onFocusChanged() } }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureForeground()
        when (intent?.action) {
            ACTION_EXTEND -> {
                Prefs.limitBytes = Prefs.limitBytes + EXTEND_STEP
                Prefs.limitEnabled = true
                alerted = false
                Prefs.alerted = false
                nm.cancel(Notif.ID_ALERT)
                reschedule(100)
            }
            ACTION_RESET -> {
                val m = NetCounters.mobile()
                baseRx = m.rx; baseTx = m.tx
                Prefs.baseRx = m.rx; Prefs.baseTx = m.tx
                warned = false; alerted = false
                nm.cancel(Notif.ID_ALERT)
                reschedule(100)
            }
            ACTION_FOCUS_END -> {
                val s = FocusState.session.value
                if (s.active && s.durationMs > 0 && System.currentTimeMillis() >= s.startEpochMs + s.durationMs - 1000) finishFocus()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        try { unregisterReceiver(receiver) } catch (_: Exception) {}
        scope.cancel()
        super.onDestroy()
    }

    // ---------------------------------------------------------------- session hotspot

    private fun restoreSession() {
        val st = Prefs.sessionStart
        if (st == 0L) return
        val m = NetCounters.mobile()
        if (NetCounters.hotspotActive(this) && m.rx >= Prefs.baseRx && m.tx >= Prefs.baseTx) {
            hotspotOn = true
            baseRx = Prefs.baseRx; baseTx = Prefs.baseTx
            sessionStartMs = st
            warned = Prefs.warned; alerted = Prefs.alerted
        } else {
            val bytes = ((Prefs.lastRx - Prefs.baseRx).coerceAtLeast(0) + (Prefs.lastTx - Prefs.baseTx).coerceAtLeast(0))
            History.addNet(History.day(st), bytes)
            Prefs.lastSessionBytes = bytes
            Prefs.sessionStart = 0
        }
    }

    private fun beginSession(m: NetCounters.Raw) {
        baseRx = m.rx; baseTx = m.tx
        sessionStartMs = System.currentTimeMillis()
        warned = false; alerted = false
        Prefs.sessionStart = sessionStartMs
        Prefs.baseRx = m.rx; Prefs.baseTx = m.tx
        Prefs.lastRx = m.rx; Prefs.lastTx = m.tx
        Prefs.warned = false; Prefs.alerted = false
        nm.cancel(Notif.ID_ALERT)
    }

    private fun endSession(m: NetCounters.Raw) {
        val bytes = ((m.rx - baseRx).coerceAtLeast(0) + (m.tx - baseTx).coerceAtLeast(0))
        History.addNet(History.day(sessionStartMs), bytes)
        Prefs.lastSessionBytes = bytes
        Prefs.sessionStart = 0
        sessionStartMs = 0
        nm.cancel(Notif.ID_ALERT)
    }

    // ---------------------------------------------------------------- échantillonnage

    private fun sample() {
        val now = SystemClock.elapsedRealtime()
        val hs = NetCounters.hotspotActive(this)
        val mob = NetCounters.mobile()
        val tot = NetCounters.total()

        if (hs && !hotspotOn) beginSession(mob)
        if (!hs && hotspotOn) endSession(mob)
        hotspotOn = hs

        var rxS = 0L
        var txS = 0L
        if (lastAt != 0L && now - lastAt in 500..12_000) {
            val dt = (now - lastAt) / 1000.0
            val cur = if (hs) mob else tot
            val prev = if (hs) prevMob else prevTot
            rxS = ((cur.rx - prev.rx) / dt).toLong().coerceAtLeast(0)
            txS = ((cur.tx - prev.tx) / dt).toLong().coerceAtLeast(0)
        }
        lastAt = now
        prevMob = mob
        prevTot = tot

        var sRx = 0L
        var sTx = 0L
        if (hs) {
            if (mob.rx < baseRx || mob.tx < baseTx) { // compteurs remis à zéro (redémarrage)
                baseRx = mob.rx; baseTx = mob.tx
                Prefs.baseRx = baseRx; Prefs.baseTx = baseTx
            }
            sRx = mob.rx - baseRx
            sTx = mob.tx - baseTx
            val info = NetCounters.tetherInfo()
            tetherIface = info?.first ?: ""
            tetherIp = info?.second ?: ""
        } else {
            tetherIface = ""; tetherIp = ""
        }

        val limit = if (Prefs.limitEnabled) Prefs.limitBytes else 0L
        val total = sRx + sTx
        val over = hs && limit > 0 && total >= limit

        FocusState.net.value = NetSnapshot(
            hotspotOn = hs, rxBytes = sRx, txBytes = sTx, rxSpeed = rxS, txSpeed = txS,
            sessionStartMs = sessionStartMs, limitBytes = limit, limitReached = over,
            lastSessionBytes = Prefs.lastSessionBytes, tetherIface = tetherIface, tetherIp = tetherIp
        )

        if (hs && limit > 0) checkLimit(total, limit, now)
        if (hs && now - lastCheckpoint > 60_000) {
            lastCheckpoint = now
            Prefs.lastRx = mob.rx; Prefs.lastTx = mob.tx
        }

        if (screenOn || !foregrounded) updateNotification(FocusState.net.value)
        stopIfIdle()
    }

    private fun checkLimit(total: Long, limit: Long, now: Long) {
        if (total >= limit) {
            if (!alerted || now - lastAlertAt > 120_000) {
                alerted = true; Prefs.alerted = true
                lastAlertAt = now
                postAlert(true, total, limit)
                if (Prefs.autoCutRoot) RootCut.stopHotspot()
            }
        } else {
            if (alerted) { alerted = false; Prefs.alerted = false; nm.cancel(Notif.ID_ALERT) }
            val warnAt = limit * Prefs.warnPercent / 100
            if (Prefs.warnEnabled && !warned && total >= warnAt) {
                warned = true; Prefs.warned = true
                postAlert(false, total, limit)
            } else if (warned && total < warnAt) {
                warned = false; Prefs.warned = false
            }
        }
    }

    // ---------------------------------------------------------------- notifications

    private fun openApp(tab: Int): PendingIntent =
        Notif.activityPi(this, Intent(this, MainActivity::class.java).putExtra("tab", tab).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 10 + tab)

    private fun ensureForeground() {
        if (foregrounded) return
        val n = buildNotification(FocusState.net.value)
        try {
            if (Build.VERSION.SDK_INT >= 34) startForeground(Notif.ID_MONITOR, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            else startForeground(Notif.ID_MONITOR, n)
            foregrounded = true
        } catch (_: Exception) {
            // Repli minimal : ne jamais rester sans startForeground() après startForegroundService().
            try {
                val fallbackNotif = Notification.Builder(this, Notif.CH_WATCH).setSmallIcon(R.drawable.ic_stat_f).setContentTitle("Focus").build()
                if (Build.VERSION.SDK_INT >= 34) startForeground(Notif.ID_MONITOR, fallbackNotif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                else startForeground(Notif.ID_MONITOR, fallbackNotif)
                foregrounded = true
            } catch (_: Exception) {
                stopSelf()
            }
        }
    }

    private fun updateNotification(s: NetSnapshot) {
        if (!foregrounded) { ensureForeground(); return }
        nm.notify(Notif.ID_MONITOR, buildNotification(s))
    }

    private fun buildNotification(s: NetSnapshot): Notification {
        val focus = FocusState.session.value
        if (!s.hotspotOn) {
            val text = when {
                focus.active -> "Mode concentration actif"
                else -> "Surveillance du point d'accès"
            }
            return Notification.Builder(this, Notif.CH_WATCH)
                .setSmallIcon(R.drawable.ic_stat_f)
                .setContentTitle("Focus")
                .setContentText(text)
                .setOngoing(true)
                .setShowWhen(false)
                .setColor(Notif.ORANGE)
                .setContentIntent(openApp(0))
                .build()
        }
        val sp = Fmt.speedParts(s.rxSpeed + s.txSpeed)
        val icon: Icon = if (Prefs.speedInStatusBar) Notif.speedIcon(sp.first, sp.second) else Icon.createWithResource(this, R.drawable.ic_stat_f)
        val title = if (s.limitBytes > 0) "Hotspot · ${Fmt.bytes(s.total)} / ${Fmt.bytes(s.limitBytes)}" else "Hotspot · ${Fmt.bytes(s.total)}"
        val b = Notification.Builder(this, Notif.CH_METER)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText("↓ ${Fmt.speed(s.rxSpeed)}   ↑ ${Fmt.speed(s.txSpeed)}")
            .setSubText(Fmt.clock(System.currentTimeMillis() - s.sessionStartMs))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(Notification.CATEGORY_STATUS)
            .setColor(Notif.ORANGE)
            .setContentIntent(openApp(1))
        if (s.limitBytes > 0) b.setProgress(1000, (s.progress * 1000).toInt(), false)
        b.addAction(Notification.Action.Builder(Icon.createWithResource(this, R.drawable.ic_stat_f), "Couper", cutPi()).build())
        if (s.limitBytes > 0) {
            b.addAction(Notification.Action.Builder(Icon.createWithResource(this, R.drawable.ic_stat_f), "+500 Mo", Notif.servicePi(this, MonitorService::class.java, ACTION_EXTEND, 21)).build())
        }
        return b.build()
    }

    private fun cutPi(): PendingIntent {
        val i = SystemControls.hotspotIntents().firstOrNull { it.resolveActivity(packageManager) != null }
            ?: Intent(android.provider.Settings.ACTION_SETTINGS)
        return Notif.activityPi(this, i, 20)
    }

    private fun postAlert(over: Boolean, total: Long, limit: Long) {
        val title = if (over) "Limite atteinte · coupe ton hotspot" else "${Prefs.warnPercent} % de la limite atteinte"
        val b = Notification.Builder(this, Notif.CH_ALERT)
            .setSmallIcon(R.drawable.ic_stat_f)
            .setContentTitle(title)
            .setContentText("${Fmt.bytes(total)} consommés sur ${Fmt.bytes(limit)}")
            .setColor(Notif.ORANGE)
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(openApp(1))
        b.addAction(Notification.Action.Builder(Icon.createWithResource(this, R.drawable.ic_stat_f), "Couper le hotspot", cutPi()).build())
        b.addAction(Notification.Action.Builder(Icon.createWithResource(this, R.drawable.ic_stat_f), "+500 Mo", Notif.servicePi(this, MonitorService::class.java, ACTION_EXTEND, 21)).build())
        try { nm.notify(Notif.ID_ALERT, b.build()) } catch (_: SecurityException) {}
    }

    // ---------------------------------------------------------------- concentration

    private fun onFocusChanged() {
        focusRunnable?.let { handler.removeCallbacks(it) }
        focusRunnable = null
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getService(
            this, 30, Intent(this, MonitorService::class.java).setAction(ACTION_FOCUS_END),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val s = FocusState.session.value
        if (s.active && s.durationMs > 0) {
            val remain = (s.startEpochMs + s.durationMs - System.currentTimeMillis()).coerceAtLeast(0)
            val r = Runnable { finishFocus() }
            focusRunnable = r
            handler.postDelayed(r, remain)
            try { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, s.startEpochMs + s.durationMs, pi) } catch (_: Exception) {}
        } else {
            am.cancel(pi)
        }
        if (foregrounded) updateNotification(FocusState.net.value)
        reschedule(200)
    }

    private fun finishFocus() {
        val s = FocusState.session.value
        if (!s.active) return
        FocusController.complete(this)
        val n = Notification.Builder(this, Notif.CH_FOCUS)
            .setSmallIcon(R.drawable.ic_stat_f)
            .setContentTitle("Session terminée")
            .setContentText("Bravo, ${s.durationMs / 60_000} min de concentration.")
            .setColor(Notif.ORANGE)
            .setAutoCancel(true)
            .setContentIntent(openApp(0))
            .build()
        try { nm.notify(Notif.ID_FOCUS_DONE, n) } catch (_: SecurityException) {}
        stopIfIdle()
    }

    private fun stopIfIdle() {
        val needed = hotspotOn || Prefs.watchHotspot || FocusState.session.value.active || FocusState.liveRequested.value
        if (!needed) stopSelf()
    }
}
