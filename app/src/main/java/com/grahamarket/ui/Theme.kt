package com.grahamarket.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Cosmic palette
val CosmicBg = Color(0xFF0B1026)
val CosmicSurface = Color(0xFF151B3B)
val CosmicSurfaceVariant = Color(0xFF1E2650)
val StarGold = Color(0xFFFFD36E)
val NebulaViolet = Color(0xFF9B8CFF)
val BullGreen = Color(0xFF4CD9A4)
val BearRed = Color(0xFFFF6B6B)
val MutedText = Color(0xFFB7BEE0)

private val CosmicColors = darkColorScheme(
    primary = NebulaViolet,
    onPrimary = Color(0xFF0B1026),
    secondary = StarGold,
    background = CosmicBg,
    onBackground = Color(0xFFEDEFFF),
    surface = CosmicSurface,
    onSurface = Color(0xFFEDEFFF),
    surfaceVariant = CosmicSurfaceVariant,
    onSurfaceVariant = MutedText
)

@Composable
fun GrahaMarketTheme(content: @Composable () -> Unit) {
    // Always use the cosmic dark scheme for a consistent look.
    @Suppress("UNUSED_EXPRESSION") isSystemInDarkTheme()
    MaterialTheme(colorScheme = CosmicColors, content = content)
}
