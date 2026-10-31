package com.picasso.focus

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.picasso.focus.data.FocusState
import com.picasso.focus.data.Prefs
import com.picasso.focus.net.SystemControls
import com.picasso.focus.service.MonitorService
import com.picasso.focus.ui.FocusRoot
import com.picasso.focus.ui.GhostButton
import com.picasso.focus.ui.theme.FocusColors
import com.picasso.focus.ui.theme.FocusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        // Si la dernière session a planté, on affiche l'erreur au lieu de relancer l'appli.
        val crash = CrashLog.read(this)
        if (crash != null) {
            setContent {
                FocusTheme(FocusColors.Orange) {
                    Column(Modifier.fillMaxSize().background(FocusColors.Bg).statusBarsPadding().padding(20.dp)) {
                        Text("Focus a rencontré une erreur", color = FocusColors.Orange, style = TextStyle(fontSize = 20.sp))
                        Spacer(Modifier.height(10.dp))
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                            Text(crash, color = FocusColors.Text, style = TextStyle(fontSize = 11.sp))
                        }
                        Spacer(Modifier.height(10.dp))
                        Row {
                            GhostButton("Copier", { SystemControls.copy(this@MainActivity, crash) })
                            Spacer(Modifier.width(10.dp))
                            GhostButton("Réessayer", { CrashLog.clear(this@MainActivity); recreate() })
                        }
                    }
                }
            }
            return
        }

        FocusState.alwaysOrange.value = Prefs.alwaysOrange
        if (Prefs.watchHotspot) runCatching { MonitorService.start(this) }
        val tab = intent.getIntExtra("tab", 0)
        val player = intent.getBooleanExtra("player", false)
        setContent { FocusRoot(tab, player) }
    }
}
