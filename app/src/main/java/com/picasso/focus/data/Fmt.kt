package com.picasso.focus.data

import java.util.Locale

object Fmt {
    private fun num(v: Double, dec: Int) = String.format(Locale.getDefault(), "%.${dec}f", v)

    /** (valeur, unité) pour affichage en gros chiffres. Unités décimales comme Android. */
    fun parts(bytes: Long): Pair<String, String> {
        val b = bytes.coerceAtLeast(0).toDouble()
        return when {
            b < 1_000 -> Pair(num(b, 0), "o")
            b < 1_000_000 -> Pair(num(b / 1e3, 0), "Ko")
            b < 1_000_000_000 -> Pair(num(b / 1e6, if (b >= 1e8) 0 else if (b >= 1e7) 1 else 2), "Mo")
            else -> Pair(num(b / 1e9, if (b >= 1e11) 0 else if (b >= 1e10) 1 else 2), "Go")
        }
    }

    fun bytes(b: Long): String = parts(b).let { "${it.first} ${it.second}" }

    fun speedParts(bps: Long): Pair<String, String> {
        val p = parts(bps)
        return Pair(p.first, p.second + "/s")
    }

    fun speed(bps: Long): String = speedParts(bps).let { "${it.first} ${it.second}" }

    /** h:mm:ss ou mm:ss */
    fun clock(ms: Long): String {
        val t = (ms / 1000).coerceAtLeast(0)
        val h = t / 3600
        val m = (t % 3600) / 60
        val s = t % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%02d:%02d", m, s)
    }

    /** 2h 23m */
    fun hm(seconds: Long): String {
        val m = (seconds / 60).coerceAtLeast(0)
        return if (m >= 60) "${m / 60}h ${(m % 60).toString().padStart(2, '0')}m" else "${m}m"
    }
}
