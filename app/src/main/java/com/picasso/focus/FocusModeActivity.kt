package com.picasso.focus

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.picasso.focus.data.FocusController
import com.picasso.focus.data.FocusState
import com.picasso.focus.data.Prefs
import com.picasso.focus.ui.StandbyScreen
import com.picasso.focus.ui.theme.FocusColors
import com.picasso.focus.ui.theme.FocusTheme

/** Écran de veille paysage du mode concentration. */
class FocusModeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true) }
        if (Prefs.keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= 28) {
            window.attributes = window.attributes.apply { layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES }
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }

        val s = FocusState.session.value
        if (!s.active && !s.finished) FocusController.start(this, Prefs.focusMinutes)

        setContent {
            FocusTheme(FocusColors.Orange) {
                StandbyScreen(setBrightness = ::setBrightness, onExit = ::endAndClose)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        FocusState.acquireLive(this)
    }

    override fun onStop() {
        FocusState.releaseLive()
        super.onStop()
    }

    private fun setBrightness(level: Float) {
        val lp = window.attributes
        lp.screenBrightness = level
        window.attributes = lp
    }

    private fun endAndClose() {
        FocusController.stop(this)
        FocusController.dismissFinished()
        finish()
    }
}
