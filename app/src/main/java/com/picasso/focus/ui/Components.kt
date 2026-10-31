package com.picasso.focus.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.picasso.focus.ui.theme.FocusColors
import com.picasso.focus.ui.theme.LocalAccent

// ------------------------------------------------------------------ icônes (tracés SVG 24x24, style trait arrondi)

enum class Ic(val d: String, val fill: Boolean = false) {
    HOME("M4 11l8-7 8 7v8a1.5 1.5 0 0 1-1.5 1.5h-13A1.5 1.5 0 0 1 4 19z M9.5 20.5V14h5v6.5"),
    WIFI("M2.5 9.5a14 14 0 0 1 19 0 M5.5 13a9.5 9.5 0 0 1 13 0 M8.7 16.3a5 5 0 0 1 6.6 0 M12 20h.01"),
    HOTSPOT("M10 12a2 2 0 1 0 4 0a2 2 0 1 0-4 0z M7.8 8.3a6 6 0 0 0 0 7.4 M16.2 8.3a6 6 0 0 1 0 7.4 M5 5.5a10 10 0 0 0 0 13 M19 5.5a10 10 0 0 1 0 13"),
    BLUETOOTH("M7 7l10 10-5 5V2l5 5L7 17"),
    FOCUS("M4 9V6a2 2 0 0 1 2-2h3 M15 4h3a2 2 0 0 1 2 2v3 M20 15v3a2 2 0 0 1-2 2h-3 M9 20H6a2 2 0 0 1-2-2v-3 M10 12a2 2 0 1 0 4 0a2 2 0 1 0-4 0z"),
    STATS("M4 19V5 M4 19h16 M8 15l4-5 3 3 5-6"),
    USER("M8 8a4 4 0 1 0 8 0a4 4 0 1 0-8 0z M4.5 20a7.5 7.5 0 0 1 15 0"),
    PLUS("M12 5v14 M5 12h14"),
    PLAY("M7 4.5v15l13-7.5z", true),
    PAUSE("M7 5h3.5v14H7z M13.5 5H17v14h-3.5z", true),
    NEXT("M6 5l9 7-9 7z M18 5v14", true),
    PREV("M18 5l-9 7 9 7z M6 5v14", true),
    MUSIC("M9 18V5l11-2v13 M3 18a3 3 0 1 0 6 0a3 3 0 1 0-6 0z M14 16a3 3 0 1 0 6 0a3 3 0 1 0-6 0z"),
    DOWN("M12 4v15 M6 13l6 6 6-6"),
    UP("M12 20V5 M6 11l6-6 6 6"),
    CLOSE("M6 6l12 12 M18 6L6 18"),
    MOON("M20 14.5A8.5 8.5 0 0 1 9.5 4 8.5 8.5 0 1 0 20 14.5z"),
    CLOCK("M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0z M12 7v5l3 2"),
    GAUGE("M4 17a8.5 8.5 0 1 1 16 0 M12 17l4-5"),
    BATTERY("M3 8h15a1 1 0 0 1 1 1v6a1 1 0 0 1-1 1H3a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1z M21 11v2"),
    SHUFFLE("M16 3h5v5 M4 20L21 3 M21 16v5h-5 M15 15l6 6 M4 4l5 5"),
    REPEAT("M17 2l4 4-4 4 M3 11V9a4 4 0 0 1 4-4h14 M7 22l-4-4 4-4 M21 13v2a4 4 0 0 1-4 4H3"),
    CHECK("M5 12.5l4.5 4.5L19 7.5"),
    RESET("M3 12a9 9 0 1 0 3-6.7 M3 4v5h5"),
    POWER("M12 3v9 M6.4 6.6a8 8 0 1 0 11.2 0"),
    BELL("M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9 M10.3 21a1.94 1.94 0 0 0 3.4 0"),
    LIST("M8 6h13 M8 12h13 M8 18h13 M3 6h.01 M3 12h.01 M3 18h.01"),
    BACK("M15 18l-6-6 6-6"),
    CODE("M16 18l6-6-6-6 M8 6l-6 6 6 6")
}

@Composable
fun FIcon(ic: Ic, modifier: Modifier = Modifier, tint: Color = FocusColors.Text, size: Dp = 24.dp, strokeWidth: Float = 2f) {
    val path = remember(ic) { PathParser().parsePathString(ic.d).toPath() }
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 24f
        scale(s, s, pivot = Offset.Zero) {
            drawPath(path, tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            if (ic.fill) drawPath(path, tint)
        }
    }
}

// ------------------------------------------------------------------ logo

