package com.grahamarket.astro

import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A more authentic ("deeper") Vedic engine than the simple MarketScorer. It uses:
 *
 *   1. A per-symbol NATAL chart. Real listing dates are not bundled, so the chart's
 *      "birth" instant is derived deterministically from the symbol hash. This is a
 *      documented approximation — swap in a real listing date to make it authentic.
 *   2. Transit-to-natal analysis: how today's grahas sit relative to the natal
 *      Moon sign (the dignity of transiting benefics/malefics by house from natal
 *      Moon — a simplified Gochar / Chandra-lagna reading).
 *   3. Bhava (house) weighting: transiting grahas in the natal 2nd/11th (wealth,
 *      gains) houses lift; those in the 6th/8th/12th (loss, obstacles) drag.
 *   4. Vimshottari Dasha: the running Mahadasha/Antardasha lords add their
 *      benefic/malefic nature to the score.
 *
 * EDUCATIONAL ONLY. This is astrologically richer than MarketScorer but is NOT a
 * validated market predictor — see the honesty notes throughout the app.
 */
object DeeperJyotishEngine {

    data class Reading(
        val symbol: String,
        val natalMoonRashi: String,
        val natalMoonNakshatra: String,
        val mahadasha: Graha,
        val antardasha: Graha,
        val mahaWindow: String,
        val estimatedPercent: Double,
        val direction: String,
        val confidence: String,
        val factors: List<MarketScorer.Factor>,
        val date: Date
    )

    /** Benefic(+)/malefic(-) nature reused from classical weightings. */
    private val nature = mapOf(
        Graha.JUPITER to 1.4, Graha.VENUS to 1.1, Graha.MERCURY to 0.9,
        Graha.MOON to 0.6, Graha.SUN to 0.2,
        Graha.MARS to -0.9, Graha.SATURN to -1.4, Graha.RAHU to -1.1, Graha.KETU to -0.8
    )

    private fun symbolHash(symbol: String): Long {
        var h = 2166136261L
        for (c in symbol.uppercase()) { h = h xor c.code.toLong(); h = (h * 16777619L) and 0xFFFFFFFFL }
        return h
    }

