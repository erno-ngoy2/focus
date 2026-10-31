package com.picasso.focus.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.picasso.focus.data.FocusState
import com.picasso.focus.ui.screens.FocusScreen
import com.picasso.focus.ui.screens.HomeScreen
import com.picasso.focus.ui.screens.NetworkScreen
import com.picasso.focus.ui.screens.PlayerScreen
import com.picasso.focus.ui.screens.SettingsScreen
import com.picasso.focus.ui.screens.StatsScreen
import com.picasso.focus.ui.theme.FocusColors
import com.picasso.focus.ui.theme.FocusTheme
import com.picasso.focus.ui.theme.LocalAccent

@Composable
fun FocusRoot(startTab: Int, openPlayer: Boolean) {
    val session by FocusState.session.collectAsState()
    val alwaysOrange by FocusState.alwaysOrange.collectAsState()
    val accent by animateColorAsState(if (session.active || alwaysOrange) FocusColors.Orange else FocusColors.Blue, tween(700), label = "accent")

    val notif = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    LaunchedEffect(Unit) {
        val p = ArrayList<String>()
        if (Build.VERSION.SDK_INT >= 33) p.add(Manifest.permission.POST_NOTIFICATIONS)
        if (p.isNotEmpty()) notif.launch(p.toTypedArray())
    }

    FocusTheme(accent) {
        var splash by rememberSaveable { mutableStateOf(true) }
        var tab by rememberSaveable { mutableIntStateOf(startTab) }
        var player by rememberSaveable { mutableStateOf(openPlayer) }
        BackHandler(enabled = player) { player = false }
        BackHandler(enabled = !player && tab != 0) { tab = 0 }

        Box(Modifier.fillMaxSize().background(FocusColors.Bg)) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    Crossfade(tab, animationSpec = tween(220), label = "tab") { t ->
                        when (t) {
                            0 -> HomeScreen(onOpenPlayer = { player = true }, onOpenNetwork = { tab = 1 })
                            1 -> NetworkScreen()
                            2 -> FocusScreen()
                            3 -> StatsScreen()
                            else -> SettingsScreen()
                        }
                    }
                }
                BottomBar(tab) { tab = it }
            }
            AnimatedVisibility(player, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                PlayerScreen(onBack = { player = false })
            }
            AnimatedVisibility(splash, exit = fadeOut(tween(500))) {
                SplashScreen { splash = false }
            }
        }
    }
}

@Composable
private fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val accent = LocalAccent.current
    Row(
        Modifier.fillMaxWidth().background(FocusColors.Surface.copy(alpha = 0.96f)).border(0.5.dp, FocusColors.Outline).navigationBarsPadding().height(68.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        NavItem(Ic.HOME, "Accueil", selected == 0, Modifier.weight(1f)) { onSelect(0) }
        NavItem(Ic.HOTSPOT, "Réseau", selected == 1, Modifier.weight(1f)) { onSelect(1) }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(Modifier.size(54.dp).background(accent, CircleShape).clickable { onSelect(2) }, contentAlignment = Alignment.Center) {
                FIcon(if (selected == 2) Ic.FOCUS else Ic.PLUS, tint = Color.White, size = 26.dp, strokeWidth = 2.4f)
            }
        }
        NavItem(Ic.STATS, "Stats", selected == 3, Modifier.weight(1f)) { onSelect(3) }
        NavItem(Ic.USER, "Profil", selected == 4, Modifier.weight(1f)) { onSelect(4) }
    }
}

@Composable
private fun NavItem(ic: Ic, label: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val accent = LocalAccent.current
    Column(modifier.clickable(onClick = onClick).padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        FIcon(ic, tint = if (on) accent else FocusColors.Dim, size = 24.dp)
        Text(label, color = if (on) accent else FocusColors.Dim, style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold))
    }
}
