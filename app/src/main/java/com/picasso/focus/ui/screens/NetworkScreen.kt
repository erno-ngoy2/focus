package com.picasso.focus.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.picasso.focus.data.FocusState
import com.picasso.focus.data.Fmt
import com.picasso.focus.data.Prefs
import com.picasso.focus.net.SystemControls
import com.picasso.focus.service.MonitorService
import com.picasso.focus.ui.Chip
import com.picasso.focus.ui.FCard
import com.picasso.focus.ui.FIcon
import com.picasso.focus.ui.GhostButton
import com.picasso.focus.ui.Ic
import com.picasso.focus.ui.Label
import com.picasso.focus.ui.PrimaryButton
import com.picasso.focus.ui.RingGauge
import com.picasso.focus.ui.SectionTitle
import com.picasso.focus.ui.SettingRow
import com.picasso.focus.ui.StatTile
import com.picasso.focus.ui.StatusDot
import com.picasso.focus.ui.Title
import com.picasso.focus.ui.theme.FocusColors
import com.picasso.focus.ui.theme.LocalAccent
import kotlinx.coroutines.delay

@Composable
fun NetworkScreen() {
    val ctx = LocalContext.current
    val accent = LocalAccent.current
    val net by FocusState.net.collectAsState()

    DisposableEffect(Unit) {
        FocusState.acquireLive(ctx)
        onDispose { FocusState.releaseLive() }
    }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(net.hotspotOn) { while (net.hotspotOn) { now = System.currentTimeMillis(); delay(1000) } }

    var limitEnabled by remember { mutableStateOf(Prefs.limitEnabled) }
    var limit by remember { mutableStateOf(Prefs.limitBytes) }
    var warn by remember { mutableStateOf(Prefs.warnEnabled) }
    var statusSpeed by remember { mutableStateOf(Prefs.speedInStatusBar) }
    var copied by remember { mutableStateOf(false) }

    val shownBytes = if (net.hotspotOn) net.total else net.lastSessionBytes
    val p = Fmt.parts(shownBytes)
    val presets = listOf(500_000_000L to "500 Mo", 1_000_000_000L to "1 Go", 2_000_000_000L to "2 Go", 5_000_000_000L to "5 Go", 10_000_000_000L to "10 Go", 20_000_000_000L to "20 Go")

    Column(Modifier.verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Title("Réseau", Modifier.weight(1f))
            StatusDot(net.hotspotOn)
            Spacer(Modifier.width(6.dp))
            Text(if (net.hotspotOn) "Hotspot actif" else "Hotspot inactif", color = FocusColors.Dim, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
        }

        FCard(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                RingGauge(if (net.limitBytes > 0) net.progress else if (net.hotspotOn) 1f else 0f, Modifier.size(230.dp), 16.dp, if (net.limitReached) FocusColors.Danger else accent) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Label(if (net.hotspotOn) "Consommé" else "Dernière session")
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(p.first, color = FocusColors.Text, style = TextStyle(fontSize = 46.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp, fontFeatureSettings = "tnum"))
                            Text(" ${p.second}", Modifier.padding(bottom = 8.dp), color = FocusColors.Dim, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold))
                        }
                        Label(if (net.limitBytes > 0) "sur ${Fmt.bytes(net.limitBytes)}" else "sans limite")
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val rem = if (net.limitBytes > 0 && net.hotspotOn) Fmt.parts(net.remaining) else null
            StatTile(Ic.GAUGE, "Restant", rem?.first ?: "—", rem?.second ?: "", Modifier.weight(1f))
            StatTile(Ic.CLOCK, "Durée", if (net.hotspotOn) Fmt.clock(now - net.sessionStartMs) else "—", "", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val d = Fmt.speedParts(net.rxSpeed)
            val u = Fmt.speedParts(net.txSpeed)
            StatTile(Ic.DOWN, "Descendant", d.first, d.second, Modifier.weight(1f))
            StatTile(Ic.UP, "Montant", u.first, u.second, Modifier.weight(1f))
        }

        if (net.hotspotOn) PrimaryButton("Couper le point d'accès", { SystemControls.openHotspot(ctx) }, color = FocusColors.Danger, icon = Ic.POWER)
        else PrimaryButton("Activer le point d'accès", { SystemControls.openHotspot(ctx) }, icon = Ic.HOTSPOT)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GhostButton("Remettre à zéro", { ctx.startService(Intent(ctx, MonitorService::class.java).setAction(MonitorService.ACTION_RESET)) }, Modifier.weight(1f), Ic.RESET)
            GhostButton("+500 Mo", { ctx.startService(Intent(ctx, MonitorService::class.java).setAction(MonitorService.ACTION_EXTEND)); limit = Prefs.limitBytes; limitEnabled = true }, Modifier.weight(1f), Ic.PLUS)
        }

        SectionTitle("Limite de données")
        FCard {
            SettingRow(Ic.GAUGE, "Limiter la consommation", "Alerte, puis coupure assistée à la limite", checked = limitEnabled, onChange = { limitEnabled = it; Prefs.limitEnabled = it })
            if (limitEnabled) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.take(3).forEach { (v, l) -> Chip(l, limit == v, { limit = v; Prefs.limitBytes = v }, Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.drop(3).forEach { (v, l) -> Chip(l, limit == v, { limit = v; Prefs.limitBytes = v }, Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    GhostButton("−100 Mo", { limit = (limit - 100_000_000L).coerceAtLeast(100_000_000L); Prefs.limitBytes = limit })
                    Text(Fmt.bytes(limit), color = FocusColors.Text, style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold))
                    GhostButton("+100 Mo", { limit += 100_000_000L; Prefs.limitBytes = limit })
                }
                SettingRow(Ic.BELL, "Prévenir à ${Prefs.warnPercent} %", null, checked = warn, onChange = { warn = it; Prefs.warnEnabled = it })
            }
        }

        SectionTitle("Barre de notifications")
        FCard {
            SettingRow(Ic.GAUGE, "Débit dans la barre d'état", "Affiche la vitesse en chiffres à côté de l'heure", checked = statusSpeed, onChange = { statusSpeed = it; Prefs.speedInStatusBar = it })
        }

        SectionTitle("Outils développeur")
        FCard {
            val ip = net.tetherIp
            SettingRow(
                Ic.CODE, "Adresse du hotspot", if (ip.isNotEmpty()) "${net.tetherIface} · ${ip}" else "Disponible quand le hotspot est actif",
                onClick = { if (ip.isNotEmpty()) { SystemControls.copy(ctx, ip); copied = true } },
                trailing = if (copied && ip.isNotEmpty()) "Copié" else if (ip.isNotEmpty()) "Copier" else null
            )
            SettingRow(Ic.CODE, "Options pour les développeurs", "Débogage USB / sans fil", onClick = { SystemControls.openDevOptions(ctx) })
            SettingRow(Ic.GAUGE, "Données mobiles", "Ouvrir le panneau de connexion", onClick = { SystemControls.openMobileData(ctx) })
        }
        Label("Le compteur mesure tout le trafic mobile pendant que le hotspot est actif (appareils connectés + téléphone).")
        Spacer(Modifier.height(16.dp))
    }
}
