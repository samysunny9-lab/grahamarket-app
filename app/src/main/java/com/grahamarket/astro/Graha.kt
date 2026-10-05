package com.grahamarket.astro

/** The nine grahas of Vedic astrology. */
enum class Graha(val displayName: String, val sanskrit: String, val benefic: Boolean) {
    SUN("Sun", "Surya", false),
    MOON("Moon", "Chandra", true),
    MARS("Mars", "Mangal", false),
    MERCURY("Mercury", "Budha", true),
    JUPITER("Jupiter", "Guru", true),
    VENUS("Venus", "Shukra", true),
    SATURN("Saturn", "Shani", false),
    RAHU("Rahu (N. Node)", "Rahu", false),
    KETU("Ketu (S. Node)", "Ketu", false);
}

/** The twelve rashis (zodiac signs), each 30° of sidereal longitude. */
val RASHIS = listOf(
    "Mesha (Aries)", "Vrishabha (Taurus)", "Mithuna (Gemini)", "Karka (Cancer)",
    "Simha (Leo)", "Kanya (Virgo)", "Tula (Libra)", "Vrishchika (Scorpio)",
    "Dhanu (Sagittarius)", "Makara (Capricorn)", "Kumbha (Aquarius)", "Meena (Pisces)"
)

/** The 27 nakshatras, each spanning 13°20' of sidereal longitude. */
val NAKSHATRAS = listOf(
    "Ashwini", "Bharani", "Krittika", "Rohini", "Mrigashira", "Ardra",
    "Punarvasu", "Pushya", "Ashlesha", "Magha", "Purva Phalguni", "Uttara Phalguni",
    "Hasta", "Chitra", "Swati", "Vishakha", "Anuradha", "Jyeshtha",
    "Mula", "Purva Ashadha", "Uttara Ashadha", "Shravana", "Dhanishta", "Shatabhisha",
    "Purva Bhadrapada", "Uttara Bhadrapada", "Revati"
)

/**
 * A fully computed position for one graha at a given instant.
 *
 * @param siderealLon sidereal (Lahiri) ecliptic longitude in degrees [0,360)
 * @param retrograde whether the graha is in apparent retrograde (vakri) motion
 * @param dignity exaltation / debilitation / own-sign status
 * @param combust whether the graha is combust (too close to the Sun)
 */
data class GrahaPosition(
    val graha: Graha,
    val siderealLon: Double,
    val retrograde: Boolean,
    val dignity: Dignity,
    val combust: Boolean
) {
    val rashiIndex: Int get() = (siderealLon / 30.0).toInt() % 12
    val rashi: String get() = RASHIS[rashiIndex]
    val degreesInRashi: Double get() = siderealLon % 30.0
    val nakshatra: String get() = NAKSHATRAS[((siderealLon / (360.0 / 27.0)).toInt()) % 27]
}

enum class Dignity(val label: String, val strengthModifier: Double) {
    EXALTED("Exalted (Uccha)", 1.0),
    OWN_SIGN("Own sign (Swakshetra)", 0.5),
    NEUTRAL("Neutral", 0.0),
    DEBILITATED("Debilitated (Neecha)", -1.0)
}
