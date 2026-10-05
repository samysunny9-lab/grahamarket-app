package com.grahamarket.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grahamarket.data.WatchItem

@Composable
fun WatchlistScreen(
    state: UiState,
    onRefresh: () -> Unit,
    onRemove: (String) -> Unit
) {
    // Refresh live prices whenever the screen is first shown.
    LaunchedEffect(Unit) { onRefresh() }

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Watchlist", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, color = StarGold)
                Text("Saved on this device — survives app restarts.",
                    style = MaterialTheme.typography.bodySmall, color = MutedText)
            }
            OutlinedButton(onClick = onRefresh, enabled = !state.watchRefreshing) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh prices")
                Text("  Refresh")
            }
        }

        Spacer(Modifier.height(12.dp))
        DisclaimerBanner()
        Spacer(Modifier.height(12.dp))

        if (state.watchRefreshing) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator(color = NebulaViolet)
            }
            Spacer(Modifier.height(12.dp))
        }

        if (state.watchItems.isEmpty()) {
            Spacer(Modifier.height(40.dp))
            Text(
                "No stocks yet. Analyse a stock on the Outlook tab and tap " +
                    "\"Add to Watchlist\".",
                color = MutedText,
                style = MaterialTheme.typography.bodyMedium
            )
            return@Column
        }

        // Prefer enriched rows (with live price); fall back to raw items.
        val rows = if (state.watchRows.size == state.watchItems.size) state.watchRows
        else state.watchItems.map { WatchRow(it, null, null) }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(rows, key = { it.item.symbol }) { row ->
                SectionCard(title = row.item.symbol) {
                    WatchRowBody(row, onRemove)
                }
            }
        }
    }
}

@Composable
private fun WatchRowBody(row: WatchRow, onRemove: (String) -> Unit) {
    val item: WatchItem = row.item
    Spacer(Modifier.height(6.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text("Added: ${shortDate(item.addedAtMillis)}",
                style = MaterialTheme.typography.bodySmall, color = MutedText)
            Text("Price when added: ${item.currency} ${"%,.2f".format(item.priceWhenAdded)}",
                style = MaterialTheme.typography.bodyMedium)
        }
        Column(horizontalAlignment = Alignment.End) {
            if (row.currentPrice != null) {
                Text("Now: ${item.currency} ${"%,.2f".format(row.currentPrice)}",
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                row.changePercent?.let { ch ->
                    Text(
                        "${if (ch >= 0) "+" else ""}${"%.2f".format(ch)}% since added",
                        color = if (ch >= 0) BullGreen else BearRed,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            } else if (row.currentError != null) {
                Text("Price unavailable", color = BearRed, style = MaterialTheme.typography.labelSmall)
            } else {
                Text("Tap Refresh for live price", color = MutedText, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Divider(color = CosmicSurfaceVariant)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        IconButton(onClick = { onRemove(item.symbol) }) {
            Icon(Icons.Filled.Delete, contentDescription = "Remove ${item.symbol}", tint = BearRed)
        }
    }
}
