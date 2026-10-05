package com.grahamarket.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.grahamarket.astro.ForecastEngine
import com.grahamarket.data.SymbolReport
import kotlin.math.abs

@Composable
fun HomeScreen(
    state: UiState,
    onQueryChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    onAddToWatchlist: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "GrahaMarket",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = StarGold
        )
        Text(
            "Planetary (graha) outlook for Indian stocks · 7 & 30-day view",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText
        )

        DisclaimerBanner()

        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            label = { Text("Stock symbol (e.g. RELIANCE, TCS, INFY)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onAnalyze() })
        )
        Button(
            onClick = onAnalyze,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.loading
        ) {
            Icon(Icons.Filled.Search, contentDescription = null)
            Text("  Analyze")
        }

        if (state.loading) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator(color = NebulaViolet)
            }
        }

        state.report?.let { ReportView(it, onAddToWatchlist) }

        Text(
            "NSE symbols are used by default (RELIANCE → RELIANCE.NS). " +
                "Add .BO for BSE. Change the data provider in Settings.",
            style = MaterialTheme.typography.labelSmall,
            color = MutedText
        )
    }
}

@Composable
private fun ReportView(report: SymbolReport, onAddToWatchlist: () -> Unit) {
    val o = report.outlook
    val color = directionColor(o.direction)

    SectionCard(title = o.symbol) {
        Spacer(Modifier.height(8.dp))
        if (report.quote != null) {
            val q = report.quote
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "${q.currency} ${"%,.2f".format(q.price)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                q.changePercent?.let {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "${if (it >= 0) "+" else ""}${"%.2f".format(it)}% today",
                        color = if (it >= 0) BullGreen else BearRed,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Text("Live price via ${q.source}", color = MutedText, style = MaterialTheme.typography.labelSmall)

            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onAddToWatchlist, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.BookmarkAdd, contentDescription = null)
                Text("  Add to Watchlist")
            }
        } else {
            Text(report.quoteError ?: "Price unavailable.", color = BearRed, style = MaterialTheme.typography.bodyMedium)
            Text(
                "The forecast below is still computed. Try another symbol or switch provider in Settings.",
                color = MutedText, style = MaterialTheme.typography.labelSmall
            )
        }

        Spacer(Modifier.height(14.dp)); Divider(color = CosmicSurfaceVariant); Spacer(Modifier.height(14.dp))

        // Dual-horizon headline
        Text("Outlook", color = NebulaViolet, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HorizonHeadlineChip("7-day", report.forecast7.headlinePercent, report.forecast7.direction, Modifier.weight(1f))
            HorizonHeadlineChip("30-day", report.forecast30.headlinePercent, report.forecast30.direction, Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))
        DayByDaySection(report.forecast7, report.forecast30)

        Spacer(Modifier.height(14.dp)); Divider(color = CosmicSurfaceVariant); Spacer(Modifier.height(14.dp))

        Text("Contributing graha factors", color = NebulaViolet, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        o.factors.take(8).forEach { f ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(f.label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                Text(
                    "${if (f.contribution >= 0) "+" else "−"}${"%.2f".format(abs(f.contribution))}",
                    color = if (f.contribution >= 0) BullGreen else BearRed,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "Each day is scored from that day's real sidereal graha positions; the " +
                "headline figures compound the daily estimates. Transparent rule engine " +
                "for learning — not a validated predictor.",
            style = MaterialTheme.typography.labelSmall,
            color = MutedText
        )
    }
}

@Composable
private fun HorizonHeadlineChip(label: String, percent: Double, direction: String, modifier: Modifier) {
    val color = directionColor(direction)
    Column(
        modifier
            .height(90.dp)
            .padding(2.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(label, color = MutedText, style = MaterialTheme.typography.labelMedium)
        Text(
            "${if (percent >= 0) "+" else ""}$percent%",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(direction, color = color, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun DayByDaySection(
    f7: ForecastEngine.HorizonForecast,
    f30: ForecastEngine.HorizonForecast
) {
    var horizon by remember { mutableIntStateOf(7) }
    HorizonToggle(selected = horizon, options = listOf(7, 30), onSelect = { horizon = it })
    Spacer(Modifier.height(10.dp))
    Text("Day-by-day prediction", color = NebulaViolet, style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(6.dp))

    val forecast = if (horizon == 7) f7 else f30
    forecast.days.forEach { d ->
        DayRow(
            dayIndex = d.dayIndex,
            dateLabel = shortDate(d.date.time),
            dailyPercent = d.dailyPercent,
            cumulativePercent = d.cumulativePercent,
            direction = d.direction,
            detail = "Moon in ${d.moonRashi} (${d.moonNakshatra})"
        )
    }
}
