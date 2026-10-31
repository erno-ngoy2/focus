package com.picasso.focus.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Icon

object Notif {
    const val CH_WATCH = "watch"
    const val CH_METER = "meter"
    const val CH_ALERT = "alert"
    const val CH_AUDIO = "audio"
    const val CH_FOCUS = "focus"

    const val ID_MONITOR = 1
    const val ID_ALERT = 2
    const val ID_AUDIO = 3
    const val ID_FOCUS_DONE = 4

    const val ORANGE = 0xFFFF7A1A.toInt()

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CH_WATCH, "Veille Focus", NotificationManager.IMPORTANCE_MIN).apply {
            description = "Surveille discrètement le point d'accès. Aucun son, aucune alerte."
            setShowBadge(false)
        })
        nm.createNotificationChannel(NotificationChannel(CH_METER, "Compteur de données", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Consommation et débit du point d'accès en direct."
            setShowBadge(false)
        })
        nm.createNotificationChannel(NotificationChannel(CH_ALERT, "Alertes de limite", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Prévient quand la limite de données du hotspot approche ou est atteinte."
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250, 120, 250, 120, 450)
        })
        nm.createNotificationChannel(NotificationChannel(CH_AUDIO, "Lecteur audio", NotificationManager.IMPORTANCE_LOW).apply {
            setShowBadge(false)
        })
        nm.createNotificationChannel(NotificationChannel(CH_FOCUS, "Session de concentration", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Fin d'une session de concentration."
        })
    }

    private const val PI_FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    fun activityPi(ctx: Context, intent: Intent, req: Int): PendingIntent =
        PendingIntent.getActivity(ctx, req, intent, PI_FLAGS)

    fun servicePi(ctx: Context, cls: Class<*>, action: String, req: Int): PendingIntent =
        PendingIntent.getService(ctx, req, Intent(ctx, cls).setAction(action), PI_FLAGS)

    private var iconKey = ""
    private var iconCache: Icon? = null

    /** Petite icône de barre d'état qui affiche le débit en chiffres (ex. « 1.2 / Mo/s »). */
    fun speedIcon(top: String, bottom: String): Icon {
        val key = "$top|$bottom"
        iconCache?.let { if (key == iconKey) return it }
        val s = 96
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        p.textSize = if (top.length >= 4) 44f else 58f
        c.drawText(top, s / 2f, 54f, p)
        p.textSize = 30f
        c.drawText(bottom, s / 2f, 90f, p)
        val ic = Icon.createWithBitmap(bmp)
        iconKey = key
        iconCache = ic
        return ic
    }
}
