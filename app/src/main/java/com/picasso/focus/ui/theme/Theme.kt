package com.picasso.focus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalAccent = staticCompositionLocalOf { FocusColors.Orange }

/** Thème sombre ; l'accent est bleu au repos et devient orange quand le mode concentration est actif. */
@Composable
fun FocusTheme(accent: Color = FocusColors.Orange, content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = accent,
        onPrimary = Color.White,
        background = FocusColors.Bg,
        onBackground = FocusColors.Text,
        surface = FocusColors.Surface,
        onSurface = FocusColors.Text,
        surfaceVariant = FocusColors.SurfaceHi,
        outline = FocusColors.Outline
    )
    CompositionLocalProvider(LocalAccent provides accent) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
