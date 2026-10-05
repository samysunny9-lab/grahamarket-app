package com.grahamarket.astro

import com.grahamarket.astro.AstroMath.DEG2RAD
import com.grahamarket.astro.AstroMath.RAD2DEG
import com.grahamarket.astro.AstroMath.cosDeg
import com.grahamarket.astro.AstroMath.norm360
import com.grahamarket.astro.AstroMath.sinDeg
import java.util.Date
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Computes geocentric ecliptic longitudes of the planets, Sun and Moon using the
 * standard low-precision Keplerian elements (Schlyter / Meeus). Longitudes are
 * returned in the TROPICAL frame; callers apply the Lahiri ayanamsa to get sidereal.
 *
 * This is deliberately a self-contained, dependency-free implementation so the app
 * works offline with no native Swiss Ephemeris binary required.
 */
object Ephemeris {

    /** Orbital elements as linear functions of d = days since 2000 Jan 0.0 TT. */
    private data class Elements(
        val N0: Double, val Nd: Double,   // longitude of ascending node
        val i0: Double, val id: Double,   // inclination
        val w0: Double, val wd: Double,   // argument of perihelion
        val a0: Double, val ad: Double,   // semi-major axis (AU)
        val e0: Double, val ed: Double,   // eccentricity
        val M0: Double, val Md: Double    // mean anomaly
    )

    // Element sets from Paul Schlyter's "How to compute planetary positions".
    private val SUN = Elements(0.0,0.0, 0.0,0.0, 282.9404,4.70935e-5, 1.0,0.0, 0.016709,-1.151e-9, 356.0470,0.9856002585)
    private val MOON = Elements(125.1228,-0.0529538083, 5.1454,0.0, 318.0634,0.1643573223, 60.2666,0.0, 0.054900,0.0, 115.3654,13.0649929509)
    private val MARS = Elements(49.5574,2.11081e-5, 1.8497,-1.78e-8, 286.5016,2.92961e-5, 1.523688,0.0, 0.093405,2.516e-9, 18.6021,0.5240207766)
    private val MERCURY = Elements(48.3313,3.24587e-5, 7.0047,5.00e-8, 29.1241,1.01444e-5, 0.387098,0.0, 0.205635,5.59e-10, 168.6562,4.0923344368)
    private val JUPITER = Elements(100.4542,2.76854e-5, 1.3030,-1.557e-7, 273.8777,1.64505e-5, 5.20256,0.0, 0.048498,4.469e-9, 19.8950,0.0830853001)
    private val VENUS = Elements(76.6799,2.46590e-5, 3.3946,2.75e-8, 54.8910,1.38374e-5, 0.723330,0.0, 0.006773,-1.302e-9, 48.0052,1.6021302244)
    private val SATURN = Elements(113.6634,2.38980e-5, 2.4886,-1.081e-7, 339.3939,2.97661e-5, 9.55475,0.0, 0.055546,-9.499e-9, 316.9670,0.0334442282)

    private fun daysSince2000(date: Date): Double {
        // d = JD - 2451543.5 (Schlyter's epoch, 1999 Dec 31 0:00 UT)
        return AstroMath.julianDay(date) - 2451543.5
    }

    /** Solve Kepler's equation for eccentric anomaly E (degrees). */
    private fun eccentricAnomaly(M: Double, e: Double): Double {
        var E = M + RAD2DEG * e * sinDeg(M) * (1 + e * cosDeg(M))
        repeat(8) {
            val dM = M - (E - RAD2DEG * e * sinDeg(E))
            val dE = dM / (1 - e * cosDeg(E))
            E += dE
        }
        return E
    }

    /** Heliocentric rectangular ecliptic coordinates (x,y,z) in AU for a planet. */
    private fun heliocentric(el: Elements, d: Double): DoubleArray {
        val N = el.N0 + el.Nd * d
        val i = el.i0 + el.id * d
        val w = el.w0 + el.wd * d
        val a = el.a0 + el.ad * d
        val e = el.e0 + el.ed * d
        val M = norm360(el.M0 + el.Md * d)

        val E = eccentricAnomaly(M, e)
        val xv = a * (cosDeg(E) - e)
        val yv = a * (sqrt(1 - e * e) * sinDeg(E))
        val v = atan2(yv, xv) * RAD2DEG
        val r = sqrt(xv * xv + yv * yv)

        val vw = (v + w) * DEG2RAD
        val nR = N * DEG2RAD
        val iR = i * DEG2RAD
        val x = r * (cos(nR) * cos(vw) - sin(nR) * sin(vw) * cos(iR))
        val y = r * (sin(nR) * cos(vw) + cos(nR) * sin(vw) * cos(iR))
        val z = r * (sin(vw) * sin(iR))
        return doubleArrayOf(x, y, z)
    }

