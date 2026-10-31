package com.picasso.focus.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.picasso.focus.FocusModeActivity
import com.picasso.focus.audio.AudioEngine
import com.picasso.focus.data.FocusController
import com.picasso.focus.data.FocusState
import com.picasso.focus.data.Fmt
import com.picasso.focus.data.History
import com.picasso.focus.data.Prefs
import com.picasso.focus.net.SystemControls
import com.picasso.focus.ui.FCard
import com.picasso.focus.ui.FIcon
import com.picasso.focus.ui.FocusLogo
import com.picasso.focus.ui.Ic
import com.picasso.focus.ui.Label
import com.picasso.focus.ui.PrimaryButton
import com.picasso.focus.ui.SectionTitle
import com.picasso.focus.ui.Segmented
import com.picasso.focus.ui.ToggleTile
import com.picasso.focus.ui.theme.FocusColors
import com.picasso.focus.ui.theme.LocalAccent
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.TextStyle as DayStyle
import java.util.Locale

@Composable
fun HomeScreen(onOpenPlayer: () -> Unit, onOpenNetwork: () -> Unit) {
    val ctx = LocalContext.current
    val accent = LocalAccent.current
    val net by FocusState.net.collectAsState()
    val session by FocusState.session.collectAsState()
    val audio by AudioEngine.state.collectAsState()

    var wifi by remember { mutableStateOf(false) }
    var bt by remember { mutableStateOf(false) }
    var dnd by remember { mutableStateOf(false) }
    var hotspot by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            val r = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                booleanArrayOf(
                    SystemControls.isWifiOn(ctx), SystemControls.isBluetoothOn(ctx), SystemControls.dndOn(ctx),
                    com.picasso.focus.net.NetCounters.hotspotActive(ctx)
                )
            }
            wifi = r[0]; bt = r[1]; dnd = r[2]; hotspot = r[3]
            delay(3000)
        }
    }

    val days = remember { (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) } }
    var selected by remember { mutableIntStateOf(6) }
    val focusMap = remember(session) { History.focus() }
    val netMap = remember(net.hotspotOn) { History.net() }
    val key = days[selected].toString()
    val focusSec = (focusMap[key] ?: 0L) + if (selected == 6 && session.active) (System.currentTimeMillis() - session.startEpochMs) / 1000 else 0L
    val netBytes = (netMap[key] ?: 0L) + if (selected == 6 && net.hotspotOn) net.total else 0L

    Column(Modifier.verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            FocusLogo(40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("focus", color = FocusColors.Text, style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp))
                Label("par MalakiLabs")
            }
        }

        // Bandeau des jours
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            days.forEachIndexed { i, d ->
                val sel = i == selected
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (sel) accent else FocusColors.Surface)
                        .clickable { selected = i }.padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(d.dayOfMonth.toString(), color = if (sel) Color.White else FocusColors.Text, style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold))
                    Text(d.dayOfWeek.getDisplayName(DayStyle.SHORT, Locale.getDefault()).take(3), color = if (sel) Color.White.copy(alpha = 0.85f) else FocusColors.Dim, style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium))
                }
            }
        }

        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(Fmt.hm(focusSec).let { if (focusSec < 60) "0m" else it }, color = FocusColors.Text, style = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp))
            Text("Temps de concentration", color = accent, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
            Spacer(Modifier.height(4.dp))
            Label("Hotspot ce jour-là : ${Fmt.bytes(netBytes)}")
        }

        // Mode concentration
        FCard(onClick = {
            if (!session.active) FocusController.start(ctx, Prefs.focusMinutes)
            ctx.startActivity(Intent(ctx, FocusModeActivity::class.java))
        }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(accent), contentAlignment = Alignment.Center) { FIcon(Ic.FOCUS, tint = Color.White, size = 26.dp) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (session.active) "Session en cours" else "Mode concentration", color = FocusColors.Text, style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold))
                    Label(if (session.active) "Touchez pour revenir à l'écran de veille" else "${Prefs.focusMinutes} min · écran de veille orange en paysage")
                }
                FIcon(Ic.NEXT, tint = FocusColors.Dim, size = 18.dp)
            }
        }

        // Hotspot
        FCard(onClick = onOpenNetwork) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FIcon(Ic.HOTSPOT, tint = if (net.hotspotOn) accent else FocusColors.Dim)
                Spacer(Modifier.width(10.dp))
                Text("Point d'accès", color = FocusColors.Text, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                Text(if (net.hotspotOn) "ACTIF" else "INACTIF", color = if (net.hotspotOn) FocusColors.Ok else FocusColors.Dim, style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
            }
            Spacer(Modifier.height(12.dp))
            if (net.hotspotOn) {
                val p = Fmt.parts(net.total)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(p.first, color = FocusColors.Text, style = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"))
                    Text(" ${p.second}", Modifier.padding(bottom = 5.dp), color = FocusColors.Dim, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium))
                    Spacer(Modifier.weight(1f))
                    Text(if (net.limitBytes > 0) "sur ${Fmt.bytes(net.limitBytes)}" else "sans limite", Modifier.padding(bottom = 5.dp), color = FocusColors.Dim, style = TextStyle(fontSize = 13.sp))
                }
                if (net.limitBytes > 0) {
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(FocusColors.SurfaceHi)) {
                        Box(Modifier.fillMaxWidth(net.progress.coerceAtLeast(0.02f)).height(8.dp).clip(RoundedCornerShape(50)).background(if (net.limitReached) FocusColors.Danger else accent))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Label("↓ ${Fmt.speed(net.rxSpeed)}    ↑ ${Fmt.speed(net.txSpeed)}")
            } else {
                Label("Activez le point d'accès : le compteur démarre tout seul et s'affiche dans la barre de notifications.")
                Spacer(Modifier.height(12.dp))
                PrimaryButton("Ouvrir le point d'accès", { SystemControls.openHotspot(ctx) }, icon = Ic.HOTSPOT)
            }
        }

        SectionTitle("Commandes")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToggleTile(Ic.HOTSPOT, "Hotspot", if (hotspot) "Actif" else "Inactif", hotspot, { SystemControls.openHotspot(ctx) }, Modifier.weight(1f))
            ToggleTile(Ic.WIFI, "Wi‑Fi", if (wifi) "Activé" else "Désactivé", wifi, { SystemControls.openWifi(ctx) }, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToggleTile(Ic.BLUETOOTH, "Bluetooth", if (bt) "Activé" else "Désactivé", bt, { SystemControls.openBluetooth(ctx) }, Modifier.weight(1f))
            ToggleTile(Ic.MOON, "Ne pas déranger", if (dnd) "Activé" else if (SystemControls.dndGranted(ctx)) "Désactivé" else "Autoriser", dnd, {
                SystemControls.setDnd(ctx, !dnd); dnd = SystemControls.dndOn(ctx)
            }, Modifier.weight(1f))
        }

        // Mini lecteur
        FCard(onClick = onOpenPlayer) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) { FIcon(Ic.MUSIC, tint = accent, size = 24.dp) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(audio.current?.title ?: "Lecteur audio", color = FocusColors.Text, maxLines = 1, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold))
                    Label(audio.current?.artist?.ifBlank { null } ?: if (audio.tracks.isEmpty()) "Ajoutez vos morceaux" else "${audio.tracks.size} morceaux")
                }
                Box(Modifier.size(44.dp).clip(CircleShape).background(accent).clickable { if (audio.tracks.isEmpty()) onOpenPlayer() else AudioEngine.toggle() }, contentAlignment = Alignment.Center) {
                    FIcon(if (audio.playing) Ic.PAUSE else Ic.PLAY, tint = Color.White, size = 20.dp)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
