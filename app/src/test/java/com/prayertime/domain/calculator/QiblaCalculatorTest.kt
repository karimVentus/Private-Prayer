package com.prayertime.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QiblaCalculatorTest {
    private val tolerance = 1.0f

    @Test
    fun `Mecca to Qibla is 0 degrees`() {
        val bearing = QiblaCalculator.bearing(21.4225, 39.8262)
        assertEquals(0f, bearing, tolerance)
    }

    @Test
    fun `Hameln Germany bearing`() {
        val bearing = QiblaCalculator.bearing(52.1, 9.35)
        assertTrue("Hameln: $bearing", bearing in 120f..135f)
    }

    @Test
    fun `Berlin bearing`() {
        val bearing = QiblaCalculator.bearing(52.52, 13.405)
        assertTrue("Berlin: $bearing", bearing in 115f..145f)
    }

    @Test
    fun `Damascus bearing`() {
        val bearing = QiblaCalculator.bearing(33.5, 36.3)
        assertTrue("Damascus: $bearing", bearing in 160f..175f)
    }

    @Test
    fun `Bearing is normalized 0 to 360`() {
        val bearing = QiblaCalculator.bearing(-90.0, 0.0)
        assertTrue("bearing=$bearing", bearing in 0f..360f)
    }

    @Test
    fun `cardinalDirection north`() {
        assertEquals("N", QiblaCalculator.cardinalDirection(0f))
        assertEquals("N", QiblaCalculator.cardinalDirection(350f))
        assertEquals("N", QiblaCalculator.cardinalDirection(10f))
    }

    @Test
    fun `cardinalDirection east`() {
        assertEquals("E", QiblaCalculator.cardinalDirection(90f))
        assertEquals("E", QiblaCalculator.cardinalDirection(100f))
    }

    @Test
    fun `cardinalDirection south`() {
        assertEquals("S", QiblaCalculator.cardinalDirection(180f))
        assertEquals("S", QiblaCalculator.cardinalDirection(190f))
    }

    @Test
    fun `cardinalDirection west`() {
        assertEquals("W", QiblaCalculator.cardinalDirection(270f))
        assertEquals("W", QiblaCalculator.cardinalDirection(260f))
    }

    @Test
    fun `cardinalDirection southeast`() {
        assertEquals("SE", QiblaCalculator.cardinalDirection(135f))
    }

    @Test
    fun `Hameln cardinal direction is SE`() {
        val bearing = QiblaCalculator.bearing(52.1, 9.35)
        assertEquals("SE", QiblaCalculator.cardinalDirection(bearing))
    }
}
