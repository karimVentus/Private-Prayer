package com.prayertime.sensor

import android.hardware.SensorManager

/** Portrait compass heading from accelerometer + magnetometer fusion. */
object CompassHeading {
    /**
     * Reads device heading the same way as classic Qibla compass Activities:
     * [SensorManager.getRotationMatrix] then [SensorManager.getOrientation] with no remap.
     */
    fun readingFromRotationMatrix(
        rotationMatrix: FloatArray,
        orientation: FloatArray = FloatArray(3),
    ): CompassReading {
        SensorManager.getOrientation(rotationMatrix, orientation)
        return CompassReading(
            azimuth = normalizeDegrees(Math.toDegrees(orientation[0].toDouble()).toFloat()),
            pitch = Math.toDegrees(orientation[1].toDouble()).toFloat(),
            roll = Math.toDegrees(orientation[2].toDouble()).toFloat(),
        )
    }

    fun toTrueNorth(
        magneticAzimuth: Float,
        latitude: Double,
        longitude: Double,
        altitudeMeters: Float = 0f,
        timeMillis: Long = System.currentTimeMillis(),
    ): Float {
        val declination =
            CompassGeographicField.correctionAt(
                latitude = latitude,
                longitude = longitude,
                altitudeMeters = altitudeMeters,
                timeMillis = timeMillis,
            ).declinationDegrees
        return normalizeDegrees(magneticAzimuth + declination)
    }

    fun applyUserOffset(
        trueNorthDegrees: Float,
        offsetDegrees: Float,
    ): Float = normalizeDegrees(trueNorthDegrees + offsetDegrees)

    private fun normalizeDegrees(degrees: Float): Float = ((degrees % 360f) + 360f) % 360f
}
