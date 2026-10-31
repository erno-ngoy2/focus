package com.picasso.focus.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.picasso.focus.audio.AudioEngine
import com.picasso.focus.data.Fmt
import com.picasso.focus.ui.FIcon
import com.picasso.focus.ui.Ic
import com.picasso.focus.ui.Label
import com.picasso.focus.ui.PrimaryButton
import com.picasso.focus.ui.SectionTitle
import com.picasso.focus.ui.theme.FocusColors
import com.picasso.focus.ui.theme.LocalAccent
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val accent = LocalAccent.current
    val a by AudioEngine.state.collectAsState()
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    LaunchedEffect(a.playing, a.index, a.version) {
        pos = AudioEngine.position(); dur = AudioEngine.duration()
        while (a.playing) { pos = AudioEngine.position(); dur = AudioEngine.duration(); delay(500) }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        uris.forEach { try { ctx.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {} }
        if (uris.isNotEmpty()) AudioEngine.addTracks(uris)
    }
    val spin = rememberInfiniteTransition(label = "spin")
    val angle by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Restart), label = "angle")

    Column(Modifier.fillMaxSize().background(FocusColors.Bg).statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(FocusColors.Surface).clickable(onClick = onBack), contentAlignment = Alignment.Center) { FIcon(Ic.BACK, size = 22.dp) }
            Text("Lecteur", Modifier.weight(1f), textAlign = TextAlign.Center, color = FocusColors.Text, style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold))
            Box(Modifier.size(42.dp).clip(CircleShape).background(FocusColors.Surface).clickable { picker.launch(arrayOf("audio/*")) }, contentAlignment = Alignment.Center) { FIcon(Ic.PLUS, size = 22.dp) }
        }
        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(240.dp).rotate(if (a.playing) angle else 0f).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(FocusColors.Amber, FocusColors.OrangeDeep))),
                contentAlignment = Alignment.Center
            ) {
                val art = a.art
                if (art != null) Image(art.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else FIcon(Ic.MUSIC, tint = Color.White, size = 84.dp, strokeWidth = 1.6f)
                Box(Modifier.size(34.dp).clip(CircleShape).background(FocusColors.Bg))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(a.current?.title ?: "Aucun morceau", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, maxLines = 1, color = FocusColors.Text, style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold))
        Label(a.current?.artist?.ifBlank { null } ?: "Focus Player", Modifier.fillMaxWidth(), align = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        SeekBar(if (dur > 0) pos.toFloat() / dur else 0f) { AudioEngine.seekTo((it * dur).toLong()); pos = (it * dur).toLong() }
        Row(Modifier.fillMaxWidth()) {
            Label(Fmt.clock(pos)); Spacer(Modifier.weight(1f)); Label(Fmt.clock(dur))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clickable { AudioEngine.toggleShuffle() }, contentAlignment = Alignment.Center) { FIcon(Ic.SHUFFLE, tint = if (a.shuffle) accent else FocusColors.Dim) }
            Box(Modifier.size(52.dp).clickable { AudioEngine.prev() }, contentAlignment = Alignment.Center) { FIcon(Ic.PREV, size = 28.dp) }
            Box(Modifier.size(72.dp).clip(CircleShape).background(accent).clickable { AudioEngine.toggle() }, contentAlignment = Alignment.Center) {
                FIcon(if (a.playing) Ic.PAUSE else Ic.PLAY, tint = Color.White, size = 32.dp)
            }
            Box(Modifier.size(52.dp).clickable { AudioEngine.next() }, contentAlignment = Alignment.Center) { FIcon(Ic.NEXT, size = 28.dp) }
            Box(Modifier.size(44.dp).clickable { AudioEngine.cycleRepeat() }, contentAlignment = Alignment.Center) {
                FIcon(Ic.REPEAT, tint = if (a.repeat > 0) accent else FocusColors.Dim)
                if (a.repeat == 2) Text("1", color = accent, style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold))
            }
        }
        Spacer(Modifier.height(24.dp))
        SectionTitle("Liste de lecture")
        Spacer(Modifier.height(8.dp))
        if (a.tracks.isEmpty()) {
            Label("Ajoutez des fichiers audio de votre téléphone (MP3, FLAC, M4A…). Aucune connexion n'est utilisée.")
            Spacer(Modifier.height(12.dp))
            PrimaryButton("Ajouter des morceaux", { picker.launch(arrayOf("audio/*")) }, icon = Ic.PLUS)
        }
        a.tracks.forEachIndexed { i, t ->
            val cur = i == a.index
            Row(
                Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(16.dp))
                    .background(if (cur) accent.copy(alpha = 0.14f) else FocusColors.Surface).clickable { AudioEngine.playIndex(i) }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FIcon(if (cur && a.playing) Ic.PAUSE else Ic.MUSIC, tint = if (cur) accent else FocusColors.Dim, size = 20.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(t.title, maxLines = 1, color = if (cur) accent else FocusColors.Text, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
                    if (t.artist.isNotBlank()) Label(t.artist)
                }
                if (t.durationMs > 0) Label(Fmt.clock(t.durationMs))
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(32.dp).clickable { AudioEngine.remove(i) }, contentAlignment = Alignment.Center) { FIcon(Ic.CLOSE, tint = FocusColors.Dim, size = 16.dp) }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SeekBar(progress: Float, onSeek: (Float) -> Unit) {
    val accent = LocalAccent.current
    var drag by remember { mutableStateOf<Float?>(null) }
    val shown = drag ?: progress
    Box(
        Modifier.fillMaxWidth().height(28.dp)
            .pointerInput(Unit) { detectTapGestures { onSeek((it.x / size.width).coerceIn(0f, 1f)) } }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { drag?.let(onSeek); drag = null },
                    onDragCancel = { drag = null }
                ) { change, _ -> drag = (change.position.x / size.width).coerceIn(0f, 1f) }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val y = size.height / 2
            drawLine(FocusColors.SurfaceHi, Offset(0f, y), Offset(size.width, y), 8f, StrokeCap.Round)
            drawLine(accent, Offset(0f, y), Offset(size.width * shown.coerceIn(0f, 1f), y), 8f, StrokeCap.Round)
            drawCircle(Color.White, 11f, Offset(size.width * shown.coerceIn(0f, 1f), y))
        }
    }
}
