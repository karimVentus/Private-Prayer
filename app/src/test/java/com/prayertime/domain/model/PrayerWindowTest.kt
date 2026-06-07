package com.prayertime.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerWindowTest {
    private val times =
        listOf(
            PrayerTime(Prayer.FAJR, "05:00", 1_000L),
            PrayerTime(Prayer.DHUHR, "12:00", 10_000L),
            PrayerTime(Prayer.MAGHRIB, "18:00", 20_000L),
        )

    @Test
    fun isInPrayerWindow_true_between_prayer_and_next() {
        assertTrue(isInPrayerWindow(Prayer.DHUHR, times, now = 12_000L))
    }

    @Test
    fun isInPrayerWindow_false_before_prayer_time() {
        assertFalse(isInPrayerWindow(Prayer.MAGHRIB, times, now = 15_000L))
    }

    @Test
    fun isInPrayerWindow_false_after_next_prayer_started() {
        assertFalse(isInPrayerWindow(Prayer.DHUHR, times, now = 20_000L))
    }
}
