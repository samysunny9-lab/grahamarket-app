package com.grahamarket.astro

import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Core astronomical math: Julian dates, angle helpers, and the Lahiri ayanamsa
 * used to convert tropical (Western) longitudes to sidereal (Vedic) longitudes.
 *
 * The planetary positions here use standard mean elements plus the dominant
 * periodic terms. They are accurate to roughly a few arc-minutes for the Sun/Moon
 * and better than ~0.5 degrees for the planets over 1900-2100, which is more than
 * sufficient to determine rashi (sign), nakshatra, and retrograde state.
 */
object AstroMath {

    const val DEG2RAD = PI / 180.0
    const val RAD2DEG = 180.0 / PI

    /** Normalise an angle in degrees to [0, 360). */
    fun norm360(deg: Double): Double {
        var d = deg % 360.0
        if (d < 0) d += 360.0
        return d
    }

    /** Julian Day for a given instant (UTC). */
    fun julianDay(date: Date): Double {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.time = date
        var year = cal.get(Calendar.YEAR)
        var month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val second = cal.get(Calendar.SECOND)
        val dayFraction = (hour + minute / 60.0 + second / 3600.0) / 24.0

        if (month <= 2) {
            year -= 1
            month += 12
        }
        val a = Math.floor(year / 100.0)
        val b = 2 - a + Math.floor(a / 4.0)
        return Math.floor(365.25 * (year + 4716)) +
            Math.floor(30.6001 * (month + 1)) +
            day + dayFraction + b - 1524.5
    }

    /** Julian centuries since J2000.0. */
    fun centuriesT(jd: Double): Double = (jd - 2451545.0) / 36525.0

    /**
     * Lahiri (Chitrapaksha) ayanamsa in degrees. Linear approximation anchored
     * at J2000 (≈23.853°) with the standard precession rate (~50.29"/yr).
     */
    fun lahiriAyanamsa(jd: Double): Double {
        val t = (jd - 2451545.0) / 365.25 // years since J2000
        return 23.85300 + 0.0139697 * t
    }

    /** Convert a tropical longitude (deg) to sidereal using Lahiri ayanamsa. */
    fun toSidereal(tropicalLon: Double, jd: Double): Double =
        norm360(tropicalLon - lahiriAyanamsa(jd))

    internal fun sinDeg(d: Double) = sin(d * DEG2RAD)
    internal fun cosDeg(d: Double) = cos(d * DEG2RAD)
}
