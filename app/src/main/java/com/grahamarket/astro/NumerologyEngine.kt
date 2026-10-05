package com.grahamarket.astro

import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlin.math.roundToInt

/**
 * A numerology-based predictor that mirrors the structure of the graha forecast,
 * so the UI can offer the "same prediction via numerology" the user asked for.
 *
 * Method (transparent + deterministic):
 *  - Reduce the stock symbol to a Chaldean "name number" (1..9).
 *  - Reduce the target calendar date to a "day number" (1..9).
 *  - Combine them into a compound vibration; map that to a bullish/bearish score
 *    using classic numerological planet rulerships of each root number
 *    (1=Sun, 2=Moon, 3=Jupiter, 4=Rahu, 5=Mercury, 6=Venus, 7=Ketu, 8=Saturn, 9=Mars).
 *
 * EDUCATIONAL ONLY. Numerology has no proven predictive validity for markets.
 */
object NumerologyEngine {

    /** Chaldean letter values (note: Chaldean has no 9 for letters). */
    private val chaldean = mapOf(
        'A' to 1, 'I' to 1, 'J' to 1, 'Q' to 1, 'Y' to 1,
        'B' to 2, 'K' to 2, 'R' to 2,
        'C' to 3, 'G' to 3, 'L' to 3, 'S' to 3,
        'D' to 4, 'M' to 4, 'T' to 4,
        'E' to 5, 'H' to 5, 'N' to 5, 'X' to 5,
        'U' to 6, 'V' to 6, 'W' to 6,
        'O' to 7, 'Z' to 7,
        'F' to 8, 'P' to 8
    )

    /** Numerological "vibration" of each root number: benefic(+) / malefic(-). */
    private val numberVibration = mapOf(
        1 to 1.0,    // Sun — leadership, strength
        2 to 0.4,    // Moon — fluctuation, mild
        3 to 1.2,    // Jupiter — expansion, luck
        4 to -1.0,   // Rahu — instability, sudden change
        5 to 0.8,    // Mercury — commerce, agility
        6 to 1.0,    // Venus — wealth, harmony
        7 to -0.6,   // Ketu — detachment, uncertainty
        8 to -1.2,   // Saturn — restriction, delay
        9 to -0.4    // Mars — volatility, energy
    )

    private val rulingPlanet = mapOf(
        1 to "Sun", 2 to "Moon", 3 to "Jupiter", 4 to "Rahu", 5 to "Mercury",
        6 to "Venus", 7 to "Ketu", 8 to "Saturn", 9 to "Mars"
    )

    /** Reduce an integer to a single digit 1..9 (keeping 0 -> 9). */
    private fun reduceToDigit(n: Int): Int {
        var x = kotlin.math.abs(n)
        while (x > 9) {
            var s = 0
            while (x > 0) { s += x % 10; x /= 10 }
            x = s
        }
        return if (x == 0) 9 else x
    }

    fun nameNumber(symbol: String): Int {
        val sum = symbol.uppercase().sumOf { chaldean[it] ?: 0 }
        return reduceToDigit(sum)
    }

    fun dateNumber(date: Date): Int {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.time = date
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        return reduceToDigit(reduceToDigit(y) + m + d)
    }

    data class NumerologyReading(
        val symbol: String,
        val nameNumber: Int,
        val nameRuler: String,
        val dateNumber: Int,
        val dateRuler: String,
        val compoundNumber: Int,
        val estimatedPercent: Double,
        val direction: String,
        val confidence: String,
        val explanation: String,
        val date: Date
    )

    fun reading(symbol: String, date: Date = Date()): NumerologyReading {
        val name = nameNumber(symbol)
        val day = dateNumber(date)
        val compound = reduceToDigit(name + day)

        // Score blends name (the stock's intrinsic vibration) with the day's number
        // and their compound. Harmony (name == day or compatible planets) lifts.
        val nameV = numberVibration[name] ?: 0.0
        val dayV = numberVibration[day] ?: 0.0
        val compoundV = numberVibration[compound] ?: 0.0
        var score = nameV * 1.2 + dayV * 0.8 + compoundV * 1.0

        // "Friendly number" bonus: 1-3-9 and 2-4-7 and 5-6-8 are classic groupings.
        val friendly = setOf(setOf(1, 3, 9), setOf(2, 4, 7), setOf(5, 6, 8))
        if (friendly.any { it.contains(name) && it.contains(day) }) score += 0.6

        val pct = (score * 2.0).coerceIn(-18.0, 18.0)
        val direction = when {
            score > 0.5 -> "Bullish"
            score < -0.5 -> "Bearish"
            else -> "Neutral"
        }

        val explanation = buildString {
            append("Name number $name (${rulingPlanet[name]}), ")
            append("date number $day (${rulingPlanet[day]}), ")
            append("compound $compound (${rulingPlanet[compound]}). ")
            append("These vibrations combine to a $direction reading for this date.")
        }

        return NumerologyReading(
            symbol = symbol.uppercase(),
            nameNumber = name,
            nameRuler = rulingPlanet[name] ?: "",
            dateNumber = day,
            dateRuler = rulingPlanet[day] ?: "",
            compoundNumber = compound,
            estimatedPercent = (pct * 10).roundToInt() / 10.0,
            direction = direction,
            confidence = "Low (educational)",
            explanation = explanation,
            date = date
        )
    }

    /** Day-by-day numerology series (mirrors ForecastEngine.horizon). */
    data class NumerologyDay(
        val date: Date,
        val dayIndex: Int,
        val dayNumber: Int,
        val dailyPercent: Double,
        val cumulativePercent: Double,
        val direction: String
    )

    data class NumerologyHorizon(
        val symbol: String,
        val horizonDays: Int,
        val headlinePercent: Double,
        val direction: String,
        val confidence: String,
        val days: List<NumerologyDay>
    )

    private fun startOfDayUtc(base: Date, addDays: Int): Date {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.time = base
        cal.add(Calendar.DAY_OF_YEAR, addDays)
        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    fun horizon(symbol: String, horizonDays: Int, start: Date = Date()): NumerologyHorizon {
        val days = ArrayList<NumerologyDay>(horizonDays)
        var cumulativeFactor = 1.0
        for (i in 1..horizonDays) {
            val d = startOfDayUtc(start, i - 1)
            val r = reading(symbol, d)
            val daily = (r.estimatedPercent / 30.0 * 100).roundToInt() / 100.0
            cumulativeFactor *= (1.0 + daily / 100.0)
            val cumulative = (cumulativeFactor - 1.0) * 100.0
            days.add(
                NumerologyDay(
                    date = d, dayIndex = i, dayNumber = dateNumber(d),
                    dailyPercent = daily,
                    cumulativePercent = (cumulative * 100).roundToInt() / 100.0,
                    direction = when { daily > 0.15 -> "Bullish"; daily < -0.15 -> "Bearish"; else -> "Neutral" }
                )
            )
        }
        val headline = (cumulativeFactor - 1.0) * 100.0
        return NumerologyHorizon(
            symbol = symbol.uppercase(),
            horizonDays = horizonDays,
            headlinePercent = (headline * 10).roundToInt() / 10.0,
            direction = when { headline > 0.15 -> "Bullish"; headline < -0.15 -> "Bearish"; else -> "Neutral" },
            confidence = "Low (educational)",
            days = days
        )
    }
}
