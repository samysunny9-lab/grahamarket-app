package com.grahamarket.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** The persistent educational-disclaimer banner shown across the app. */
@Composable
fun DisclaimerBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(StarGold.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Filled.Info, contentDescription = null, tint = StarGold)
        Text(
            "Educational only — not investment advice. Planetary positions have no " +
                "scientifically proven effect on markets. Figures are illustrative.",
            style = MaterialTheme.typography.bodySmall,
            color = StarGold
        )
    }
}

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = NebulaViolet
            )
            content()
        }
    }
}

fun directionColor(direction: String): Color = when (direction) {
    "Bullish" -> BullGreen
    "Bearish" -> BearRed
    else -> MutedText
}

// ---- Forecast UI shared components ----

/** A simple two-option segmented toggle (e.g. 7 days / 30 days). */
@Composable
fun HorizonToggle(
    selected: Int,
    options: List<Int>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CosmicSurfaceVariant, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { opt ->
            val isSel = opt == selected
            Text(
                text = "$opt days",
                color = if (isSel) CosmicBg else MutedText,
                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (isSel) StarGold else Color.Transparent,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelect(opt) }
                    .padding(vertical = 10.dp)
            )
        }
    }
}

/** A headline percentage block (big number + direction + confidence). */
@Composable
fun HeadlineOutlook(
    percent: Double,
    direction: String,
    confidence: String,
    subtitle: String? = null
) {
    val color = directionColor(direction)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "${if (percent >= 0) "+" else ""}$percent%",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        androidx.compose.foundation.layout.Spacer(Modifier.padding(start = 14.dp))
        Column {
            Text(direction, color = color, fontWeight = FontWeight.SemiBold)
            Text("Confidence: $confidence", color = MutedText, style = MaterialTheme.typography.labelSmall)
            if (subtitle != null) Text(subtitle, color = MutedText, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** One day row in a day-by-day forecast table. */
@Composable
fun DayRow(
    dayIndex: Int,
    dateLabel: String,
    dailyPercent: Double,
    cumulativePercent: Double,
    direction: String,
    detail: String?
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "D$dayIndex",
            color = NebulaViolet,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(end = 8.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(dateLabel, style = MaterialTheme.typography.bodySmall)
            if (detail != null) Text(detail, color = MutedText, style = MaterialTheme.typography.labelSmall)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${if (dailyPercent >= 0) "+" else ""}$dailyPercent%",
                color = directionColor(direction),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                "cum ${if (cumulativePercent >= 0) "+" else ""}$cumulativePercent%",
                color = MutedText,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/** A date label helper used by forecast screens. */
fun shortDate(millis: Long): String {
    val fmt = java.text.SimpleDateFormat("EEE, d MMM", java.util.Locale.getDefault())
    return fmt.format(java.util.Date(millis))
}
