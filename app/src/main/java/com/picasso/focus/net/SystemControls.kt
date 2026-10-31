package com.picasso.focus.net

import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings

/**
 * Android interdit à une application tierce d'activer/couper le hotspot, le Wi‑Fi ou le Bluetooth
 * directement : on ouvre donc le bon panneau système en un tap, et on lit l'état réel.
 */
object SystemControls {
    fun isWifiOn(ctx: Context): Boolean = try {
        (ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).isWifiEnabled
    } catch (_: Exception) {
        false
    }

    fun isBluetoothOn(ctx: Context): Boolean = try {
        (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter?.isEnabled == true
    } catch (_: Exception) {
        false
    }

    fun dndGranted(ctx: Context): Boolean =
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).isNotificationPolicyAccessGranted

    fun dndOn(ctx: Context): Boolean {
        val f = (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).currentInterruptionFilter
        return f != NotificationManager.INTERRUPTION_FILTER_ALL && f != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
    }

    fun setDnd(ctx: Context, on: Boolean) {
        if (!dndGranted(ctx)) {
            start(ctx, Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            return
        }
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.setInterruptionFilter(if (on) NotificationManager.INTERRUPTION_FILTER_PRIORITY else NotificationManager.INTERRUPTION_FILTER_ALL)
    }

    fun hotspotIntents(): List<Intent> = listOf(
        Intent().setClassName("com.android.settings", "com.android.settings.TetherSettings"),
        Intent().setClassName("com.android.settings", "com.android.settings.Settings\$TetherSettingsActivity"),
        Intent().setClassName("com.android.settings", "com.android.settings.wifi.tether.WifiTetherSettings"),
        Intent(Settings.ACTION_WIRELESS_SETTINGS)
    )

    fun openHotspot(ctx: Context) {
        for (i in hotspotIntents()) if (start(ctx, i)) return
        start(ctx, Intent(Settings.ACTION_SETTINGS))
    }

    fun openWifi(ctx: Context) {
        val i = if (Build.VERSION.SDK_INT >= 29) Intent(Settings.Panel.ACTION_WIFI) else Intent(Settings.ACTION_WIFI_SETTINGS)
        start(ctx, i)
    }

    fun openBluetooth(ctx: Context) {
        start(ctx, Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    }

    fun openMobileData(ctx: Context) {
        val i = if (Build.VERSION.SDK_INT >= 29) Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY) else Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
        start(ctx, i)
    }

    fun openDevOptions(ctx: Context) {
        start(ctx, Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
    }

    fun copy(ctx: Context, text: String) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Focus", text))
    }

    fun start(ctx: Context, intent: Intent): Boolean = try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    } catch (_: Exception) {
        false
    }
}

/** Coupure automatique : uniquement sur appareil rooté (meilleur effort, commandes selon la version d'Android). */
object RootCut {
    fun stopHotspot() {
        Thread {
            for (cmd in listOf("cmd wifi stop-softap", "cmd connectivity stop-tethering wifi")) {
                try {
                    val p = ProcessBuilder("su", "-c", cmd).redirectErrorStream(true).start()
                    p.waitFor(4, java.util.concurrent.TimeUnit.SECONDS)
                    p.destroy()
                } catch (_: Throwable) {
                }
            }
        }.start()
    }
}
