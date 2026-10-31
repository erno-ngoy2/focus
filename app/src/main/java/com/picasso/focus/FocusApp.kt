package com.picasso.focus

import android.app.Application
import com.picasso.focus.audio.AudioEngine
import com.picasso.focus.data.FocusController
import com.picasso.focus.data.Prefs
import com.picasso.focus.service.Notif

class FocusApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
        Prefs.init(this)
        runCatching { Notif.createChannels(this) }
        runCatching { FocusController.restore(this) }
        runCatching { AudioEngine.init(this) }
    }
}