private const val LOGO_BRACKETS = "M3 8V5.5A2.5 2.5 0 0 1 5.5 3H8 M16 3h2.5A2.5 2.5 0 0 1 21 5.5V8 M21 16v2.5a2.5 2.5 0 0 1-2.5 2.5H16 M8 21H5.5A2.5 2.5 0 0 1 3 18.5V16"
private const val LOGO_F = "M11.5 17.7V10.05C11.5 7.65 12.86 6.3 15.26 6.3 M8.74 10.2H14.59"

/** Logo Focus : un « f » arrondi dans un viseur. */
@Composable
fun FocusLogo(size: Dp, modifier: Modifier = Modifier, tile: Boolean = true) {
    val b = remember { PathParser().parsePathString(LOGO_BRACKETS).toPath() }
    val f = remember { PathParser().parsePathString(LOGO_F).toPath() }
    val accent = LocalAccent.current
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        if (tile) {
            drawRoundRect(
                brush = Brush.linearGradient(listOf(FocusColors.Amber, FocusColors.OrangeDeep), Offset.Zero, Offset(s, s)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.28f)
            )
        }
        val col = if (tile) Color.White else accent
        scale(s / 24f, s / 24f, pivot = Offset.Zero) {
            drawPath(b, col, style = Stroke(1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(f, col, style = Stroke(2.1f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

// ------------------------------------------------------------------ conteneurs

@Composable
fun FCard(modifier: Modifier = Modifier, padding: Dp = 18.dp, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    var m = modifier.clip(shape).background(FocusColors.Surface).border(1.dp, FocusColors.Outline.copy(alpha = 0.6f), shape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(m.padding(padding), content = content)
}

@Composable
fun Title(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, color = FocusColors.Text, style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp))
}

@Composable
fun Label(text: String, modifier: Modifier = Modifier, color: Color = FocusColors.Dim, align: TextAlign = TextAlign.Unspecified) {
    Text(text, modifier, color = color, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium), textAlign = align)
}

@Composable
fun SectionTitle(text: String) {
    Text(text.uppercase(), Modifier.padding(start = 6.dp, top = 8.dp), color = FocusColors.Dim, style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp))
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = LocalAccent.current
    val shape = RoundedCornerShape(50)
    Box(
        modifier.clip(shape).background(if (selected) accent else FocusColors.SurfaceHi).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) Color.White else FocusColors.Dim, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
    }
}

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val accent = LocalAccent.current
    Row(modifier.clip(RoundedCornerShape(50)).background(FocusColors.Surface).padding(4.dp)) {
        options.forEachIndexed { i, o ->
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(50)).background(if (i == selected) accent else Color.Transparent)
                    .clickable { onSelect(i) }.padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(o, color = if (i == selected) Color.White else FocusColors.Dim, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
            }
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = LocalAccent.current, icon: Ic? = null) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(color).clickable(onClick = onClick).padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) { FIcon(icon, tint = Color.White, size = 20.dp); Spacer(Modifier.width(8.dp)) }
        Text(text, color = Color.White, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold))
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: Ic? = null) {
    Row(
        modifier.clip(RoundedCornerShape(16.dp)).background(FocusColors.SurfaceHi).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) { FIcon(icon, tint = FocusColors.Text, size = 18.dp); Spacer(Modifier.width(8.dp)) }
        Text(text, color = FocusColors.Text, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
    }
}

@Composable
fun FSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val accent = LocalAccent.current
    Switch(
        checked = checked, onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White, checkedTrackColor = accent,
            uncheckedThumbColor = FocusColors.Dim, uncheckedTrackColor = FocusColors.SurfaceHi, uncheckedBorderColor = FocusColors.Outline
        )
    )
}

@Composable
fun SettingRow(icon: Ic, title: String, subtitle: String? = null, checked: Boolean? = null, onChange: ((Boolean) -> Unit)? = null, onClick: (() -> Unit)? = null, trailing: String? = null) {
    val accent = LocalAccent.current
    var m = Modifier.fillMaxWidth()
    if (onClick != null) m = m.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick)
    Row(m.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            FIcon(icon, tint = accent, size = 20.dp)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = FocusColors.Text, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
            if (subtitle != null) Text(subtitle, color = FocusColors.Dim, style = TextStyle(fontSize = 12.sp))
        }
        if (checked != null && onChange != null) FSwitch(checked, onChange)
        if (trailing != null) Text(trailing, color = accent, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold))
    }
}

