package com.prayertime.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class CountdownFormatterTest {
    @Test
    fun formatsHoursMinutes_whenOverOneHour() {
        val text =
            CountdownFormatter.format(
                millis = 2 * 3_600_000L + 14 * 60_000L,
                hoursUnit = "h",
                minutesUnit = "m",
            )
        assertEquals("2h 14m", text)
    }

    @Test
    fun formatsMinutes_whenUnderOneHour() {
        val text =
            CountdownFormatter.format(
                millis = 5 * 60_000L + 9_000L,
                hoursUnit = "h",
                minutesUnit = "m",
            )
        assertEquals("5m", text)
    }

    @Test
    fun formatCompact_omitsSpaces() {
        val text =
            CountdownFormatter.formatCompact(
                millis = 2 * 3_600_000L + 14 * 60_000L,
                hoursUnit = "h",
                minutesUnit = "m",
            )
        assertEquals("2h14m", text)
    }

    @Test
    fun negativeMillis_coercedToZeroMinutes() {
        val text =
            CountdownFormatter.format(
                millis = -60_000L,
                hoursUnit = "h",
                minutesUnit = "m",
            )
        assertEquals("0m", text)
    }
}
