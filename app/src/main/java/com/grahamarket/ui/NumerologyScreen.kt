package com.grahamarket.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.grahamarket.astro.NumerologyEngine
import com.grahamarket.data.NumerologyReport

@Composable
fun NumerologyScreen(
    state: UiState,
    onQueryChange: (String) -> Unit,
    onAnalyze: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Numerology", style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold, color = StarGold)
        Text("Same 7 & 30-day prediction, computed via Chaldean numerology.",
            style = MaterialTheme.typography.bodyMedium, color = MutedText)

        DisclaimerBanner()

        OutlinedTextField(
            value = state.numQuery,
            onValueChange = onQueryChange,
            label = { Text("Stock symbol (e.g. RELIANCE, TCS)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onAnalyze() })
        )
        Button(onClick = onAnalyze, modifier = Modifier.fillMaxWidth(), enabled = !state.numLoading) {
            Icon(Icons.Filled.Calculate, contentDescription = null)
            Text("  Calculate")
        }

        if (state.numLoading) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator(color = NebulaViolet)
            }
        }

        state.numReport?.let { NumReportView(it) }
    }
}

@Composable
private fun NumReportView(report: NumerologyReport) {
    val r = report.reading
    SectionCard(title = r.symbol) {
        Spacer(Modifier.height(8.dp))
        if (report.quote != null) {
            val q = report.quote
            Text("${q.currency} ${"%,.2f".format(q.price)}",
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Live price via ${q.source}", color = MutedText, style = MaterialTheme.typography.labelSmall)
        } else {
            Text(report.quoteError ?: "Price unavailable.", color = BearRed, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(14.dp)); Divider(color = CosmicSurfaceVariant); Spacer(Modifier.height(14.dp))

        // Numerology numbers
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            NumberChip("Name", r.nameNumber, r.nameRuler)
            NumberChip("Date", r.dateNumber, r.dateRuler)
            NumberChip("Compound", r.compoundNumber, "")
        }

        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HorizonChip("7-day", report.horizon7.headlinePercent, report.horizon7.direction, Modifier.weight(1f))
            HorizonChip("30-day", report.horizon30.headlinePercent, report.horizon30.direction, Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))
        Text(r.explanation, color = MutedText, style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(16.dp))
        DayByDayNumSection(report)

        Spacer(Modifier.height(12.dp))
        Text(
            "Numerology maps the symbol's letters and each date to root numbers " +
                "(1–9) with classic planetary rulerships. Educational only — no proven " +
                "predictive validity.",
            style = MaterialTheme.typography.labelSmall, color = MutedText
        )
    }
}

@Composable
private fun NumberChip(label: String, number: Int, ruler: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = MutedText, style = MaterialTheme.typography.labelSmall)
        Text("$number", style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold, color = NebulaViolet)
        if (ruler.isNotEmpty()) Text(ruler, color = StarGold, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun HorizonChip(label: String, percent: Double, direction: String, modifier: Modifier) {
    val color = directionColor(direction)
    Column(modifier.padding(2.dp)) {
        Text(label, color = MutedText, style = MaterialTheme.typography.labelMedium)
        Text("${if (percent >= 0) "+" else ""}$percent%",
            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
        Text(direction, color = color, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun DayByDayNumSection(report: NumerologyReport) {
    var horizon by remember { mutableIntStateOf(7) }
    HorizonToggle(selected = horizon, options = listOf(7, 30), onSelect = { horizon = it })
    Spacer(Modifier.height(10.dp))
    Text("Day-by-day prediction", color = NebulaViolet, style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(6.dp))

    val h = if (horizon == 7) report.horizon7 else report.horizon30
    h.days.forEach { d ->
        DayRow(
            dayIndex = d.dayIndex,
            dateLabel = shortDate(d.date.time),
            dailyPercent = d.dailyPercent,
            cumulativePercent = d.cumulativePercent,
            direction = d.direction,
            detail = "Day number ${d.dayNumber}"
        )
    }
}
