package com.picasso.focus.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.picasso.focus.FocusModeActivity
import com.picasso.focus.data.FocusController
import com.picasso.focus.data.FocusState
import com.picasso.focus.data.Fmt
import com.picasso.focus.data.Prefs
import com.picasso.focus.net.SystemControls
import com.picasso.focus.ui.Chip
import com.picasso.focus.ui.FCard
import com.picasso.focus.ui.GhostButton
import com.picasso.focus.ui.Ic
import com.picasso.focus.ui.Label
import com.picasso.focus.ui.PrimaryButton
import com.picasso.focus.ui.RingGauge
import com.picasso.focus.ui.SectionTitle
import com.picasso.focus.ui.SettingRow
import com.picasso.focus.ui.Title
import com.picasso.focus.ui.theme.FocusColors
import com.picasso.focus.ui.theme.LocalAccent
import kotlinx.coroutines.delay

@Composable
fun FocusScreen() {
    val ctx = LocalContext.current
    val accent = LocalAccent.current
    val session by FocusState.session.collectAsState()
    var minutes by remember { mutableStateOf(Prefs.focusMinutes) }
    var dnd by remember { mutableStateOf(Prefs.dnd) }
    var keepOn by remember { mutableStateOf(Prefs.keepScreenOn) }
    var dim by remember { mutableStateOf(Prefs.dimAfterSec) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(session.active) { while (session.active) { now = System.currentTimeMillis(); delay(1000) } }

    fun launchStandby() = ctx.startActivity(Intent(ctx, FocusModeActivity::class.java))

    Column(Modifier.verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Title("Concentration", Modifier.padding(top = 12.dp))

        if (session.active) {
            val total = session.durationMs
            val elapsed = now - session.startEpochMs
            val remain = if (total > 0) (total - elapsed).coerceAtLeast(0) else elapsed
            FCard(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    RingGauge(if (total > 0) elapsed.toFloat() / total else 0f, Modifier.size(250.dp), 16.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(Fmt.clock(remain), color = FocusColors.Text, style = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"))
                            Label(if (total > 0) "restantes" else "écoulées")
                        }
                    }
                }
            }
            PrimaryButton("Ouvrir l'écran de veille", { launchStandby() }, icon = Ic.FOCUS)
            PrimaryButton("Abandonner", { FocusController.stop(ctx) }, color = FocusColors.Danger, icon = Ic.CLOSE)
        } else {
            FCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Label("Durée de la session")
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(if (minutes == 0) "∞" else minutes.toString(), color = accent, style = TextStyle(fontSize = 84.sp, fontWeight = FontWeight.Bold, letterSpacing = (-3).sp, fontFeatureSettings = "tnum"))
                        if (minutes != 0) Text(" min", Modifier.padding(bottom = 16.dp), color = FocusColors.Dim, style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15, 25, 45, 60, 90).forEach { m -> Chip("$m", minutes == m, { minutes = m; Prefs.focusMinutes = m }, Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        GhostButton("−5", { minutes = (minutes - 5).coerceAtLeast(5); Prefs.focusMinutes = minutes })
                        Chip("Sans limite", minutes == 0, { minutes = 0; Prefs.focusMinutes = 0 })
                        GhostButton("+5", { minutes = (minutes + 5).coerceAtMost(240); Prefs.focusMinutes = minutes })
                    }
                }
            }
            PrimaryButton("Lancer le mode concentration", {
                FocusController.start(ctx, minutes)
                launchStandby()
            }, icon = Ic.FOCUS)

            SectionTitle("Pendant la session")
            FCard {
                SettingRow(Ic.MOON, "Ne pas déranger", if (SystemControls.dndGranted(ctx)) "Coupe les notifications" else "Autorisation requise (toucher pour l'accorder)",
                    checked = dnd, onChange = { dnd = it; Prefs.dnd = it; if (it && !SystemControls.dndGranted(ctx)) SystemControls.setDnd(ctx, true) })
                SettingRow(Ic.CLOCK, "Écran de veille allumé", "Horloge OLED très faible luminosité", checked = keepOn, onChange = { keepOn = it; Prefs.keepScreenOn = it })
                Spacer(Modifier.height(4.dp))
                Label("Assombrir après")
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5 to "5 s", 10 to "10 s", 30 to "30 s", 0 to "Jamais").forEach { (v, l) -> Chip(l, dim == v, { dim = v; Prefs.dimAfterSec = v }, Modifier.weight(1f)) }
                }
            }
        }
        Label("Le mode veille n'utilise ni réseau ni capteurs : une mise à jour par minute pour l'horloge, quelques secondes pour les infos de connexion.")
        Spacer(Modifier.height(16.dp))
    }
}
