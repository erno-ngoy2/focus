package com.picasso.focus.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.picasso.focus.ui.theme.FocusColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Splash animé : le viseur se referme, le « f » se dessine, puis le nom apparaît. Même visuel que le splash système (fond noir + logo orange). */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    val brackets = remember { PathParser().parsePathString("M3 8V5.5A2.5 2.5 0 0 1 5.5 3H8 M16 3h2.5A2.5 2.5 0 0 1 21 5.5V8 M21 16v2.5a2.5 2.5 0 0 1-2.5 2.5H16 M8 21H5.5A2.5 2.5 0 0 1 3 18.5V16").toPath() }
    val stem = remember { PathParser().parsePathString("M11.5 17.7V10.05C11.5 7.65 12.86 6.3 15.26 6.3").toPath() }
    val bar = remember { PathParser().parsePathString("M8.74 10.2H14.59").toPath() }
    val drawF = remember { Animatable(0f) }
    val focusIn = remember { Animatable(0.82f) }
    val glow = remember { Animatable(0f) }
    val text = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { focusIn.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
        launch { glow.animateTo(1f, tween(1200)) }
        launch { drawF.animateTo(1f, tween(1000, 250, FastOutSlowInEasing)) }
        launch { text.animateTo(1f, tween(600, 900)) }
        delay(1900)
        onDone()
    }

    Box(Modifier.fillMaxSize().background(FocusColors.Bg), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                Brush.radialGradient(listOf(FocusColors.Orange.copy(alpha = 0.28f * glow.value), androidx.compose.ui.graphics.Color.Transparent), center, size.minDimension * 0.55f),
                radius = size.minDimension * 0.55f, center = center
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Canvas(Modifier.size(160.dp).graphicsLayer { scaleX = focusIn.value; scaleY = focusIn.value }) {
                val u = size.minDimension / 24f
                val measure = PathMeasure()
                scale(u, u, pivot = Offset.Zero) {
                    drawPath(brackets, FocusColors.Orange, style = Stroke(1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    for ((p, frac) in listOf(stem to drawF.value, bar to (drawF.value * 1.6f - 0.6f).coerceIn(0f, 1f))) {
                        measure.setPath(p, false)
                        val seg = Path()
                        measure.getSegment(0f, measure.length * frac, seg, true)
                        drawPath(seg, FocusColors.Orange, style = Stroke(2.1f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Column(Modifier.graphicsLayer { alpha = text.value; translationY = (1f - text.value) * 24f }, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("focus", color = FocusColors.Text, style = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1.5).sp))
                Text("par MalakiLabs", Modifier.padding(top = 4.dp), color = FocusColors.Dim, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp))
            }
        }
    }
}
