package com.prayertime.sensor

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CompassGeographicFieldTest {
    @Test
    fun `Hameln declination is positive eastward`() {
        val correction = CompassGeographicField.correctionAt(52.1, 9.35)
        assertTrue(
            "declination=${correction.declinationDegrees}",
            correction.declinationDegrees in 1f..6f,
        )
    }
}
