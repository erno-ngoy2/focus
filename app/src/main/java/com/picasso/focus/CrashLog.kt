package com.picasso.focus

import android.content.Context
import java.io.File

/** Enregistre la dernière erreur fatale pour l'afficher au prochain lancement (pratique sans Logcat). */
object CrashLog {
    private fun file(ctx: Context) = File(ctx.filesDir, "last_crash.txt")

    fun install(ctx: Context) {
        val app = ctx.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                file(app).writeText("Thread: ${t.name}\n" + android.util.Log.getStackTraceString(e))
            } catch (_: Throwable) {
            }
            previous?.uncaughtException(t, e)
        }
    }

    fun read(ctx: Context): String? = try {
        file(ctx).takeIf { it.exists() }?.readText()
    } catch (_: Throwable) {
        null
    }

    fun clear(ctx: Context) {
        try { file(ctx).delete() } catch (_: Throwable) {}
    }
}