    /** Geocentric tropical longitude (deg) of the Sun. */
    private fun sunLongitude(d: Double): Double {
        val w = SUN.w0 + SUN.wd * d
        val e = SUN.e0 + SUN.ed * d
        val M = norm360(SUN.M0 + SUN.Md * d)
        val E = eccentricAnomaly(M, e)
        val xv = cosDeg(E) - e
        val yv = sqrt(1 - e * e) * sinDeg(E)
        val v = atan2(yv, xv) * RAD2DEG
        return norm360(v + w)
    }

    /** Returns tropical longitude in degrees for a given planet (not nodes). */
    private fun planetTropicalLongitude(el: Elements, d: Double): Double {
        val sun = sunRect(d)
        val p = heliocentric(el, d)
        val xg = p[0] + sun[0]
        val yg = p[1] + sun[1]
        val zg = p[2] + sun[2]
        return norm360(atan2(yg, xg) * RAD2DEG)
    }

    /** Sun's rectangular geocentric coords (used to shift heliocentric to geocentric). */
    private fun sunRect(d: Double): DoubleArray {
        val lon = sunLongitude(d)
        val e = SUN.e0 + SUN.ed * d
        val M = norm360(SUN.M0 + SUN.Md * d)
        val E = eccentricAnomaly(M, e)
        val xv = cosDeg(E) - e
        val yv = sqrt(1 - e * e) * sinDeg(E)
        val r = sqrt(xv * xv + yv * yv)
        return doubleArrayOf(r * cosDeg(lon), r * sinDeg(lon), 0.0)
    }

    /** Moon's geocentric tropical longitude with principal perturbations (deg). */
    private fun moonLongitude(d: Double): Double {
        val N = MOON.N0 + MOON.Nd * d
        val i = MOON.i0
        val w = MOON.w0 + MOON.wd * d
        val a = MOON.a0
        val e = MOON.e0
        val M = norm360(MOON.M0 + MOON.Md * d)
        val E = eccentricAnomaly(M, e)

        val xv = a * (cosDeg(E) - e)
        val yv = a * (sqrt(1 - e * e) * sinDeg(E))
        val v = atan2(yv, xv) * RAD2DEG
        val r = sqrt(xv * xv + yv * yv)

        val vw = (v + w) * DEG2RAD
        val nR = N * DEG2RAD
        val iR = i * DEG2RAD
        val xe = r * (cos(nR) * cos(vw) - sin(nR) * sin(vw) * cos(iR))
        val ye = r * (sin(nR) * cos(vw) + cos(nR) * sin(vw) * cos(iR))
        var lon = atan2(ye, xe) * RAD2DEG

        // Principal periodic perturbations in longitude.
        val Ms = norm360(SUN.M0 + SUN.Md * d)
        val Mm = M
        val Ls = norm360(SUN.w0 + Ms)
        val Lm = norm360(N + w + Mm)
        val Dm = Lm - Ls
        val F = Lm - N

        lon += -1.274 * sinDeg(Mm - 2 * Dm)
        lon += +0.658 * sinDeg(2 * Dm)
        lon += -0.186 * sinDeg(Ms)
        lon += -0.059 * sinDeg(2 * Mm - 2 * Dm)
        lon += -0.057 * sinDeg(Mm - 2 * Dm + Ms)
        lon += +0.053 * sinDeg(Mm + 2 * Dm)
        lon += +0.046 * sinDeg(2 * Dm - Ms)
        lon += +0.041 * sinDeg(Mm - Ms)
        lon += -0.035 * sinDeg(Dm)
        lon += -0.031 * sinDeg(Mm + Ms)
        lon += -0.015 * sinDeg(2 * F - 2 * Dm)
        lon += +0.011 * sinDeg(Mm - 4 * Dm)
        return norm360(lon)
    }

    /** Mean longitude of the Moon's ascending node (Rahu), tropical (deg). */
    private fun rahuLongitude(d: Double): Double = norm360(MOON.N0 + MOON.Nd * d)

    /**
     * Computes the tropical longitude for any graha at the given instant.
     * Nodes are always treated as retrograde (their mean motion is retrograde).
     */
    fun tropicalLongitude(graha: Graha, date: Date): Double {
        val d = daysSince2000(date)
        return when (graha) {
            Graha.SUN -> sunLongitude(d)
            Graha.MOON -> moonLongitude(d)
            Graha.MARS -> planetTropicalLongitude(MARS, d)
            Graha.MERCURY -> planetTropicalLongitude(MERCURY, d)
            Graha.JUPITER -> planetTropicalLongitude(JUPITER, d)
            Graha.VENUS -> planetTropicalLongitude(VENUS, d)
            Graha.SATURN -> planetTropicalLongitude(SATURN, d)
            Graha.RAHU -> rahuLongitude(d)
            Graha.KETU -> norm360(rahuLongitude(d) + 180.0)
        }
    }
}
