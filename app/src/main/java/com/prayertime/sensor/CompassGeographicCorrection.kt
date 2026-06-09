package com.prayertime.sensor

import android.hardware.GeomagneticField

/** City-scoped magnetic declination/inclination for compass true-north correction (Phase 7B). */
data class CompassGeographicCorrection(
    val declinationDegrees: Float,
    val inclinationDegrees: Float,
)

object CompassGeographicField {
    /**
     * @param altitudeMeters City elevation; 0f when unknown (acceptable for declination at prayer-app scale).
     */
    fun correctionAt(
        latitude: Double,
        longitude: Double,
        altitudeMeters: Float = 0f,
        timeMillis: Long = System.currentTimeMillis(),
    ): CompassGeographicCorrection {
        val field =
            GeomagneticField(
                latitude.toFloat(),
                longitude.toFloat(),
                altitudeMeters,
                timeMillis,
            )
        return CompassGeographicCorrection(
            declinationDegrees = field.declination,
            inclinationDegrees = field.inclination,
        )
    }
}
