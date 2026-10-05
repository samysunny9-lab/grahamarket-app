package com.grahamarket.astro

import java.util.Date
import kotlin.math.abs

/**
 * Assembles complete [GrahaPosition]s: sidereal longitude, retrograde state,
 * dignity (exaltation/debilitation/own-sign) and combustion.
 */
object GrahaEngine {

    /** Exaltation sign index (0=Mesha) and the degree of deep exaltation for each graha. */
    private val exaltationSign = mapOf(
        Graha.SUN to 0,       // Aries
        Graha.MOON to 1,      // Taurus
        Graha.MARS to 9,      // Capricorn
        Graha.MERCURY to 5,   // Virgo
        Graha.JUPITER to 3,   // Cancer
        Graha.VENUS to 11,    // Pisces
        Graha.SATURN to 6     // Libra
    )

    /** Own signs (rulership) by graha. Rahu/Ketu excluded (no classical rulership). */
    private val ownSigns = mapOf(
        Graha.SUN to listOf(4),            // Leo
        Graha.MOON to listOf(3),           // Cancer
        Graha.MARS to listOf(0, 7),        // Aries, Scorpio
        Graha.MERCURY to listOf(2, 5),     // Gemini, Virgo
        Graha.JUPITER to listOf(8, 11),    // Sagittarius, Pisces
        Graha.VENUS to listOf(1, 6),       // Taurus, Libra
        Graha.SATURN to listOf(9, 10)      // Capricorn, Aquarius
    )

    /** Combustion orbs (degrees from Sun) per graha, classical values. */
    private val combustionOrb = mapOf(
        Graha.MOON to 12.0,
        Graha.MARS to 17.0,
        Graha.MERCURY to 14.0,
        Graha.JUPITER to 11.0,
        Graha.VENUS to 10.0,
        Graha.SATURN to 15.0
    )

    private fun dignityFor(graha: Graha, siderealLon: Double): Dignity {
        val signIndex = (siderealLon / 30.0).toInt() % 12
        val exalt = exaltationSign[graha]
        if (exalt != null) {
            if (signIndex == exalt) return Dignity.EXALTED
            // Debilitation is the sign opposite the exaltation sign.
            if (signIndex == (exalt + 6) % 12) return Dignity.DEBILITATED
        }
        if (ownSigns[graha]?.contains(signIndex) == true) return Dignity.OWN_SIGN
        return Dignity.NEUTRAL
    }

    private fun isRetrograde(graha: Graha, date: Date): Boolean {
        // Nodes are always retrograde in mean motion.
        if (graha == Graha.RAHU || graha == Graha.KETU) return true
        // Sun and Moon never retrograde from Earth.
        if (graha == Graha.SUN || graha == Graha.MOON) return false

        val now = Ephemeris.tropicalLongitude(graha, date)
        val later = Ephemeris.tropicalLongitude(graha, Date(date.time + 24L * 3600 * 1000))
        var delta = later - now
        if (delta > 180) delta -= 360
        if (delta < -180) delta += 360
        return delta < 0
    }

    private fun isCombust(graha: Graha, siderealLon: Double, sunSiderealLon: Double): Boolean {
        val orb = combustionOrb[graha] ?: return false
        var sep = abs(siderealLon - sunSiderealLon)
        if (sep > 180) sep = 360 - sep
        return sep <= orb
    }

    fun positionFor(graha: Graha, date: Date, sunSiderealLon: Double): GrahaPosition {
        val jd = AstroMath.julianDay(date)
        val tropical = Ephemeris.tropicalLongitude(graha, date)
        val sidereal = AstroMath.toSidereal(tropical, jd)
        return GrahaPosition(
            graha = graha,
            siderealLon = sidereal,
            retrograde = isRetrograde(graha, date),
            dignity = dignityFor(graha, sidereal),
            combust = if (graha == Graha.SUN) false else isCombust(graha, sidereal, sunSiderealLon)
        )
    }

    /** Compute every graha's position for a given instant. */
    fun allPositions(date: Date): List<GrahaPosition> {
        val sunJd = AstroMath.julianDay(date)
        val sunSidereal = AstroMath.toSidereal(Ephemeris.tropicalLongitude(Graha.SUN, date), sunJd)
        return Graha.entries.map { positionFor(it, date, sunSidereal) }
    }
}
