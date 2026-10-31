package com.picasso.focus.net

import android.content.Context
import android.net.TrafficStats
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface

/** Lectures très légères (compteurs noyau) : aucune requête réseau, aucun réveil. */
object NetCounters {
    data class Raw(val rx: Long, val tx: Long)

    fun mobile(): Raw {
        val rx = TrafficStats.getMobileRxBytes()
        val tx = TrafficStats.getMobileTxBytes()
        return if (rx == TrafficStats.UNSUPPORTED.toLong() || tx == TrafficStats.UNSUPPORTED.toLong()) total() else Raw(rx, tx)
    }

    fun total(): Raw {
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        return Raw(rx.coerceAtLeast(0), tx.coerceAtLeast(0))
    }

    /** Mis à jour par les diffusions système (fiable sur Android 11+, où le scan des interfaces est bloqué). */
    @Volatile var apEnabled = false
    @Volatile var tetherActive = false
    @Volatile private var scanBroken = false

    private val tetherNames = Regex("^(ap\\d+|swlan\\d+|softap\\d+|wlan[1-9]\\d*|rndis\\d+|ncm\\d+|bt-pan|usb\\d+)$")

    /** Interface de partage de connexion active (Wi‑Fi, USB ou Bluetooth) et son adresse IPv4. */
    fun tetherInfo(): Pair<String, String>? {
        if (scanBroken) return null
        try {
            val list = NetworkInterface.getNetworkInterfaces()
            if (list == null || !list.hasMoreElements()) { scanBroken = true; return null }
            for (ni in list) {
                if (!tetherNames.matches(ni.name)) continue
                if (!ni.isUp) continue
                for (a in ni.inetAddresses) {
                    if (a is Inet4Address && !a.isLoopbackAddress) return Pair(ni.name, a.hostAddress ?: "")
                }
            }
        } catch (_: Exception) {
            scanBroken = true
        }
        return null
    }

    fun hotspotActive(ctx: Context): Boolean {
        if (tetherActive || apEnabled) return true
        if (tetherInfo() != null) return true
        // Repli : méthode cachée mais encore accessible sur beaucoup d'appareils.
        return try {
            val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val m = wm.javaClass.getMethod("isWifiApEnabled")
            m.invoke(wm) as Boolean
        } catch (_: Throwable) {
            false
        }
    }
}
