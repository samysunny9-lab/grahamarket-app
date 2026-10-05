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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grahamarket.astro.GrahaPosition

@Composable
fun GrahaScreen(grahas: List<GrahaPosition>) {
    LazyColumn(
        Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Current Sky (Grahas)",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = StarGold
            )
            Text(
                "Sidereal positions (Lahiri ayanamsa), computed on-device.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedText
            )
            Spacer(Modifier.height(4.dp))
        }
        items(grahas) { p -> GrahaRow(p) }
    }
}

@Composable
private fun GrahaRow(p: GrahaPosition) {
    SectionCard(title = "${p.graha.displayName} (${p.graha.sanskrit})") {
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Rashi: ${p.rashi}", color = MaterialTheme.colorScheme.onSurface)
                Text("Nakshatra: ${p.nakshatra}", color = MutedText, style = MaterialTheme.typography.bodySmall)
                Text(
                    "Longitude: ${"%.2f".format(p.siderealLon)}° (${"%.2f".format(p.degreesInRashi)}° in sign)",
                    color = MutedText, style = MaterialTheme.typography.bodySmall
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                val nature = if (p.graha.benefic) "Benefic" else "Malefic/Neutral"
                Text(nature, color = if (p.graha.benefic) BullGreen else BearRed, style = MaterialTheme.typography.labelMedium)
                if (p.dignity.label != "Neutral") Text(p.dignity.label, color = StarGold, style = MaterialTheme.typography.labelSmall)
                if (p.retrograde) Text("Retrograde (Vakri)", color = NebulaViolet, style = MaterialTheme.typography.labelSmall)
                if (p.combust) Text("Combust (Astam)", color = BearRed, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
