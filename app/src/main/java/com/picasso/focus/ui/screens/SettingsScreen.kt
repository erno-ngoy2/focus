package com.picasso.focus.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.core.content.ContextCompat
import com.picasso.focus.data.FocusState
import com.picasso.focus.data.Prefs
import com.picasso.focus.net.SystemControls
import com.picasso.focus.service.MonitorService
import com.picasso.focus.ui.FCard
import com.picasso.focus.ui.FocusLogo
import com.picasso.focus.ui.Ic
import com.picasso.focus.ui.Label
import com.picasso.focus.ui.SectionTitle
import com.picasso.focus.ui.SettingRow
import com.picasso.focus.ui.Title
import com.picasso.focus.ui.theme.FocusColors

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    var watch by remember { mutableStateOf(Prefs.watchHotspot) }
    var root by remember { mutableStateOf(Prefs.autoCutRoot) }
    var orange by remember { mutableStateOf(Prefs.alwaysOrange) }
    var notifOk by remember { mutableStateOf(Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifOk = it }

    Column(Modifier.verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Title("Profil", Modifier.padding(top = 12.dp))
        FCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FocusLogo(64.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Focus", color = FocusColors.Text, style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold))
                    Label("par MalakiLabs · v1.0")
                }
            }
        }

        SectionTitle("Hotspot")
        FCard {
            SettingRow(Ic.HOTSPOT, "Veille du point d'accès", "Démarre le compteur automatiquement. Coût quasi nul.", checked = watch, onChange = {
                watch = it; Prefs.watchHotspot = it
                if (it) MonitorService.start(ctx)
            })
            SettingRow(Ic.POWER, "Coupure automatique (root)", "Tente de couper le hotspot à la limite. Sans root : alerte + raccourci.", checked = root, onChange = { root = it; Prefs.autoCutRoot = it })
        }

        SectionTitle("Apparence")
        FCard {
            SettingRow(Ic.FOCUS, "Toujours en orange", "Par défaut, le thème passe à l'orange en mode concentration.", checked = orange, onChange = {
                orange = it; Prefs.alwaysOrange = it; FocusState.alwaysOrange.value = it
            })
        }

        SectionTitle("Autorisations")
        FCard {
            SettingRow(Ic.BELL, "Notifications", if (notifOk) "Autorisées" else "Nécessaires pour le compteur et les alertes",
                onClick = {
                    if (!notifOk && Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else SystemControls.start(ctx, Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
                }, trailing = if (notifOk) "OK" else "Autoriser")
            SettingRow(Ic.MOON, "Ne pas déranger", if (SystemControls.dndGranted(ctx)) "Accès accordé" else "Accès requis pour le mode concentration",
                onClick = { SystemControls.start(ctx, Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) })
            SettingRow(Ic.CLOCK, "Économie d'énergie", "Exclure Focus des optimisations si le compteur s'arrête",
                onClick = { SystemControls.start(ctx, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) })
            SettingRow(Ic.GAUGE, "Infos de l'application", null,
                onClick = { SystemControls.start(ctx, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))) })
        }

        Label("Android n'autorise pas une application à activer le hotspot, le Wi‑Fi ou le Bluetooth à votre place : Focus ouvre le bon panneau en un tap et affiche l'état réel.")
        Spacer(Modifier.height(24.dp))
    }
}
