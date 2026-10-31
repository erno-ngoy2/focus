package com.picasso.focus.ui

import android.content.Context
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.picasso.focus.audio.AudioEngine
import com.picasso.focus.data.FocusState
import com.picasso.focus.data.Fmt
import com.picasso.focus.data.History
import com.picasso.focus.data.Prefs
import com.picasso.focus.ui.theme.FocusColors
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil

private const val BRIGHT_AWAKE = 0.22f
private const val BRIGHT_DIM = 0.01f

/**
 * Écran de veille « concentration » (style StandBy) : fond noir pur (OLED éteint), grande horloge orange,
 * date, et infos de connexion en bas. L'horloge ne se met à jour qu'une fois par minute.
 */
@Composable
fun StandbyScreen(setBrightness: (Float) -> Unit, onExit: () -> Unit) {
    val ctx = LocalContext.current
    val session by FocusState.session.collectAsState()
    val audio by AudioEngine.state.collectAsState()
    val orange = FocusColors.Orange

    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    var battery by remember { mutableIntStateOf(readBattery(ctx)) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            battery = readBattery(ctx)
            delay(60_000L - now % 60_000L + 50L)
        }
    }

    var touch by remember { mutableIntStateOf(0) }
    var dim by remember { mutableStateOf(false) }
    var controls by remember { mutableStateOf(false) }
    val dimAfter = Prefs.dimAfterSec
    LaunchedEffect(touch) {
        dim = false
        setBrightness(BRIGHT_AWAKE)
        if (dimAfter > 0) {
            delay(dimAfter * 1000L)
            dim = true
            controls = false
            setBrightness(BRIGHT_DIM)
        }
    }
    LaunchedEffect(touch, controls) { if (controls) { delay(6000); controls = false } }

    val alpha by animateFloatAsState(if (dim) 0.45f else 1f, tween(900), label = "dim")
    val shift = ((now / 60_000L) % 9).toInt()
    val dx by animateDpAsState(10.dp * ((shift % 3) - 1), tween(2500), label = "dx")
    val dy by animateDpAsState(8.dp * ((shift / 3) - 1), tween(2500), label = "dy")

    val zdt = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault())
    val is24 = DateFormat.is24HourFormat(ctx)
    val hhmm = zdt.format(DateTimeFormatter.ofPattern(if (is24) "HH:mm" else "h:mm"))
    val date = zdt.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())).replaceFirstChar { it.uppercase() }

    Box(
        Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) {
            detectTapGestures(
                onTap = { if (session.finished) onExit() else { val wasDim = dim; touch++; controls = !wasDim && !controls } },
                onLongPress = { onExit() }
            )
        }
    ) {
        Column(Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }.padding(horizontal = 28.dp, vertical = 18.dp)) {
            // Barre du haut
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val remaining = if (session.active && session.durationMs > 0) ceil((session.startEpochMs + session.durationMs - now) / 60_000.0).toInt().coerceAtLeast(0) else -1
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(orange.copy(alpha = 0.16f)).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(orange))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (session.finished) "Terminé" else if (remaining >= 0) "Focus · $remaining min" else "Focus",
                        color = orange, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    )
                }
                Spacer(Modifier.weight(1f))
                FIcon(Ic.BATTERY, tint = FocusColors.Dim, size = 18.dp)
                Spacer(Modifier.width(6.dp))
                Text("$battery %", color = FocusColors.Dim, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
            }

            // Horloge
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val fs = minOf(maxHeight.value * 0.62f, maxWidth.value * 0.27f).sp
                Column(Modifier.offset(dx, dy), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (session.finished) {
                        Text("Session terminée", color = orange, style = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold))
                        Text("Touchez l'écran pour fermer", color = FocusColors.Dim, style = TextStyle(fontSize = 15.sp))
                    } else {
                        Text(
                            hhmm,
                            style = TextStyle(
                                brush = Brush.verticalGradient(listOf(FocusColors.Amber, orange, FocusColors.OrangeDeep)),
                                fontSize = fs, fontWeight = FontWeight.Bold, letterSpacing = (-4).sp, fontFeatureSettings = "tnum", textAlign = TextAlign.Center
                            )
                        )
                        Text(date, color = FocusColors.Text.copy(alpha = 0.85f), style = TextStyle(fontSize = (fs.value * 0.1f).coerceIn(16f, 30f).sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp))
                    }
                }
                // Commandes (tap) : musique + aide
                androidx.compose.animation.AnimatedVisibility(controls, Modifier.align(Alignment.BottomCenter), enter = fadeIn(), exit = fadeOut()) {
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(Color(0xFF16161A)).border(1.dp, FocusColors.Outline, RoundedCornerShape(50)).padding(horizontal = 18.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Box(Modifier.size(36.dp).clickable { touch++; AudioEngine.prev() }, contentAlignment = Alignment.Center) { FIcon(Ic.PREV, size = 20.dp) }
                        Box(Modifier.size(44.dp).clip(CircleShape).background(orange).clickable { touch++; AudioEngine.toggle() }, contentAlignment = Alignment.Center) {
                            FIcon(if (audio.playing) Ic.PAUSE else Ic.PLAY, tint = Color.White, size = 20.dp)
                        }
                        Box(Modifier.size(36.dp).clickable { touch++; AudioEngine.next() }, contentAlignment = Alignment.Center) { FIcon(Ic.NEXT, size = 20.dp) }
                        Text("Appui long : terminer", color = FocusColors.Dim, style = TextStyle(fontSize = 12.sp))
                    }
                }
            }

            InfoBar()
        }
    }
}

@Composable
private fun InfoBar() {
    val net by FocusState.net.collectAsState()
    val todayBytes = remember(net.hotspotOn) { History.net()[History.today()] ?: 0L }
    val cells: List<Triple<String, String, Boolean>> = if (net.hotspotOn) listOf(
        Triple("CONSOMMÉ", Fmt.bytes(net.total), true),
        Triple("RESTANT", if (net.limitBytes > 0) Fmt.bytes(net.remaining) else "Illimité", true),
        Triple("DESCENDANT", Fmt.speed(net.rxSpeed), false),
        Triple("MONTANT", Fmt.speed(net.txSpeed), false)
    ) else listOf(
        Triple("HOTSPOT", "Inactif", false),
        Triple("AUJOURD'HUI", Fmt.bytes(todayBytes), true),
        Triple("DESCENDANT", Fmt.speed(net.rxSpeed), false),
        Triple("MONTANT", Fmt.speed(net.txSpeed), false)
    )
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color(0xFF111114)).border(1.dp, Color(0xFF222226), RoundedCornerShape(22.dp)).padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        cells.forEach { (l, v, accent) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(l, color = FocusColors.Dim, style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp))
                Spacer(Modifier.height(4.dp))
                Text(v, color = if (accent) FocusColors.Orange else FocusColors.Text, style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"))
            }
        }
    }
}

private fun readBattery(ctx: Context): Int = try {
    (ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
} catch (_: Exception) { 0 }
