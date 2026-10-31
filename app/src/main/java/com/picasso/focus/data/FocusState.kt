package com.picasso.focus.data

import android.app.NotificationManager
import android.content.Context
import com.picasso.focus.service.MonitorService
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.atomic.AtomicInteger

data class NetSnapshot(
    val hotspotOn: Boolean = false,
    val rxBytes: Long = 0,
    val txBytes: Long = 0,
    val rxSpeed: Long = 0,
    val txSpeed: Long = 0,
    val sessionStartMs: Long = 0,
    val limitBytes: Long = 0, // 0 = illimité
    val limitReached: Boolean = false,
    val lastSessionBytes: Long = 0,
    val tetherIface: String = "",
    val tetherIp: String = ""
) {
    val total: Long get() = rxBytes + txBytes
    val remaining: Long get() = if (limitBytes > 0) (limitBytes - total).coerceAtLeast(0) else -1
    val progress: Float get() = if (limitBytes > 0) (total.toFloat() / limitBytes).coerceIn(0f, 1f) else 0f
}

data class FocusSession(
    val active: Boolean = false,
    val finished: Boolean = false,
    val startEpochMs: Long = 0,
    val durationMs: Long = 0 // 0 = libre
)

object FocusState {
    val net = MutableStateFlow(NetSnapshot())
    val session = MutableStateFlow(FocusSession())
    val alwaysOrange = MutableStateFlow(false)
    val liveRequested = MutableStateFlow(false)
    private val liveCount = AtomicInteger(0)

    fun acquireLive(ctx: Context) {
        liveCount.incrementAndGet()
        liveRequested.value = true
        MonitorService.start(ctx)
    }

    fun releaseLive() {
        if (liveCount.decrementAndGet() <= 0) {
            liveCount.set(0)
            liveRequested.value = false
        }
    }
}

object Dnd {
    private fun nm(ctx: Context) = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun enter(ctx: Context) {
        val n = nm(ctx)
        if (!n.isNotificationPolicyAccessGranted) return
        if (Prefs.prevFilter < 0) Prefs.prevFilter = n.currentInterruptionFilter
        n.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
    }

    fun exit(ctx: Context) {
        val n = nm(ctx)
        if (Prefs.prevFilter >= 0 && n.isNotificationPolicyAccessGranted) {
            n.setInterruptionFilter(Prefs.prevFilter.coerceAtLeast(NotificationManager.INTERRUPTION_FILTER_ALL))
        }
        Prefs.prevFilter = -1
    }
}

object FocusController {
    fun restore(ctx: Context) {
        FocusState.alwaysOrange.value = Prefs.alwaysOrange
        val st = Prefs.focusStart
        if (st != 0L) {
            val d = Prefs.focusDur
            if (d > 0 && System.currentTimeMillis() >= st + d) {
                FocusState.session.value = FocusSession(true, false, st, d)
                complete(ctx)
            } else FocusState.session.value = FocusSession(true, false, st, d)
        }
    }

    fun start(ctx: Context, minutes: Int) {
        val now = System.currentTimeMillis()
        val dur = minutes * 60_000L
        Prefs.focusStart = now
        Prefs.focusDur = dur
        FocusState.session.value = FocusSession(true, false, now, dur)
        if (Prefs.dnd) Dnd.enter(ctx)
        MonitorService.start(ctx)
    }

    private fun commit(s: FocusSession) {
        val end = if (s.durationMs > 0) minOf(System.currentTimeMillis(), s.startEpochMs + s.durationMs) else System.currentTimeMillis()
        History.addFocus(History.day(s.startEpochMs), ((end - s.startEpochMs) / 1000).coerceAtLeast(0))
    }

    /** Arrêt manuel. */
    fun stop(ctx: Context) {
        val s = FocusState.session.value
        if (!s.active) return
        commit(s)
        Prefs.focusStart = 0
        Dnd.exit(ctx)
        FocusState.session.value = FocusSession()
    }

    /** Fin naturelle du minuteur (idempotent). */
    fun complete(ctx: Context) {
        val s = FocusState.session.value
        if (!s.active) return
        commit(s)
        Prefs.focusStart = 0
        Dnd.exit(ctx)
        FocusState.session.value = FocusSession(active = false, finished = true, startEpochMs = s.startEpochMs, durationMs = s.durationMs)
    }

    fun dismissFinished() {
        if (FocusState.session.value.finished) FocusState.session.value = FocusSession()
    }
}
