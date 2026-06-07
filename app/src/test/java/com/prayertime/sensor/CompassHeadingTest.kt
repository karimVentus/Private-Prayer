package com.prayertime.sensor

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CompassHeadingTest {
    @Test
    fun `toTrueNorth shifts magnetic heading by declination at Hameln`() {
        val magnetic = 90f
        val trueNorth = CompassHeading.toTrueNorth(magnetic, 52.1, 9.35)
        val delta = trueNorth - magnetic
        val wrapped =
            when {
                delta > 180f -> delta - 360f
                delta < -180f -> delta + 360f
                else -> delta
            }
        assertTrue("declination delta=$wrapped", wrapped in 0.5f..8f)
    }

    @Test
    fun `toTrueNorth normalizes into 0 to 360`() {
        val heading = CompassHeading.toTrueNorth(350f, 52.1, 9.35)
        assertTrue(heading in 0f..360f)
    }

    @Test
    fun `readingFromRotationMatrix returns normalized azimuth`() {
        val identity =
            floatArrayOf(
                1f, 0f, 0f,
                0f, 1f, 0f,
                0f, 0f, 1f,
            )
        val reading = CompassHeading.readingFromRotationMatrix(identity)
        assertTrue(reading.azimuth in 0f..360f)
    }
}
