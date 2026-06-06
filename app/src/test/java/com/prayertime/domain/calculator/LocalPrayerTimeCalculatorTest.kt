package com.prayertime.domain.calculator

import com.prayertime.domain.model.Prayer
import com.prayertime.domain.model.PrayerTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class LocalPrayerTimeCalculatorTest {
    @Test
    fun hameln_june_umm_al_qura_shafi_high_latitude() {
        val times =
            LocalPrayerTimeCalculator.calculate(
                latitude = 52.103,
                longitude = 9.356,
                timezone = "Europe/Berlin",
                date = dateIn("Europe/Berlin", 2026, Calendar.JUNE, 3),
            )

        assertEquals("02:48", time(times, Prayer.FAJR))
        assertEquals("13:21", time(times, Prayer.DHUHR))
        assertEquals("17:43", time(times, Prayer.ASR))
        assertEquals("21:36", time(times, Prayer.MAGHRIB))
        assertEquals("23:06", time(times, Prayer.ISHA))
        assertEquals("05:07", time(times, Prayer.SHURUQ))
    }

    @Test
    fun berlin_june_returns_six_prayers_in_order() {
        val times =
            LocalPrayerTimeCalculator.calculate(
                latitude = 52.52,
                longitude = 13.405,
                timezone = "Europe/Berlin",
            )

        assertSixPrayersInChronologicalOrder(times)
    }

    @Test
    fun sydney_june_southern_hemisphere_golden_times() {
        val times =
            LocalPrayerTimeCalculator.calculate(
                latitude = -33.87,
                longitude = 151.21,
                timezone = "Australia/Sydney",
                date = dateIn("Australia/Sydney", 2026, Calendar.JUNE, 15),
            )

        assertEquals("05:26", time(times, Prayer.FAJR))
        assertEquals("06:58", time(times, Prayer.SHURUQ))
        assertEquals("11:56", time(times, Prayer.DHUHR))
        assertEquals("14:35", time(times, Prayer.ASR))
        assertEquals("16:53", time(times, Prayer.MAGHRIB))
        assertEquals("18:23", time(times, Prayer.ISHA))
        assertSixPrayersInChronologicalOrder(times)
    }

    @Test
    fun auckland_june_near_date_line_golden_times() {
        val times =
            LocalPrayerTimeCalculator.calculate(
                latitude = -36.8485,
                longitude = 174.7633,
                timezone = "Pacific/Auckland",
                date = dateIn("Pacific/Auckland", 2026, Calendar.JUNE, 15),
            )

        assertEquals("05:56", time(times, Prayer.FAJR))
        assertEquals("07:32", time(times, Prayer.SHURUQ))
        assertEquals("12:21", time(times, Prayer.DHUHR))
        assertEquals("14:53", time(times, Prayer.ASR))
        assertEquals("17:11", time(times, Prayer.MAGHRIB))
        assertEquals("18:41", time(times, Prayer.ISHA))
        assertSixPrayersInChronologicalOrder(times)
    }

    @Test
    fun riyadh_june_east_asia_golden_times() {
        val times =
            LocalPrayerTimeCalculator.calculate(
                latitude = 24.7136,
                longitude = 46.6753,
                timezone = "Asia/Riyadh",
                date = dateIn("Asia/Riyadh", 2026, Calendar.JUNE, 15),
            )

        assertEquals("03:32", time(times, Prayer.FAJR))
        assertEquals("05:04", time(times, Prayer.SHURUQ))
        assertEquals("11:54", time(times, Prayer.DHUHR))
        assertEquals("15:15", time(times, Prayer.ASR))
        assertEquals("18:44", time(times, Prayer.MAGHRIB))
        assertEquals("20:14", time(times, Prayer.ISHA))
        assertSixPrayersInChronologicalOrder(times)
    }

    @Test
    fun tokyo_june_east_asia_golden_times() {
        val times =
            LocalPrayerTimeCalculator.calculate(
                latitude = 35.6762,
                longitude = 139.6503,
                timezone = "Asia/Tokyo",
                date = dateIn("Asia/Tokyo", 2026, Calendar.JUNE, 15),
            )

        assertEquals("02:33", time(times, Prayer.FAJR))
        assertEquals("04:25", time(times, Prayer.SHURUQ))
        assertEquals("11:42", time(times, Prayer.DHUHR))
        assertEquals("15:31", time(times, Prayer.ASR))
        assertEquals("18:59", time(times, Prayer.MAGHRIB))
        assertEquals("20:29", time(times, Prayer.ISHA))
        assertSixPrayersInChronologicalOrder(times)
    }

    @Test
    fun high_latitude_twilight_rule_applies_from_48_degrees() {
        val date = dateIn("Europe/Berlin", 2026, Calendar.JUNE, 3)
        val belowThreshold =
            LocalPrayerTimeCalculator.calculate(
                latitude = 47.9,
                longitude = 9.356,
                timezone = "Europe/Berlin",
                date = date,
            )
        val atThreshold =
            LocalPrayerTimeCalculator.calculate(
                latitude = 48.0,
                longitude = 9.356,
                timezone = "Europe/Berlin",
                date = date,
            )

        // Below 48°N: default rule — Fajr much earlier in midsummer.
        assertEquals("02:21", time(belowThreshold, Prayer.FAJR))
        assertEquals("22:45", time(belowThreshold, Prayer.ISHA))
        // At 48°N+: TWILIGHT_ANGLE — later Fajr, slightly later Isha.
        assertEquals("02:55", time(atThreshold, Prayer.FAJR))
        assertEquals("22:46", time(atThreshold, Prayer.ISHA))
        assertTrue(
            "Twilight rule should delay Fajr at |lat| ≥ 48°",
            time(atThreshold, Prayer.FAJR) > time(belowThreshold, Prayer.FAJR),
        )
    }

    @Test
    fun oslo_june_solstice_high_latitude_golden_times() {
        val times =
            LocalPrayerTimeCalculator.calculate(
                latitude = 59.9139,
                longitude = 10.7522,
                timezone = "Europe/Oslo",
                date = dateIn("Europe/Oslo", 2026, Calendar.JUNE, 21),
            )

        assertEquals("02:18", time(times, Prayer.FAJR))
        assertEquals("03:54", time(times, Prayer.SHURUQ))
        assertEquals("13:19", time(times, Prayer.DHUHR))
        assertEquals("18:00", time(times, Prayer.ASR))
        assertEquals("22:44", time(times, Prayer.MAGHRIB))
        // Isha after midnight — valid in high-latitude summer with twilight rule.
        assertEquals("00:14", time(times, Prayer.ISHA))
        assertSixPrayersInChronologicalOrder(times)
    }

    @Test
    fun london_march_31_dst_spring_forward_returns_six_ordered_prayers() {
        val times =
            LocalPrayerTimeCalculator.calculate(
                latitude = 51.5074,
                longitude = -0.1278,
                timezone = "Europe/London",
                date = dateIn("Europe/London", 2024, Calendar.MARCH, 31),
            )

        assertSixPrayersInChronologicalOrder(times)
    }

    private fun dateIn(
        timezone: String,
        year: Int,
        month: Int,
        day: Int,
    ): Date {
        val tz = TimeZone.getTimeZone(timezone)
        return Calendar.getInstance(tz).apply {
            set(year, month, day, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    private fun time(
        times: List<PrayerTime>,
        prayer: Prayer,
    ): String = times.first { it.prayer == prayer }.displayTime

    private fun assertSixPrayersInChronologicalOrder(times: List<PrayerTime>) {
        assertEquals(
            listOf(Prayer.FAJR, Prayer.SHURUQ, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA),
            times.map { it.prayer },
        )
        val timestamps = times.map { it.timestamp }
        for (i in 1 until timestamps.size) {
            assertTrue("Timestamps not in order at index $i", timestamps[i] > timestamps[i - 1])
        }
    }
}
