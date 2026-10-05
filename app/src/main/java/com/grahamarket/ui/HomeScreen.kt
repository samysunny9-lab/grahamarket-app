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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grahamarket.data.SymbolReport
import kotlin.math.abs

@Composable
fun HomeScreen(
    state: UiState,
    onQueryChange: (String) -> Unit,
    onAnalyze: () -> Unit
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
            "Planetary (graha) outlook for Indian stocks · 30-day view",
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
            Spacer(Modifier.height(0.dp))
            Text("  Analyze")
        }

        if (state.loading) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) { CircularProgressIndicator(color = NebulaViolet) }
        }

        state.report?.let { ReportView(it) }

        Text(
            "NSE symbols are used by default (RELIANCE → RELIANCE.NS). " +
                "Add .BO for BSE. Change the data provider in Settings.",
            style = MaterialTheme.typography.labelSmall,
            color = MutedText
        )
    }
}

@Composable
private fun ReportView(report: SymbolReport) {
    val o = report.outlook
    val color = directionColor(o.direction)

    SectionCard(title = o.symbol) {
        Spacer(Modifier.height(8.dp))
        // Live price
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
        } else {
            Text(
                report.quoteError ?: "Price unavailable.",
                color = BearRed,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "The graha outlook below is still computed. Try another symbol or switch provider in Settings.",
                color = MutedText, style = MaterialTheme.typography.labelSmall
            )
        }

        Spacer(Modifier.height(14.dp))
        Divider(color = CosmicSurfaceVariant)
        Spacer(Modifier.height(14.dp))

        // 30-day outlook
        Text("30-day graha outlook", color = NebulaViolet, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${if (o.estimatedPercent >= 0) "+" else ""}${o.estimatedPercent}%",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(o.direction, color = color, fontWeight = FontWeight.SemiBold)
                Text("Confidence: ${o.confidence}", color = MutedText, style = MaterialTheme.typography.labelSmall)
            }
        }

        report.projectedPrice?.let {
            Spacer(Modifier.height(6.dp))
            Text(
                "Illustrative projected price: ${report.quote?.currency ?: ""} ${"%,.2f".format(it)}",
                color = MutedText,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(14.dp))
        Text("Contributing graha factors", color = NebulaViolet, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        o.factors.take(8).forEach { f ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
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
            "How this is computed: real sidereal (Lahiri) positions of all nine grahas " +
                "are weighted by classical benefic/malefic nature, dignity, retrograde and " +
                "combustion, then scaled to a monthly estimate. This is a transparent rule " +
                "engine for learning — not a validated predictor.",
            style = MaterialTheme.typography.labelSmall,
            color = MutedText,
            textAlign = TextAlign.Start
        )
    }
}
