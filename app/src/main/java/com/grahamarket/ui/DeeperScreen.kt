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
import androidx.compose.material.icons.filled.AutoGraph
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
import com.grahamarket.astro.BacktestEngine
import com.grahamarket.astro.DeeperJyotishEngine
import com.grahamarket.data.BacktestBundle
import com.grahamarket.data.DeeperReport
import kotlin.math.abs

@Composable
fun DeeperScreen(
    state: UiState,
    onQueryChange: (String) -> Unit,
    onAnalyze: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Deeper Jyotish + Reality Check", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, color = StarGold)
        Text("Authentic dasha + transit-to-natal forecast, shown next to an honest " +
            "backtest of how it actually performs.",
            style = MaterialTheme.typography.bodyMedium, color = MutedText)

        DisclaimerBanner()

        OutlinedTextField(
            value = state.deepQuery,
            onValueChange = onQueryChange,
            label = { Text("Stock symbol (e.g. RELIANCE, TCS)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onAnalyze() })
        )
        Button(onClick = onAnalyze, modifier = Modifier.fillMaxWidth(), enabled = !state.deepLoading) {
            Icon(Icons.Filled.AutoGraph, contentDescription = null)
            Text("  Analyze (both)")
        }

        if (state.deepLoading) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator(color = NebulaViolet)
            }
        }

        state.deeperReport?.let { DeeperReportView(it) }
        state.backtest?.let { RealityCheckView(it) }
    }
}

@Composable
private fun DeeperReportView(report: DeeperReport) {
    val r = report.reading
    SectionCard(title = "${r.symbol} · Deeper Jyotish (B)") {
        Spacer(Modifier.height(8.dp))
        if (report.quote != null) {
            val q = report.quote
            Text("${q.currency} ${"%,.2f".format(q.price)}",
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Live price via ${q.source}", color = MutedText, style = MaterialTheme.typography.labelSmall)
        } else {
            Text(report.quoteError ?: "Price unavailable.", color = BearRed)
        }

        Spacer(Modifier.height(14.dp)); Divider(color = CosmicSurfaceVariant); Spacer(Modifier.height(14.dp))

        // Dasha + natal
        Text("Vimshottari period (from inferred natal chart)", color = NebulaViolet,
            style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        Text("Mahadasha: ${r.mahadasha.displayName}  (${r.mahaWindow})")
        Text("Antardasha: ${r.antardasha.displayName}", color = MutedText, style = MaterialTheme.typography.bodySmall)
        Text("Natal Moon: ${r.natalMoonRashi} (${r.natalMoonNakshatra})",
            color = MutedText, style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HzChip("7-day", report.horizon7.headlinePercent, report.horizon7.direction, Modifier.weight(1f))
            HzChip("30-day", report.horizon30.headlinePercent, report.horizon30.direction, Modifier.weight(1f))
        }

        Spacer(Modifier.height(14.dp))
        Text("Top transit / dasha factors", color = NebulaViolet, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        r.factors.take(7).forEach { f ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(f.label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                Text("${if (f.contribution >= 0) "+" else "−"}${"%.2f".format(abs(f.contribution))}",
                    color = if (f.contribution >= 0) BullGreen else BearRed,
                    style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(Modifier.height(14.dp))
        DeeperDayByDay(report)
    }
}

@Composable
private fun DeeperDayByDay(report: DeeperReport) {
    var horizon by remember { mutableIntStateOf(7) }
    HorizonToggle(selected = horizon, options = listOf(7, 30), onSelect = { horizon = it })
    Spacer(Modifier.height(10.dp))
    Text("Day-by-day", color = NebulaViolet, style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(6.dp))
    val h = if (horizon == 7) report.horizon7 else report.horizon30
    h.days.forEach { d ->
        DayRow(
            dayIndex = d.dayIndex,
            dateLabel = shortDate(d.date.time),
            dailyPercent = d.dailyPercent,
            cumulativePercent = d.cumulativePercent,
            direction = d.direction,
            detail = "Maha ${d.maha.displayName} / Antar ${d.antar.displayName}"
        )
    }
}

@Composable
private fun RealityCheckView(bundle: BacktestBundle) {
    SectionCard(title = "Reality Check (A) · ${bundle.graha.symbol}") {
        Spacer(Modifier.height(8.dp))
        Text(
            "How well did each engine's past daily calls actually line up with real " +
                "market direction? A coin-flip is ~50% hit-rate and 0.0 correlation.",
            color = MutedText, style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(12.dp))

        BacktestRow("Graha (simple)", bundle.graha)
        Divider(color = CosmicSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
        BacktestRow("Numerology", bundle.numerology)
        Divider(color = CosmicSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
        BacktestRow("Deeper Jyotish", bundle.deeper)

        Spacer(Modifier.height(12.dp))
        Text(bundle.graha.verdict, color = StarGold, style = MaterialTheme.typography.bodySmall)

        if (bundle.graha.syntheticReference) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Note: this build compares against a synthetic reference series " +
                    "(no bundled historical-price feed). The statistics are computed for " +
                    "real; wire in a historical-price API to backtest against actual prices.",
                color = MutedText, style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun BacktestRow(label: String, res: BacktestEngine.Result) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Medium)
            Text("over ${res.days} days", color = MutedText, style = MaterialTheme.typography.labelSmall)
        }
        Column(horizontalAlignment = Alignment.End) {
            val nearChance = res.hitRatePercent in 45.0..55.0
            Text("Hit-rate ${res.hitRatePercent}%",
                color = if (nearChance) MutedText else StarGold,
                fontWeight = FontWeight.SemiBold)
            Text("corr ${res.correlation}  ·  MAE ${res.meanAbsError}%",
                color = MutedText, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun HzChip(label: String, percent: Double, direction: String, modifier: Modifier) {
    val color = directionColor(direction)
    Column(modifier.padding(2.dp)) {
        Text(label, color = MutedText, style = MaterialTheme.typography.labelMedium)
        Text("${if (percent >= 0) "+" else ""}$percent%",
            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
        Text(direction, color = color, style = MaterialTheme.typography.labelMedium)
    }
}