@Composable
fun ToggleTile(icon: Ic, title: String, subtitle: String, on: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = LocalAccent.current
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier.clip(shape).background(if (on) accent else FocusColors.Surface)
            .border(1.dp, if (on) Color.Transparent else FocusColors.Outline.copy(alpha = 0.6f), shape)
            .clickable(onClick = onClick).padding(16.dp)
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(if (on) Color.White.copy(alpha = 0.22f) else FocusColors.SurfaceHi), contentAlignment = Alignment.Center) {
            FIcon(icon, tint = if (on) Color.White else FocusColors.Dim, size = 20.dp)
        }
        Spacer(Modifier.height(14.dp))
        Text(title, color = Color.White, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold))
        Text(subtitle, color = if (on) Color.White.copy(alpha = 0.8f) else FocusColors.Dim, style = TextStyle(fontSize = 12.sp))
    }
}

@Composable
fun StatTile(icon: Ic, label: String, value: String, unit: String = "", modifier: Modifier = Modifier) {
    val accent = LocalAccent.current
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(FocusColors.Surface).border(1.dp, FocusColors.Outline.copy(alpha = 0.6f), RoundedCornerShape(20.dp)).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FIcon(icon, tint = accent, size = 16.dp)
            Spacer(Modifier.width(6.dp))
            Label(label)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = FocusColors.Text, style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"))
            if (unit.isNotEmpty()) {
                Spacer(Modifier.width(4.dp))
                Text(unit, Modifier.padding(bottom = 3.dp), color = FocusColors.Dim, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium))
            }
        }
    }
}

// ------------------------------------------------------------------ jauge circulaire

@Composable
fun RingGauge(progress: Float, modifier: Modifier = Modifier, stroke: Dp = 16.dp, color: Color = LocalAccent.current, content: @Composable BoxScope.() -> Unit = {}) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(700), label = "ring")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val tl = Offset(sw / 2, sw / 2)
            val sz = Size(size.width - sw, size.height - sw)
            drawArc(FocusColors.SurfaceHi, -90f, 360f, false, tl, sz, style = Stroke(sw))
            if (p > 0.001f) {
                drawArc(color.copy(alpha = 0.22f), -90f, 360f * p, false, tl, sz, style = Stroke(sw * 1.6f, cap = StrokeCap.Round))
                drawArc(color, -90f, 360f * p, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
            }
        }
        content()
    }
}

// ------------------------------------------------------------------ graphiques

@Composable
fun AreaChart(values: List<Float>, modifier: Modifier = Modifier, color: Color = LocalAccent.current) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val max = (values.maxOrNull() ?: 0f).let { if (it <= 0f) 1f else it * 1.15f }
        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 10f))
        for (i in 0..3) {
            val y = h * i / 3f
            drawLine(FocusColors.Outline, Offset(0f, y), Offset(w, y), strokeWidth = 1f, pathEffect = dash)
        }
        if (values.size < 2) return@Canvas
        val step = w / (values.size - 1)
        val pts = values.mapIndexed { i, v -> Offset(i * step, h - (v / max) * h) }
        val line = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) {
                val mx = (pts[i - 1].x + pts[i].x) / 2f
                cubicTo(mx, pts[i - 1].y, mx, pts[i].y, pts[i].x, pts[i].y)
            }
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent)))
        drawPath(line, color, style = Stroke(5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        val last = pts.last()
        drawCircle(color.copy(alpha = 0.3f), 14f, last)
        drawCircle(color, 7f, last)
    }
}

@Composable
fun BarChart(values: List<Float>, labels: List<String>, highlight: Int, modifier: Modifier = Modifier, color: Color = LocalAccent.current) {
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val n = values.size
            if (n == 0) return@Canvas
            val max = (values.maxOrNull() ?: 0f).let { if (it <= 0f) 1f else it }
            val slot = size.width / n
            val bw = slot * 0.46f
            for (i in 0 until n) {
                val hh = (values[i] / max) * size.height
                val x = i * slot + (slot - bw) / 2
                drawRoundRect(FocusColors.SurfaceHi, Offset(x, 0f), Size(bw, size.height), androidx.compose.ui.geometry.CornerRadius(bw / 2))
                if (hh > 0f) {
                    drawRoundRect(
                        if (i == highlight) color else color.copy(alpha = 0.55f),
                        Offset(x, size.height - hh.coerceAtLeast(bw)), Size(bw, hh.coerceAtLeast(bw)),
                        androidx.compose.ui.geometry.CornerRadius(bw / 2)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { i, l ->
                Text(l, Modifier.weight(1f), textAlign = TextAlign.Center, color = if (i == highlight) FocusColors.Text else FocusColors.Dim, style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium))
            }
        }
    }
}

@Composable
fun StatusDot(on: Boolean) {
    Box(Modifier.size(8.dp).clip(CircleShape).background(if (on) FocusColors.Ok else FocusColors.Dim))
}

fun pad(h: Dp = 20.dp) = PaddingValues(horizontal = h)
