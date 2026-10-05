package com.grahamarket.astro

import java.util.Date
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Maps graha states to a bullish/bearish score and a 30-day percentage estimate
 * for a given symbol, following classical financial-astrology heuristics.
 *
 * IMPORTANT: this is a transparent, deterministic rule engine for EDUCATIONAL use.
 * There is no proven causal link between planetary positions and markets, so the
 * output always carries a LOW confidence label and the reasoning is fully shown.
 */
object MarketScorer {

    data class Factor(val label: String, val contribution: Double)

    data class Outlook(
        val symbol: String,
        val score: Double,           // net bullish(+)/bearish(-) score
        val direction: String,       // "Bullish" / "Bearish" / "Neutral"
        val estimatedPercent: Double, // 30-day % estimate
        val confidence: String,       // always "Low (educational)"
        val factors: List<Factor>,
        val computedAt: Date
    )

    /**
     * Classical "financial" weights: benefic grahas lift, malefics drag.
     * Deliberately balanced so benefic (+4.2) and malefic (-4.2) totals cancel —
     * the net direction then comes from current dignities, retrogrades, combustion
     * and the stock's lord, not from a baked-in bullish or bearish bias.
     */
    private val baseWeight = mapOf(
        Graha.JUPITER to 1.4,   // wealth, expansion
        Graha.VENUS to 1.1,     // luxury, liquidity
        Graha.MERCURY to 0.9,   // trade, commerce
        Graha.MOON to 0.6,      // sentiment, public mood
        Graha.SUN to 0.2,       // governance
        Graha.MARS to -0.9,     // volatility, aggression
        Graha.SATURN to -1.4,   // contraction, fear
        Graha.RAHU to -1.1,     // speculation, bubbles
        Graha.KETU to -0.8      // sudden drops
    )

    /**
     * Deterministic per-symbol hash (FNV-1a, 32-bit) — stable across runs, well
     * distributed so distinct symbols rarely collide. No randomness.
     */
    private fun symbolHash(symbol: String): Long {
        var h = 2166136261L
        for (c in symbol.uppercase()) {
            h = h xor c.code.toLong()
            h = (h * 16777619L) and 0xFFFFFFFFL
        }
        return h
    }

    /**
     * Each symbol gets a stable "natal lord" — the graha whose influence is
     * amplified for that stock, derived from its hash. This differentiates stocks
     * and also lets a malefic-lorded stock turn bearish even in a benefic sky.
     */
    private fun lordFor(hash: Long): Graha {
        val grahas = Graha.entries
        return grahas[(hash % grahas.size).toInt()]
    }

    fun outlook(symbol: String, date: Date = Date()): Outlook {
        val positions = GrahaEngine.allPositions(date)
        val hash = symbolHash(symbol)
        val lord = lordFor(hash)
        // Baseline "market mood" offset per symbol, widened to [-3.0, +3.0] so an
        // individual stock can diverge from the overall benefic/malefic sky.
        val moodBias = (((hash shr 8) % 6000).toDouble() / 1000.0) - 3.0

        val factors = mutableListOf<Factor>()
        var score = moodBias

        for (p in positions) {
            var w = baseWeight[p.graha] ?: 0.0

            // Dignity modulates strength: exalted strengthens the graha's nature,
            // debilitated weakens/reverses it slightly.
            w *= (1.0 + 0.5 * p.dignity.strengthModifier)

            // Retrograde (vakri) intensifies a graha's expression without changing
            // its benefic/malefic nature — a retrograde malefic drags harder.
            if (p.retrograde && p.graha != Graha.RAHU && p.graha != Graha.KETU) w *= 1.3

            // Combust grahas lose expressive strength.
            if (p.combust) w *= 0.5

            // The stock's natal lord dominates its chart (strong amplification),
            // so a malefic-lorded stock can turn bearish even in a benefic sky.
            if (p.graha == lord) w *= 3.5

            if (abs(w) > 0.01) {
                val detail = buildString {
                    append(p.graha.displayName)
                    append(" in ").append(p.rashi.substringBefore(" ("))
                    if (p.dignity != Dignity.NEUTRAL) append(", ").append(p.dignity.label)
                    if (p.retrograde) append(", retrograde")
                    if (p.combust) append(", combust")
                    if (p.graha == lord) append(" [stock's lord]")
                }
                factors.add(Factor(detail, w))
                score += w
            }
        }

        // Per-symbol magnitude modulation (stable): scales by 0.6 .. 1.4.
        val magMod = 0.6 + ((hash shr 20) % 800).toDouble() / 1000.0
        score *= magMod

        // Convert score to a bounded 30-day % estimate.
        val pct = (score * 1.8).coerceIn(-18.0, 18.0)

        val direction = when {
            score > 0.6 -> "Bullish"
            score < -0.6 -> "Bearish"
            else -> "Neutral"
        }

        return Outlook(
            symbol = symbol.uppercase(),
            score = score,
            direction = direction,
            estimatedPercent = (pct * 10).roundToInt() / 10.0,
            confidence = "Low (educational)",
            factors = factors.sortedByDescending { abs(it.contribution) },
            computedAt = date
        )
    }
}
