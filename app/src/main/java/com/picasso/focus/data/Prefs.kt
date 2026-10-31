package com.picasso.focus.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.time.LocalDate

/** Préférences + historique. Écritures rares (jamais à chaque échantillon) pour économiser l'énergie. */
object Prefs {
    private lateinit var sp: SharedPreferences

    fun init(ctx: Context) {
        sp = ctx.applicationContext.getSharedPreferences("focus", Context.MODE_PRIVATE)
    }

    private fun b(k: String, d: Boolean) = sp.getBoolean(k, d)
    private fun l(k: String, d: Long) = sp.getLong(k, d)
    private fun i(k: String, d: Int) = sp.getInt(k, d)
    private fun s(k: String, d: String) = sp.getString(k, d) ?: d

    // --- Hotspot ---
    var limitEnabled: Boolean get() = b("limitEnabled", true); set(v) = sp.edit().putBoolean("limitEnabled", v).apply()
    var limitBytes: Long get() = l("limitBytes", 5_000_000_000L); set(v) = sp.edit().putLong("limitBytes", v).apply()
    var warnEnabled: Boolean get() = b("warnEnabled", true); set(v) = sp.edit().putBoolean("warnEnabled", v).apply()
    var warnPercent: Int get() = i("warnPercent", 80); set(v) = sp.edit().putInt("warnPercent", v).apply()
    var autoCutRoot: Boolean get() = b("autoCutRoot", false); set(v) = sp.edit().putBoolean("autoCutRoot", v).apply()
    var watchHotspot: Boolean get() = b("watchHotspot", true); set(v) = sp.edit().putBoolean("watchHotspot", v).apply()
    var speedInStatusBar: Boolean get() = b("speedInStatusBar", true); set(v) = sp.edit().putBoolean("speedInStatusBar", v).apply()

    // --- Session hotspot en cours (pour survivre à un redémarrage du process) ---
    var sessionStart: Long get() = l("sessionStart", 0); set(v) = sp.edit().putLong("sessionStart", v).apply()
    var baseRx: Long get() = l("baseRx", 0); set(v) = sp.edit().putLong("baseRx", v).apply()
    var baseTx: Long get() = l("baseTx", 0); set(v) = sp.edit().putLong("baseTx", v).apply()
    var lastRx: Long get() = l("lastRx", 0); set(v) = sp.edit().putLong("lastRx", v).apply()
    var lastTx: Long get() = l("lastTx", 0); set(v) = sp.edit().putLong("lastTx", v).apply()
    var warned: Boolean get() = b("warned", false); set(v) = sp.edit().putBoolean("warned", v).apply()
    var alerted: Boolean get() = b("alerted", false); set(v) = sp.edit().putBoolean("alerted", v).apply()
    var lastSessionBytes: Long get() = l("lastSessionBytes", 0); set(v) = sp.edit().putLong("lastSessionBytes", v).apply()

    // --- Concentration ---
    var focusMinutes: Int get() = i("focusMinutes", 25); set(v) = sp.edit().putInt("focusMinutes", v).apply()
    var dnd: Boolean get() = b("dnd", true); set(v) = sp.edit().putBoolean("dnd", v).apply()
    var keepScreenOn: Boolean get() = b("keepScreenOn", true); set(v) = sp.edit().putBoolean("keepScreenOn", v).apply()
    var dimAfterSec: Int get() = i("dimAfterSec", 10); set(v) = sp.edit().putInt("dimAfterSec", v).apply()
    var alwaysOrange: Boolean get() = b("alwaysOrange", false); set(v) = sp.edit().putBoolean("alwaysOrange", v).apply()
    var focusStart: Long get() = l("focusStart", 0); set(v) = sp.edit().putLong("focusStart", v).apply()
    var focusDur: Long get() = l("focusDur", 0); set(v) = sp.edit().putLong("focusDur", v).apply()
    var prevFilter: Int get() = i("prevFilter", -1); set(v) = sp.edit().putInt("prevFilter", v).apply()

    // --- Audio ---
    var playlist: String get() = s("playlist", "[]"); set(v) = sp.edit().putString("playlist", v).apply()

    // --- Historique ---
    internal fun raw(k: String): String = s(k, "{}")
    internal fun putRaw(k: String, v: String) = sp.edit().putString(k, v).apply()
}

/** Historique par jour : octets hotspot et secondes de concentration. */
object History {
    private const val NET = "h_net"
    private const val FOCUS = "h_focus"

    fun day(epochMs: Long = System.currentTimeMillis()): String =
        java.time.Instant.ofEpochMilli(epochMs).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()

    fun today(): String = LocalDate.now().toString()

    fun addNet(day: String, bytes: Long) = add(NET, day, bytes)
    fun addFocus(day: String, seconds: Long) = add(FOCUS, day, seconds)
    fun net(): Map<String, Long> = load(NET)
    fun focus(): Map<String, Long> = load(FOCUS)

    private fun load(key: String): Map<String, Long> {
        val out = HashMap<String, Long>()
        try {
            val o = JSONObject(Prefs.raw(key))
            for (k in o.keys()) out[k] = o.getLong(k)
        } catch (_: Exception) {
        }
        return out
    }

    private fun add(key: String, day: String, v: Long) {
        if (v <= 0) return
        val m = load(key).toMutableMap()
        m[day] = (m[day] ?: 0L) + v
        val cutoff = LocalDate.now().minusDays(120).toString()
        val o = JSONObject()
        for ((k, x) in m) if (k >= cutoff) o.put(k, x)
        Prefs.putRaw(key, o.toString())
    }
}
