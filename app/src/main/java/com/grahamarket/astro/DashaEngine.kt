package com.grahamarket.astro

import java.util.Date
import kotlin.math.floor

/**
 * Vimshottari Dasha — the classical 120-year planetary-period timing system of
 * Vedic astrology. The sequence of ruling grahas and their year-lengths, and the
 * mapping from the Moon's nakshatra to the starting dasha lord, follow Brihat
 * Parashara Hora Shastra as summarised by standard references.
 *
 *   Order & years: Ketu 7, Venus 20, Sun 6, Moon 10, Mars 7, Rahu 18,
 *                  Jupiter 16, Saturn 19, Mercury 17  (total 120)
 *   The 27 nakshatras cycle through these 9 lords three times, so nakshatra i
 *   (0-based) opens the dasha of lord[i % 9].
 *
 * EDUCATIONAL ONLY — used here to characterise the "period" a stock's inferred
 * chart is running through; it has no proven market-predictive validity.
 */
object DashaEngine {

    /** The fixed Vimshottari order with each lord's full period in years. */
    val sequence: List<Pair<Graha, Double>> = listOf(
        Graha.KETU to 7.0,
        Graha.VENUS to 20.0,
        Graha.SUN to 6.0,
        Graha.MOON to 10.0,
        Graha.MARS to 7.0,
        Graha.RAHU to 18.0,
        Graha.JUPITER to 16.0,
        Graha.SATURN to 19.0,
        Graha.MERCURY to 17.0
    )

    private const val TOTAL_YEARS = 120.0
    private const val NAK_SPAN = 360.0 / 27.0 // 13.3333 deg
    private const val DAYS_PER_YEAR = 365.25

    /** Lord that opens the dasha for a given nakshatra index (0..26). */
    fun lordForNakshatra(nakshatraIndex: Int): Graha =
        sequence[nakshatraIndex % 9].first

    private fun years(g: Graha): Double = sequence.first { it.first == g }.second

    data class DashaPeriod(
        val maha: Graha,          // Mahadasha lord
        val antar: Graha,         // Antardasha (sub-period) lord
        val mahaStart: Date,
        val mahaEnd: Date,
        val antarStart: Date,
        val antarEnd: Date
    )

    /**
     * Computes the running Mahadasha + Antardasha at [at], given the Moon's
     * sidereal longitude of the chart's "birth" moment.
     *
     * We derive the balance of the first dasha from how far the Moon has
     * progressed through its nakshatra at birth, exactly as classical practice.
     */
    fun periodAt(moonBirthLon: Double, birth: Date, at: Date): DashaPeriod {
        val nakIndex = floor(moonBirthLon / NAK_SPAN).toInt() % 27
        val intoNak = (moonBirthLon % NAK_SPAN) / NAK_SPAN // fraction 0..1 through nakshatra

        // Starting lord and the elapsed fraction of its period already consumed.
        val startLordIndex = nakIndex % 9
        val startLord = sequence[startLordIndex].first
        val startLordYears = years(startLord)
        val consumedYears = intoNak * startLordYears

        // Build a timeline of Mahadashas from (birth - consumedYears) forward.
        val timelineStartMillis = birth.time - (consumedYears * DAYS_PER_YEAR * 86400_000L).toLong()

        var cursor = timelineStartMillis
        var idx = startLordIndex
        // Advance through Mahadashas until we contain `at`.
        var mahaLord = sequence[idx].first
        var mahaStart = cursor
        var mahaEnd = cursor + (years(mahaLord) * DAYS_PER_YEAR * 86400_000L).toLong()
        var guard = 0
        while (at.time >= mahaEnd && guard < 100) {
            cursor = mahaEnd
            idx = (idx + 1) % 9
            mahaLord = sequence[idx].first
            mahaStart = cursor
            mahaEnd = cursor + (years(mahaLord) * DAYS_PER_YEAR * 86400_000L).toLong()
            guard++
        }

        // Within the Mahadasha, antardashas run in the same sequence starting from
        // the maha lord itself, each proportional to lordYears * mahaYears / 120.
        val mahaYears = years(mahaLord)
        var aCursor = mahaStart
        var aIdx = idx
        var antarLord = sequence[aIdx].first
        var antarStart = aCursor
        var antarEnd = aCursor + antarMillis(antarLord, mahaYears)
        guard = 0
        while (at.time >= antarEnd && guard < 20) {
            aCursor = antarEnd
            aIdx = (aIdx + 1) % 9
            antarLord = sequence[aIdx].first
            antarStart = aCursor
            antarEnd = aCursor + antarMillis(antarLord, mahaYears)
            guard++
        }

        return DashaPeriod(
            maha = mahaLord,
            antar = antarLord,
            mahaStart = Date(mahaStart),
            mahaEnd = Date(mahaEnd),
            antarStart = Date(antarStart),
            antarEnd = Date(antarEnd)
        )
    }

    private fun antarMillis(antarLord: Graha, mahaYears: Double): Long {
        val antarYears = years(antarLord) * mahaYears / TOTAL_YEARS
        return (antarYears * DAYS_PER_YEAR * 86400_000L).toLong()
    }
}
