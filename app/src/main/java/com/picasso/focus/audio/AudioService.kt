package com.picasso.focus.audio

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat
import com.picasso.focus.MainActivity
import com.picasso.focus.R
import com.picasso.focus.service.Notif
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Notification média + session (écran de verrouillage, casque). Se ferme seule après 5 min de pause. */
class AudioService : Service() {
    companion object {
        const val A_PLAY = "focus.audio.PLAY"
        const val A_NEXT = "focus.audio.NEXT"
        const val A_PREV = "focus.audio.PREV"
        const val A_STOP = "focus.audio.STOP"
    }

    private lateinit var session: MediaSession
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    private var foregrounded = false
    private val idleStop = Runnable { AudioEngine.shutdown(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }

    private val noisy = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) { AudioEngine.pause() }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        session = MediaSession(this, "FocusAudio").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() = AudioEngine.resume()
                override fun onPause() = AudioEngine.pause()
                override fun onSkipToNext() = AudioEngine.next()
                override fun onSkipToPrevious() = AudioEngine.prev()
                override fun onSeekTo(pos: Long) = AudioEngine.seekTo(pos)
            })
            isActive = true
        }
        ContextCompat.registerReceiver(this, noisy, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_EXPORTED)
        scope.launch { AudioEngine.state.collect { render(it) } }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            A_PLAY -> AudioEngine.toggle()
            A_NEXT -> AudioEngine.next()
            A_PREV -> AudioEngine.prev()
            A_STOP -> { AudioEngine.shutdown(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return START_NOT_STICKY }
        }
        render(AudioEngine.state.value)
        return START_NOT_STICKY
    }

    private fun pi(action: String, req: Int) = Notif.servicePi(this, AudioService::class.java, action, 40 + req)

    private fun render(s: AudioUi) {
        val t = s.current
        val md = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, t?.title ?: "Focus")
            .putString(MediaMetadata.METADATA_KEY_ARTIST, t?.artist ?: "")
            .putLong(MediaMetadata.METADATA_KEY_DURATION, AudioEngine.duration())
        s.art?.let { md.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, it) }
        session.setMetadata(md.build())
        session.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE or
                        PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS or PlaybackState.ACTION_SEEK_TO
                )
                .setState(if (s.playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED, AudioEngine.position(), if (s.playing) 1f else 0f)
                .build()
        )

        val open = Notif.activityPi(this, Intent(this, MainActivity::class.java).putExtra("player", true).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 50)
        val n = Notification.Builder(this, Notif.CH_AUDIO)
            .setSmallIcon(R.drawable.ic_stat_f)
            .setContentTitle(t?.title ?: "Focus")
            .setContentText(t?.artist?.ifBlank { "Lecteur Focus" } ?: "Lecteur Focus")
            .setLargeIcon(s.art)
            .setColor(Notif.ORANGE)
            .setOngoing(s.playing)
            .setShowWhen(false)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(Icon.createWithResource(this, android.R.drawable.ic_media_previous), "Précédent", pi(A_PREV, 0)).build())
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, if (s.playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play),
                    if (s.playing) "Pause" else "Lecture", pi(A_PLAY, 1)
                ).build()
            )
            .addAction(Notification.Action.Builder(Icon.createWithResource(this, android.R.drawable.ic_media_next), "Suivant", pi(A_NEXT, 2)).build())
            .addAction(Notification.Action.Builder(Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel), "Fermer", pi(A_STOP, 3)).build())
            .setStyle(Notification.MediaStyle().setMediaSession(session.sessionToken).setShowActionsInCompactView(0, 1, 2))
            .build()

        try {
            if (!foregrounded) {
                if (Build.VERSION.SDK_INT >= 29) startForeground(Notif.ID_AUDIO, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                else startForeground(Notif.ID_AUDIO, n)
                foregrounded = true
            } else {
                (getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager).notify(Notif.ID_AUDIO, n)
            }
        } catch (_: Exception) {
            stopSelf()
        }

        handler.removeCallbacks(idleStop)
        if (!s.playing) handler.postDelayed(idleStop, 5 * 60_000L)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        try { unregisterReceiver(noisy) } catch (_: Exception) {}
        session.release()
        scope.cancel()
        super.onDestroy()
    }
}
