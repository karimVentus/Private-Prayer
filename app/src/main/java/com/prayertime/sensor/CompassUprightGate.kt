package com.prayertime.sensor

import kotlin.math.abs

/** Portrait compass: accept heading only when the phone is held roughly upright (Phase 7B). */
object CompassUprightGate {
    const val MAX_ABS_PITCH_DEGREES = 50f
    const val MAX_ABS_ROLL_DEGREES = 35f

    fun isUpright(
        pitchDegrees: Float,
        rollDegrees: Float,
    ): Boolean =
        abs(pitchDegrees) <= MAX_ABS_PITCH_DEGREES &&
            abs(rollDegrees) <= MAX_ABS_ROLL_DEGREES
}
