package com.picasso.focus.audio

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.picasso.focus.data.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class Track(val uri: String, val title: String, val artist: String, val durationMs: Long)

data class AudioUi(
    val tracks: List<Track> = emptyList(),
    val index: Int = -1,
    val playing: Boolean = false,
    val shuffle: Boolean = false,
    val repeat: Int = 0, // 0 off, 1 tout, 2 piste
    val art: Bitmap? = null,
    val version: Int = 0
) {
    val current: Track? get() = tracks.getOrNull(index)
}

/** Lecteur local léger basé sur MediaPlayer (aucune dépendance). Le service ne sert qu'à la notification / session média. */
object AudioEngine {
    val state = MutableStateFlow(AudioUi())

    private lateinit var app: Context
    private var mp: MediaPlayer? = null
    private var prepared = false
    private val main = Handler(Looper.getMainLooper())
    private var focusReq: AudioFocusRequest? = null
    private var ducked = false
    private var resumeOnFocus = false

    fun init(ctx: Context) {
        app = ctx.applicationContext
        val list = ArrayList<Track>()
        try {
            val a = JSONArray(Prefs.playlist)
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                list.add(Track(o.getString("u"), o.getString("t"), o.optString("a"), o.optLong("d")))
            }
        } catch (_: Exception) {
        }
        state.value = AudioUi(tracks = list)
    }

    private fun save() {
        val a = JSONArray()
        for (t in state.value.tracks) a.put(JSONObject().put("u", t.uri).put("t", t.title).put("a", t.artist).put("d", t.durationMs))
        Prefs.playlist = a.toString()
    }

    private fun set(f: (AudioUi) -> AudioUi) {
        state.value = f(state.value).let { it.copy(version = it.version + 1) }
    }

    fun addTracks(uris: List<Uri>) {
        Thread {
            val out = ArrayList<Track>()
            for (u in uris) {
                var title = u.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "Piste"
                var artist = ""
                var dur = 0L
                try {
                    val r = MediaMetadataRetriever()
                    r.setDataSource(app, u)
                    r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let { if (it.isNotBlank()) title = it }
                    artist = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: ""
                    dur = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                    r.release()
                } catch (_: Exception) {
                }
                out.add(Track(u.toString(), title, artist, dur))
            }
            main.post {
                set { it.copy(tracks = it.tracks + out) }
                save()
            }
        }.start()
    }

    fun remove(i: Int) {
        val s = state.value
        if (i !in s.tracks.indices) return
        if (i == s.index) stopPlayer()
        val newList = s.tracks.toMutableList().also { it.removeAt(i) }
        val newIdx = if (s.index > i) s.index - 1 else if (s.index == i) -1 else s.index
        set { it.copy(tracks = newList, index = newIdx, playing = if (s.index == i) false else it.playing, art = if (s.index == i) null else it.art) }
        save()
    }

    private fun requestFocus(): Boolean {
        val am = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setOnAudioFocusChangeListener { change ->
                when (change) {
                    AudioManager.AUDIOFOCUS_LOSS -> { resumeOnFocus = false; pause() }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> { resumeOnFocus = state.value.playing; pause() }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> { ducked = true; mp?.setVolume(0.25f, 0.25f) }
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        if (ducked) { ducked = false; mp?.setVolume(1f, 1f) }
                        if (resumeOnFocus) { resumeOnFocus = false; resume() }
                    }
                }
            }.build()
        focusReq = req
        return am.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonFocus() {
        val am = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        focusReq?.let { am.abandonAudioFocusRequest(it) }
        focusReq = null
    }

    private fun ensureService() {
        try {
            app.startForegroundService(Intent(app, AudioService::class.java))
        } catch (_: Exception) {
        }
    }

    fun playIndex(i: Int) {
        val s = state.value
        val t = s.tracks.getOrNull(i) ?: return
        stopPlayer()
        if (!requestFocus()) return
        ensureService()
        try {
            val p = MediaPlayer()
            p.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            p.setDataSource(app, Uri.parse(t.uri))
            p.setOnPreparedListener { prepared = true; it.start(); set { u -> u.copy(playing = true) } }
            p.setOnCompletionListener { onComplete() }
            p.setOnErrorListener { _, _, _ -> stopPlayer(); set { u -> u.copy(playing = false) }; true }
            mp = p
            prepared = false
            set { it.copy(index = i, playing = false, art = loadArt(t.uri)) }
            p.prepareAsync()
        } catch (_: Exception) {
            stopPlayer()
        }
    }

    private fun loadArt(uri: String): Bitmap? = try {
        val r = MediaMetadataRetriever()
        r.setDataSource(app, Uri.parse(uri))
        val b = r.embeddedPicture
        r.release()
        if (b != null) BitmapFactory.decodeByteArray(b, 0, b.size, BitmapFactory.Options().apply { inSampleSize = 2 }) else null
    } catch (_: Exception) {
        null
    }

    private fun onComplete() {
        val s = state.value
        when {
            s.repeat == 2 -> playIndex(s.index)
            s.tracks.size <= 1 && s.repeat == 0 -> { set { it.copy(playing = false) } }
            else -> next(auto = true)
        }
    }

    fun toggle() {
        val s = state.value
        if (s.index < 0) { if (s.tracks.isNotEmpty()) playIndex(0); return }
        if (s.playing) pause() else resume()
    }

    fun pause() {
        try { if (prepared) mp?.pause() } catch (_: Exception) {}
        set { it.copy(playing = false) }
    }

    fun resume() {
        if (mp == null || !prepared) { val i = state.value.index; if (i >= 0) playIndex(i); return }
        if (focusReq == null && !requestFocus()) return
        ensureService()
        try { mp?.start() } catch (_: Exception) {}
        set { it.copy(playing = true) }
    }

    fun next(auto: Boolean = false) {
        val s = state.value
        if (s.tracks.isEmpty()) return
        val n = when {
            s.shuffle && s.tracks.size > 1 -> { var r: Int; do { r = s.tracks.indices.random() } while (r == s.index); r }
            s.index + 1 < s.tracks.size -> s.index + 1
            s.repeat == 1 || !auto -> 0
            else -> { stopPlayer(); set { it.copy(playing = false) }; return }
        }
        playIndex(n)
    }

    fun prev() {
        val s = state.value
        if (s.tracks.isEmpty()) return
        if (position() > 3000) { seekTo(0); return }
        playIndex(if (s.index - 1 >= 0) s.index - 1 else s.tracks.size - 1)
    }

    fun seekTo(ms: Long) { try { if (prepared) mp?.seekTo(ms.toInt()) } catch (_: Exception) {} }

    fun position(): Long = try { if (prepared) (mp?.currentPosition ?: 0).toLong() else 0L } catch (_: Exception) { 0L }

    fun duration(): Long = try { if (prepared) (mp?.duration ?: 0).toLong() else (state.value.current?.durationMs ?: 0L) } catch (_: Exception) { 0L }

    fun toggleShuffle() = set { it.copy(shuffle = !it.shuffle) }

    fun cycleRepeat() = set { it.copy(repeat = (it.repeat + 1) % 3) }

    private fun stopPlayer() {
        try { mp?.release() } catch (_: Exception) {}
        mp = null
        prepared = false
        abandonFocus()
    }

    fun shutdown() {
        stopPlayer()
        set { it.copy(playing = false) }
    }
}
