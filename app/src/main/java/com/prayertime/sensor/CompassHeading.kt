package com.prayertime.sensor

import android.hardware.GeomagneticField
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
    ): Float {
        val declination =
            GeomagneticField(
                latitude.toFloat(),
                longitude.toFloat(),
                0f,
                System.currentTimeMillis(),
            ).declination
        return normalizeDegrees(magneticAzimuth + declination)
    }

    private fun normalizeDegrees(degrees: Float): Float = ((degrees % 360f) + 360f) % 360f
}
