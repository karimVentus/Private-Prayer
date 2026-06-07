package com.prayertime.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides device azimuth (degrees from magnetic north) via the rotation vector sensor.
 * Falls back to the magnetometer+accelerometer if rotation vector is unavailable.
 *
 * No runtime permissions required — orientation sensors are freely accessible.
 */
@Singleton
class CompassSensor
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val sensorManager: SensorManager? =
            context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

        /** Azimuth (0..360, 0=North) as a continuous flow. Completes when collected stops. */
        val azimuth: Flow<Float> = callbackFlow {
            val manager = sensorManager ?: run { close(); return@callbackFlow }

            // Prefer rotation vector (better fusion), fall back to magnetic field + accelerometer
            val sensor =
                manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
                    ?: manager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
                    ?: manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

            if (sensor == null) {
                close()
                return@callbackFlow
            }

            val rotationMatrix = FloatArray(9)
            val orientationValues = FloatArray(3)

            val listener =
                object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        val azimuthDegrees = when (event.sensor.type) {
                            Sensor.TYPE_ROTATION_VECTOR,
                            Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> {
                                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                                SensorManager.getOrientation(rotationMatrix, orientationValues)
                                Math.toDegrees(orientationValues[0].toDouble()).toFloat()
                            }
                            Sensor.TYPE_MAGNETIC_FIELD -> {
                                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                                SensorManager.getOrientation(rotationMatrix, orientationValues)
                                Math.toDegrees(orientationValues[0].toDouble()).toFloat()
                            }
                            else -> return
                        }
                        val normalized = ((azimuthDegrees + 360) % 360).toFloat()
                        trySend(normalized)
                    }

                    override fun onAccuracyChanged(
                        sensor: Sensor,
                        accuracy: Int,
                    ) {
                        _accuracy.value = accuracy
                    }
                }

            manager.registerListener(
                listener,
                sensor,
                SensorManager.SENSOR_DELAY_UI,
            )

            awaitClose {
                manager.unregisterListener(listener)
            }
        }.flowOn(Dispatchers.Default)

    private val _accuracy = MutableStateFlow(SensorManager.SENSOR_STATUS_ACCURACY_HIGH)

    /**
     * Current sensor accuracy level.
     * - [SensorManager.SENSOR_STATUS_ACCURACY_HIGH] (3) = good
     * - [SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM] (2) = ok
     * - [SensorManager.SENSOR_STATUS_ACCURACY_LOW] (1) = needs calibration
     * - [SensorManager.SENSOR_STATUS_UNRELIABLE] (0) = unusable
     */
    val accuracy: Flow<Int> = _accuracy

        /** Whether the device has any orientation/magnetic sensor. */
        val isAvailable: Boolean
            get() =
                sensorManager?.let { mgr ->
                    mgr.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null ||
                        mgr.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) != null ||
                        mgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null
                } ?: false
    }
