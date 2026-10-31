package com.picasso.focus.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.picasso.focus.data.FocusState
import com.picasso.focus.data.Fmt
import com.picasso.focus.data.History
import com.picasso.focus.ui.AreaChart
import com.picasso.focus.ui.BarChart
import com.picasso.focus.ui.FCard
import com.picasso.focus.ui.Ic
import com.picasso.focus.ui.Label
import com.picasso.focus.ui.Segmented
import com.picasso.focus.ui.StatTile
import com.picasso.focus.ui.Title
import com.picasso.focus.ui.theme.FocusColors
import java.time.LocalDate
import java.time.format.TextStyle as DayStyle
import java.util.Locale

@Composable
fun StatsScreen() {
    var kind by remember { mutableIntStateOf(0) }  // 0 données, 1 concentration
    var range by remember { mutableIntStateOf(0) } // 0 semaine, 1 mois
    val net by FocusState.net.collectAsState()
    val session by FocusState.session.collectAsState()

    val n = if (range == 0) 7 else 30
    val days = (n - 1 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
    val map = if (kind == 0) History.net() else History.focus()
    val today = LocalDate.now().toString()
    val values = days.map { d ->
        val k = d.toString()
        var v = map[k] ?: 0L
        if (k == today) {
            if (kind == 0 && net.hotspotOn) v += net.total
            if (kind == 1 && session.active) v += (System.currentTimeMillis() - session.startEpochMs) / 1000
        }
        v
    }
    val total = values.sum()
    val avg = total / n
    val best = values.maxOrNull() ?: 0L
    fun f(v: Long) = if (kind == 0) Fmt.bytes(v) else Fmt.hm(v)
    val labels = if (range == 0) days.map { it.dayOfWeek.getDisplayName(DayStyle.SHORT, Locale.getDefault()).take(3) } else emptyList()

    Column(Modifier.verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Title("Statistiques", Modifier.padding(top = 12.dp))
        Segmented(listOf("Données hotspot", "Concentration"), kind, { kind = it })
        Segmented(listOf("7 jours", "30 jours"), range, { range = it })

        FCard(Modifier.fillMaxWidth()) {
            Text(f(avg), color = FocusColors.Text, style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"))
            Label(if (kind == 0) "Moyenne quotidienne de données" else "Moyenne quotidienne de concentration")
            Spacer(Modifier.height(16.dp))
            if (range == 0) BarChart(values.map { it.toFloat() }, labels, n - 1, Modifier.fillMaxWidth().height(180.dp))
            else AreaChart(values.map { it.toFloat() }, Modifier.fillMaxWidth().height(180.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(if (kind == 0) Ic.DOWN else Ic.CLOCK, "Total", f(total), "", Modifier.weight(1f))
            StatTile(Ic.GAUGE, "Meilleur jour", f(best), "", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(Ic.CHECK, "Jours actifs", values.count { it > 0 }.toString(), "/ $n", Modifier.weight(1f))
            StatTile(Ic.STATS, "Aujourd'hui", f(values.last()), "", Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
    }
}
