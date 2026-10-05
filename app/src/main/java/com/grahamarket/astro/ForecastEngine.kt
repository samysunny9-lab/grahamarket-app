package com.grahamarket.astro

import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlin.math.roundToInt

/**
 * Builds multi-day forecasts from the per-day [MarketScorer] outputs.
 *
 * Each day is scored independently using that day's REAL sidereal graha positions,
 * so a 30-day series reflects how the sky genuinely changes day to day (the Moon
 * moves ~13 deg/day, so daily values vary meaningfully; slow grahas drift slowly).
 *
 * A horizon's "cumulative" percent compounds the daily estimates, giving a single
 * 7-day or 30-day headline figure that is consistent with the day-by-day detail.
 *
 * EDUCATIONAL ONLY — see MarketScorer for the honesty caveats.
 */
object ForecastEngine {

    /** One day's prediction for a symbol. */
    data class DailyPrediction(
        val date: Date,
        val dayIndex: Int,            // 1-based day offset from the start date
        val dailyPercent: Double,     // this single day's estimated move
        val cumulativePercent: Double, // compounded move from day 1 through this day
        val direction: String,
        val moonRashi: String,        // the day's Moon sign (fastest-moving driver)
        val moonNakshatra: String
    )

    /** A full horizon (e.g. 7 or 30 days) with headline + daily detail. */
    data class HorizonForecast(
        val symbol: String,
        val horizonDays: Int,
        val headlinePercent: Double,  // compounded over the whole horizon
        val direction: String,
        val confidence: String,
        val days: List<DailyPrediction>
    )

    private fun startOfDayUtc(base: Date, addDays: Int): Date {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.time = base
        cal.add(Calendar.DAY_OF_YEAR, addDays)
        cal.set(Calendar.HOUR_OF_DAY, 12) // midday sample for stability
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    /**
     * A single day's standalone move is a dampened fraction of the full outlook
     * score for that day — a monthly-scale score shouldn't be realised in one day.
     * We scale so that ~30 compounded days roughly reproduce the 30-day outlook.
     */
    private fun dailyPercentFor(symbol: String, date: Date): Double {
        val o = MarketScorer.outlook(symbol, date)
        // Full outlook is a ~30-day figure; approximate the per-day move.
        val daily = o.estimatedPercent / 30.0
        return (daily * 100).roundToInt() / 100.0
    }

    fun horizon(symbol: String, horizonDays: Int, start: Date = Date()): HorizonForecast {
        val days = ArrayList<DailyPrediction>(horizonDays)
        var cumulativeFactor = 1.0

        for (i in 1..horizonDays) {
            val d = startOfDayUtc(start, i - 1)
            val daily = dailyPercentFor(symbol, d)
            cumulativeFactor *= (1.0 + daily / 100.0)
            val cumulative = (cumulativeFactor - 1.0) * 100.0

            val moon = GrahaEngine.allPositions(d).first { it.graha == Graha.MOON }

            days.add(
                DailyPrediction(
                    date = d,
                    dayIndex = i,
                    dailyPercent = daily,
                    cumulativePercent = (cumulative * 100).roundToInt() / 100.0,
                    direction = dirOf(daily),
                    moonRashi = moon.rashi.substringBefore(" ("),
                    moonNakshatra = moon.nakshatra
                )
            )
        }

        val headline = (cumulativeFactor - 1.0) * 100.0
        return HorizonForecast(
            symbol = symbol.uppercase(),
            horizonDays = horizonDays,
            headlinePercent = (headline * 10).roundToInt() / 10.0,
            direction = dirOf(headline),
            confidence = "Low (educational)",
            days = days
        )
    }

    private fun dirOf(pct: Double): String = when {
        pct > 0.15 -> "Bullish"
        pct < -0.15 -> "Bearish"
        else -> "Neutral"
    }
}
