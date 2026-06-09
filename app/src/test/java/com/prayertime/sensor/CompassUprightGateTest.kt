package com.prayertime.sensor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompassUprightGateTest {
    @Test
    fun `upright when pitch and roll within portrait band`() {
        assertTrue(CompassUprightGate.isUpright(pitchDegrees = 10f, rollDegrees = 5f))
    }

    @Test
    fun `not upright when pitch exceeds limit`() {
        assertFalse(CompassUprightGate.isUpright(pitchDegrees = 60f, rollDegrees = 0f))
    }

    @Test
    fun `not upright when roll exceeds limit`() {
        assertFalse(CompassUprightGate.isUpright(pitchDegrees = 0f, rollDegrees = 40f))
    }
}
