package com.prayertime.domain.calculator

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object QiblaCalculator {
    private const val KAABA_LATITUDE = 21.4225
    private const val KAABA_LONGITUDE = 39.8262

    /**
     * Returns the Qibla direction in degrees clockwise from true north.
     * 0° = North, 90° = East, etc.
     * @param latitude User's latitude in degrees (-90 to 90)
     * @param longitude User's longitude in degrees (-180 to 180)
     */
    fun bearing(
        latitude: Double,
        longitude: Double,
    ): Float {
        val latRad = Math.toRadians(latitude)
        val kaabaLatRad = Math.toRadians(KAABA_LATITUDE)
        val deltaLng = Math.toRadians(KAABA_LONGITUDE - longitude)

        val x = sin(deltaLng) * cos(kaabaLatRad)
        val y = cos(latRad) * sin(kaabaLatRad) - sin(latRad) * cos(kaabaLatRad) * cos(deltaLng)

        val bearing = Math.toDegrees(atan2(x, y))
        return ((bearing + 360) % 360).toFloat()
    }

    /** Human-readable cardinal direction abbreviation. */
    fun cardinalDirection(bearingDegrees: Float): String =
        when {
            bearingDegrees < 22.5 || bearingDegrees >= 337.5 -> "N"
            bearingDegrees < 67.5 -> "NE"
            bearingDegrees < 112.5 -> "E"
            bearingDegrees < 157.5 -> "SE"
            bearingDegrees < 202.5 -> "S"
            bearingDegrees < 247.5 -> "SW"
            bearingDegrees < 292.5 -> "W"
            else -> "NW"
        }
}