    /**
     * Deterministic pseudo-"listing date" for a symbol: a day between 1990 and 2020
     * derived from the hash. Documented approximation (see class docs).
     */
    private fun natalDate(hash: Long): Date {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val year = 1990 + (hash % 31).toInt()           // 1990..2020
        val dayOfYear = 1 + ((hash shr 8) % 365).toInt() // 1..365
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.DAY_OF_YEAR, dayOfYear)
        cal.set(Calendar.HOUR_OF_DAY, 10)
        cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    fun reading(symbol: String, at: Date = Date()): Reading {
        val hash = symbolHash(symbol)
        val birth = natalDate(hash)

        // Natal chart: the natal Moon anchors Chandra-lagna and the dasha.
        val natal = GrahaEngine.allPositions(birth)
        val natalMoon = natal.first { it.graha == Graha.MOON }
        val natalMoonSign = natalMoon.rashiIndex

        // Transits now.
        val transit = GrahaEngine.allPositions(at)

        val factors = ArrayList<MarketScorer.Factor>()
        var score = 0.0

        // --- Bhava from natal Moon (Chandra-lagna) ---
        // House = sign distance of the transiting graha from the natal Moon sign + 1.
        val wealthHouses = setOf(2, 11)       // dhana, labha
        val gainHouses = setOf(1, 5, 9)        // supportive trikonas
        val lossHouses = setOf(6, 8, 12)       // dusthana
        for (p in transit) {
            val n = nature[p.graha] ?: 0.0
            val house = ((p.rashiIndex - natalMoonSign + 12) % 12) + 1
            var w = n
            when (house) {
                in wealthHouses -> w *= 1.6
                in gainHouses -> w *= 1.2
                in lossHouses -> w *= 1.5 // amplifies effect (malefics drag harder here)
                else -> w *= 0.8
            }
            // Dignity & retrograde modulation (consistent with MarketScorer craft).
            w *= (1.0 + 0.5 * p.dignity.strengthModifier)
            if (p.retrograde && p.graha != Graha.RAHU && p.graha != Graha.KETU) w *= 1.3
            if (p.combust) w *= 0.5

            if (abs(w) > 0.02) {
                factors.add(MarketScorer.Factor("${p.graha.displayName} transiting house $house from natal Moon", w))
                score += w
            }
        }

        // --- Vimshottari Dasha contribution ---
        val dasha = DashaEngine.periodAt(natalMoon.siderealLon, birth, at)
        val mahaN = (nature[dasha.maha] ?: 0.0) * 2.0   // Mahadasha dominates
        val antarN = (nature[dasha.antar] ?: 0.0) * 1.0
        factors.add(MarketScorer.Factor("Mahadasha of ${dasha.maha.displayName}", mahaN))
        factors.add(MarketScorer.Factor("Antardasha of ${dasha.antar.displayName}", antarN))
        score += mahaN + antarN

        val pct = (score * 0.9).coerceIn(-18.0, 18.0)
        val direction = when {
            score > 0.8 -> "Bullish"
            score < -0.8 -> "Bearish"
            else -> "Neutral"
        }

        val mahaWindow = "${yearOf(dasha.mahaStart)}–${yearOf(dasha.mahaEnd)}"

        return Reading(
            symbol = symbol.uppercase(),
            natalMoonRashi = natalMoon.rashi.substringBefore(" ("),
            natalMoonNakshatra = natalMoon.nakshatra,
            mahadasha = dasha.maha,
            antardasha = dasha.antar,
            mahaWindow = mahaWindow,
            estimatedPercent = (pct * 10).roundToInt() / 10.0,
            direction = direction,
            confidence = "Low (educational)",
            factors = factors.sortedByDescending { abs(it.contribution) },
            date = at
        )
    }

    private fun yearOf(d: Date): Int {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")); cal.time = d
        return cal.get(Calendar.YEAR)
    }

    // --- Day-by-day horizon for the deeper engine (mirrors ForecastEngine) ---

    data class DeeperDay(
        val date: Date, val dayIndex: Int, val dailyPercent: Double,
        val cumulativePercent: Double, val direction: String,
        val maha: Graha, val antar: Graha
    )
    data class DeeperHorizon(
        val symbol: String, val horizonDays: Int, val headlinePercent: Double,
        val direction: String, val confidence: String, val days: List<DeeperDay>
    )

    private fun startOfDayUtc(base: Date, addDays: Int): Date {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.time = base; cal.add(Calendar.DAY_OF_YEAR, addDays)
        cal.set(Calendar.HOUR_OF_DAY, 12); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    fun horizon(symbol: String, horizonDays: Int, start: Date = Date()): DeeperHorizon {
        val days = ArrayList<DeeperDay>(horizonDays)
        var cumulativeFactor = 1.0
        for (i in 1..horizonDays) {
            val d = startOfDayUtc(start, i - 1)
            val r = reading(symbol, d)
            val daily = (r.estimatedPercent / 30.0 * 100).roundToInt() / 100.0
            cumulativeFactor *= (1.0 + daily / 100.0)
            val cum = (cumulativeFactor - 1.0) * 100.0
            days.add(
                DeeperDay(d, i, daily, (cum * 100).roundToInt() / 100.0,
                    when { daily > 0.15 -> "Bullish"; daily < -0.15 -> "Bearish"; else -> "Neutral" },
                    r.mahadasha, r.antardasha)
            )
        }
        val headline = (cumulativeFactor - 1.0) * 100.0
        return DeeperHorizon(
            symbol.uppercase(), horizonDays, (headline * 10).roundToInt() / 10.0,
            when { headline > 0.15 -> "Bullish"; headline < -0.15 -> "Bearish"; else -> "Neutral" },
            "Low (educational)", days
        )
    }
}
