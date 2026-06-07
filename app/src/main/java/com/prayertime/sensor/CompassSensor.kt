package com.prayertime.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject
import javax.inject.Singleton

data class CompassReading(
    val azimuth: Float,
    val pitch: Float,
    val roll: Float,
)

/**
 * Accelerometer + magnetometer fusion — same pipeline as standard Android Qibla Activities.
 * No runtime permissions required.
 */
@Singleton
class CompassSensor
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val sensorManager: SensorManager? =
            context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        private val _accuracy = MutableStateFlow(SensorManager.SENSOR_STATUS_ACCURACY_HIGH)

        val accuracy: StateFlow<Int> = _accuracy.asStateFlow()

        val readings: SharedFlow<CompassReading> =
            callbackFlow {
                val manager =
                    sensorManager ?: run {
                        close()
                        return@callbackFlow
                    }

                val accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                val magnetometer = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
                if (accelerometer == null || magnetometer == null) {
                    close()
                    return@callbackFlow
                }

                val gravity = FloatArray(3)
                val geomagnetic = FloatArray(3)
                val rotationMatrix = FloatArray(9)
                val inclinationMatrix = FloatArray(9)
                var hasGravity = false
                var hasGeomagnetic = false

                val listener =
                    object : SensorEventListener {
                        override fun onSensorChanged(event: SensorEvent) {
                            when (event.sensor.type) {
                                Sensor.TYPE_ACCELEROMETER -> {
                                    System.arraycopy(event.values, 0, gravity, 0, event.values.size)
                                    hasGravity = true
                                }
                                Sensor.TYPE_MAGNETIC_FIELD -> {
                                    System.arraycopy(event.values, 0, geomagnetic, 0, event.values.size)
                                    hasGeomagnetic = true
                                }
                            }
                            if (!hasGravity || !hasGeomagnetic) return
                            if (
                                !SensorManager.getRotationMatrix(
                                    rotationMatrix,
                                    inclinationMatrix,
                                    gravity,
                                    geomagnetic,
                                )
                            ) {
                                return
                            }
                            trySend(CompassHeading.readingFromRotationMatrix(rotationMatrix))
                        }

                        override fun onAccuracyChanged(
                            sensor: Sensor,
                            accuracy: Int,
                        ) {
                            if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                                _accuracy.value = accuracy
                            }
                        }
                    }

                manager.registerListener(
                    listener,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_UI,
                )
                manager.registerListener(
                    listener,
                    magnetometer,
                    SensorManager.SENSOR_DELAY_UI,
                )

                awaitClose {
                    manager.unregisterListener(listener)
                }
            }.shareIn(
                scope,
                SharingStarted.WhileSubscribed(stopTimeoutMillis = 60_000),
                replay = 0,
            )

        val isAvailable: Boolean
            get() =
                sensorManager?.let { mgr ->
                    mgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null &&
                        mgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null
                } ?: false
    }
