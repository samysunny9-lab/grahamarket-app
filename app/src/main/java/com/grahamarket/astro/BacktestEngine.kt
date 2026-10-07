package com.grahamarket.astro

import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * The "Reality Check". It evaluates how well the astrology engines' past daily
 * predictions line up with an INDEPENDENT reference series, computing honest
 * statistics: directional hit-rate and Pearson correlation.
 *
 * The whole point of this screen is HONESTY: because planetary positions have no
 * causal link to prices, the hit-rate lands near 50% (coin-flip) and the
 * correlation near 0. Seeing that is the educational payoff.
 *
 * NOTE ON DATA: this build has no bundled historical price feed, so the reference
 * series is a deterministic pseudo-random-walk per symbol (clearly labelled as a
 * synthetic baseline in the UI). The statistics are computed for real. The series
 * is produced behind [ReferenceSeries] so a real historical-price source can be
 * dropped in as a one-function replacement.
 */
object BacktestEngine {

    enum class Source { GRAHA, NUMEROLOGY, DEEPER }

    data class DayComparison(
        val date: Date,
        val predictedPercent: Double,
        val actualPercent: Double,
        val predictedDir: Int,   // sign: +1 / 0 / -1
        val actualDir: Int,
        val hit: Boolean
    )

    data class Result(
        val symbol: String,
        val source: Source,
        val days: Int,
        val hitRatePercent: Double,     // % of days direction matched
        val correlation: Double,        // Pearson r between predicted and actual %
        val meanAbsError: Double,       // avg |predicted - actual| in % points
        val syntheticReference: Boolean,
        val verdict: String,
        val comparisons: List<DayComparison>
    )

    /**
     * Independent reference daily returns. Deterministic per symbol so results are
     * reproducible, but generated from a hash chain that is INDEPENDENT of the
     * astrology inputs — mimicking a real, unrelated price series.
     */
    private object ReferenceSeries {
        fun dailyReturn(symbol: String, dayEpochDay: Long): Double {
            // xorshift-style deterministic pseudo-random in [-2%, +2%].
            var x = (symbol.hashCode().toLong() * 2862933555777941757L) xor (dayEpochDay * 3037000493L)
            x = x xor (x shl 13); x = x xor (x ushr 7); x = x xor (x shl 17)
            val unit = ((x ushr 11).toDouble() / (1L shl 53).toDouble()) // 0..1
            return (unit * 4.0) - 2.0
        }
    }

    private fun predictedFor(source: Source, symbol: String, date: Date): Double = when (source) {
        Source.GRAHA -> MarketScorer.outlook(symbol, date).estimatedPercent / 30.0
        Source.NUMEROLOGY -> NumerologyEngine.reading(symbol, date).estimatedPercent / 30.0
        Source.DEEPER -> DeeperJyotishEngine.reading(symbol, date).estimatedPercent / 30.0
    }

    private fun sign(x: Double): Int = when {
        x > 0.02 -> 1
        x < -0.02 -> -1
        else -> 0
    }

    private fun dayStart(base: Date, offset: Int): Date {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.time = base; cal.add(Calendar.DAY_OF_YEAR, offset)
        cal.set(Calendar.HOUR_OF_DAY, 12); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    /** Backtest over the [days] days ending yesterday. */
    fun run(symbol: String, source: Source, days: Int = 60, now: Date = Date()): Result {
        val comparisons = ArrayList<DayComparison>(days)
        val preds = ArrayList<Double>(days)
        val acts = ArrayList<Double>(days)
        var hits = 0
        var absErrSum = 0.0

        for (i in days downTo 1) {
            val d = dayStart(now, -i)
            val predicted = predictedFor(source, symbol, d)
            val epochDay = d.time / 86_400_000L
            val actual = ReferenceSeries.dailyReturn(symbol, epochDay)

            val pd = sign(predicted); val ad = sign(actual)
            val hit = pd != 0 && pd == ad
            if (hit) hits++
            absErrSum += kotlin.math.abs(predicted - actual)

            preds.add(predicted); acts.add(actual)
            comparisons.add(DayComparison(d, round2(predicted), round2(actual), pd, ad, hit))
        }

        val hitRate = hits.toDouble() / days * 100.0
        val corr = pearson(preds, acts)
        val mae = absErrSum / days

        val verdict = buildString {
            append("Over $days days, this engine's direction matched the reference ")
            append("${hitRate.roundToInt()}% of the time ")
            append("(a coin-flip is ~50%). ")
            append("Correlation with actual moves: ${round2(corr)} ")
            append("(0 = no relationship). ")
            append("This is the expected result: planetary / numerology signals have ")
            append("no causal link to prices, so they cannot beat chance.")
        }

        return Result(
            symbol = symbol.uppercase(),
            source = source,
            days = days,
            hitRatePercent = (hitRate * 10).roundToInt() / 10.0,
            correlation = round2(corr),
            meanAbsError = round2(mae),
            syntheticReference = true,
            verdict = verdict,
            comparisons = comparisons
        )
    }

    private fun pearson(a: List<Double>, b: List<Double>): Double {
        val n = a.size
        if (n == 0) return 0.0
        val ma = a.average(); val mb = b.average()
        var num = 0.0; var da = 0.0; var db = 0.0
        for (i in 0 until n) {
            val xa = a[i] - ma; val xb = b[i] - mb
            num += xa * xb; da += xa * xa; db += xb * xb
        }
        val den = sqrt(da * db)
        return if (den == 0.0) 0.0 else num / den
    }

    private fun round2(x: Double): Double = (x * 100).roundToInt() / 100.0
}
