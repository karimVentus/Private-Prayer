package com.prayertime.data.repository

import com.prayertime.domain.model.Prayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class AladhanTimingsMapperTest {
    // --- Basic parsing ---

    @Test
    fun `parses complete Aladhan timing map into 6 prayers`() {
        val timings =
            mapOf(
                "Fajr" to "05:12",
                "Sunrise" to "06:30",
                "Dhuhr" to "12:30",
                "Asr" to "15:45",
                "Maghrib" to "18:10",
                "Isha" to "19:40",
            )
        val result = AladhanTimingsMapper.buildPrayerTimes(timings, "03-06-2024", "Asia/Riyadh")
        assertEquals(6, result.size)
        assertEquals(Prayer.FAJR, result[0].prayer)
        assertEquals(Prayer.SHURUQ, result[1].prayer) // Sunrise → Shuruq
        assertEquals(Prayer.DHUHR, result[2].prayer)
        assertEquals(Prayer.ASR, result[3].prayer)
        assertEquals(Prayer.MAGHRIB, result[4].prayer)
        assertEquals(Prayer.ISHA, result[5].prayer)
    }

    @Test
    fun `timestamps are chronological`() {
        val timings =
            mapOf(
                "Fajr" to "05:12",
                "Sunrise" to "06:30",
                "Dhuhr" to "12:30",
                "Asr" to "15:45",
                "Maghrib" to "18:10",
                "Isha" to "19:40",
            )
        val result = AladhanTimingsMapper.buildPrayerTimes(timings, "03-06-2024", "Asia/Riyadh")
        for (i in 1 until result.size) {
            assertTrue(
                "${result[i].prayer} not after ${result[i - 1].prayer}",
                result[i].timestamp > result[i - 1].timestamp,
            )
        }
    }

    // --- Sunrise → Shuruq mapping ---

    @Test
    fun `Sunrise key maps to SHURUQ prayer enum`() {
        val timings = mapOf("Sunrise" to "06:30")
        val result = AladhanTimingsMapper.buildPrayerTimes(timings, "03-06-2024", "UTC")
        assertEquals(1, result.size)
        assertEquals(Prayer.SHURUQ, result[0].prayer)
        assertEquals("06:30", result[0].displayTime)
    }

    // --- Time normalization (Aladhan appends " (EET)" etc.) ---

    @Test
    fun `strips timezone suffix from Aladhan time strings`() {
        val timings = mapOf("Fajr" to "05:12 (EET)")
        val result = AladhanTimingsMapper.buildPrayerTimes(timings, "03-06-2024", "UTC")
        assertEquals("05:12", result[0].displayTime)
    }

    @Test
    fun `handles time with multiple spaces in suffix`() {
        val timings = mapOf("Fajr" to "05:12   (AST)")
        val result = AladhanTimingsMapper.buildPrayerTimes(timings, "03-06-2024", "UTC")
        assertEquals("05:12", result[0].displayTime)
    }

    // --- Timezone-shifted timestamps ---

    @Test
    fun `same wall-clock time produces different epochs in different timezones`() {
        val timings = mapOf("Fajr" to "05:00")
        val riyadh = AladhanTimingsMapper.buildPrayerTimes(timings, "03-06-2024", "Asia/Riyadh")
        val berlin = AladhanTimingsMapper.buildPrayerTimes(timings, "03-06-2024", "Europe/Berlin")
        // Riyadh is UTC+3, Berlin is UTC+2 in summer → Riyadh Fajr is 1h earlier in UTC
        val diff = berlin[0].timestamp - riyadh[0].timestamp
        assertEquals(3_600_000L, diff) // 1 hour in millis
    }

    @Test
    fun `Mecca Fajr timestamp is correct for known date`() {
        val timings = mapOf("Fajr" to "04:15")
        val result = AladhanTimingsMapper.buildPrayerTimes(timings, "03-06-2024", "Asia/Riyadh")
        // 03-06-2024 04:15 in Asia/Riyadh (UTC+3) = 03-06-2024 01:15 UTC
        val expected =
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("Asia/Riyadh") }
                .parse("2024-06-03 04:15")!!.time
        assertEquals(expected, result[0].timestamp)
    }

    // --- Date format conversion (Aladhan uses dd-MM-yyyy) ---

    @Test
    fun `converts Aladhan date format dd-MM-yyyy to internal yyyy-MM-dd`() {
        val timings = mapOf("Fajr" to "05:00")
        val result = AladhanTimingsMapper.buildPrayerTimes(timings, "03-06-2024", "UTC")
        // Should parse "03-06-2024" as June 3, 2024 — not crash
        assertTrue(result[0].timestamp > 0)
    }

    @Test
    fun `handles date with single-digit day and month`() {
        val timings = mapOf("Fajr" to "05:00")
        val result = AladhanTimingsMapper.buildPrayerTimes(timings, "1-1-2024", "UTC")
        assertTrue(result[0].timestamp > 0)
        // January 1, 2024 05:00 UTC
        val expected =
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
                .parse("2024-01-01 05:00")!!.time
        assertEquals(expected, result[0].timestamp)
    }

    // --- Edge cases ---

    @Test
    fun `returns empty list when no timings match`() {
        val result = AladhanTimingsMapper.buildPrayerTimes(emptyMap(), "03-06-2024", "UTC")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `skips prayers missing from map but includes present ones`() {
        val timings =
            mapOf(
                "Fajr" to "05:12",
                "Dhuhr" to "12:30",
                "Isha" to "19:40",
            )
        val result = AladhanTimingsMapper.buildPrayerTimes(timings, "03-06-2024", "UTC")
        assertEquals(3, result.size)
        assertEquals(Prayer.FAJR, result[0].prayer)
        assertEquals(Prayer.DHUHR, result[1].prayer)
        assertEquals(Prayer.ISHA, result[2].prayer)
    }

    @Test
    fun `malformed time without colon is omitted`() {
        val timings = mapOf("Fajr" to "invalid")
        val result = AladhanTimingsMapper.buildPrayerTimes(timings, "03-06-2024", "UTC")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `hour-only time without minutes is omitted`() {
        val timings = mapOf("Fajr" to "05")
        val result = AladhanTimingsMapper.buildPrayerTimes(timings, "03-06-2024", "UTC")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `one malformed prayer drops full set below six`() {
        val timings =
            mapOf(
                "Fajr" to "05:12",
                "Sunrise" to "06:30",
                "Dhuhr" to "12:30",
                "Asr" to "15:45",
                "Maghrib" to "18:10",
                "Isha" to "invalid",
            )
        val result = AladhanTimingsMapper.buildPrayerTimes(timings, "03-06-2024", "UTC")
        assertEquals(5, result.size)
    }

    @Test
    fun `handles blank date string by returning empty list`() {
        val timings = mapOf("Fajr" to "05:00")
        val result = AladhanTimingsMapper.buildPrayerTimes(timings, "", "UTC")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `null api date defaults to today for timestamps`() {
        val timings = mapOf("Fajr" to "05:00")
        val result = AladhanTimingsMapper.buildPrayerTimes(timings, null, "UTC")
        assertTrue(result.isNotEmpty())
        val now = System.currentTimeMillis()
        assertTrue(result[0].timestamp in (now - 86_400_000L)..(now + 86_400_000L))
    }

    @Test
    fun `unparseable date label returns empty list`() {
        val timings = mapOf("Fajr" to "05:00")
        val result = AladhanTimingsMapper.buildPrayerTimes(timings, "not-a-date", "UTC")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `displays times with leading zeros preserved`() {
        val timings = mapOf("Fajr" to "05:02")
        val result = AladhanTimingsMapper.buildPrayerTimes(timings, "03-06-2024", "UTC")
        assertEquals("05:02", result[0].displayTime)
    }

    @Test
    fun `strips suffix but keeps non-standard time format`() {
        // Some Aladhan responses have 24h format
        val timings = mapOf("Isha" to "20:15 (GMT+3)")
        val result = AladhanTimingsMapper.buildPrayerTimes(timings, "03-06-2024", "UTC")
        assertEquals("20:15", result[0].displayTime)
    }

    // --- DST-aware timestamps (2E.2) ---

    @Test
    fun `DST-aware — London BST summer Fajr is 1h earlier in UTC than UTC zone Fajr`() {
        // Jan 15: Europe/London is GMT (UTC+0); Jul 15: BST (UTC+1)
        // Same wall-clock time 06:30 in London → 1h different in UTC
        val date = "15-07-2024" // mid-summer, BST=UTC+1
        val timings = mapOf("Fajr" to "06:30")
        val london = AladhanTimingsMapper.buildPrayerTimes(timings, date, "Europe/London")
        val utc = AladhanTimingsMapper.buildPrayerTimes(timings, date, "UTC")
        // London BST 06:30 = UTC 05:30, UTC 06:30 = UTC 06:30
        // London timestamp = UTC timestamp - 1h
        assertEquals(3_600_000L, utc[0].timestamp - london[0].timestamp)
    }

    @Test
    fun `Islamabad Fajr is before same wall-clock time in London`() {
        val timings = mapOf("Fajr" to "05:00")
        val islamabad = AladhanTimingsMapper.buildPrayerTimes(timings, "03-06-2024", "Asia/Karachi")
        val london = AladhanTimingsMapper.buildPrayerTimes(timings, "03-06-2024", "Europe/London")
        // Islamabad UTC+5, London UTC+1 in summer → Islamabad Fajr is 4h earlier in UTC
        assertTrue(islamabad[0].timestamp < london[0].timestamp)
    }

    // --- 5D.1: DST spring-forward (Europe/London, March 31 2024) ---

    @Test
    fun `DST spring-forward — March 31 London has BST offset UTC+1`() {
        // March 31 2024: clocks spring forward 01:00→02:00 GMT→BST
        // After the transition, all times are in BST (UTC+1)
        val timings = mapOf("Dhuhr" to "12:00")
        val london = AladhanTimingsMapper.buildPrayerTimes(timings, "31-03-2024", "Europe/London")
        val utc = AladhanTimingsMapper.buildPrayerTimes(timings, "31-03-2024", "UTC")
        // London BST 12:00 = UTC 11:00, UTC 12:00 = UTC 12:00
        // London epoch should be exactly 1h earlier (1h ahead means smaller epoch)
        assertEquals(3_600_000L, utc[0].timestamp - london[0].timestamp)
    }

    @Test
    fun `DST spring-forward — March 30 vs March 31 London offset differs`() {
        // March 30: still GMT (UTC+0).  March 31: BST (UTC+1).
        // Same wall-clock Dhuhr 12:00 — epoch should differ by 1h (23h apart in UTC)
        val timings = mapOf("Dhuhr" to "12:00")
        val mar30 = AladhanTimingsMapper.buildPrayerTimes(timings, "30-03-2024", "Europe/London")
        val mar31 = AladhanTimingsMapper.buildPrayerTimes(timings, "31-03-2024", "Europe/London")
        // mar30 at 12:00 GMT = UTC 12:00, mar31 at 12:00 BST = UTC 11:00
        // Calendar-day advance = 23h (because 1h was lost)
        val dayMs = 24 * 3_600_000L
        assertEquals(dayMs - 3_600_000L, mar31[0].timestamp - mar30[0].timestamp)
    }

    @Test
    fun `DST spring-forward — Fajr before transition vs Dhuhr after transition`() {
        // March 31 2024: transition at 01:00 GMT → 02:00 BST
        // Fajr at 00:30 is BEFORE transition (GMT=UTC+0)
        // Dhuhr at 12:00 is AFTER transition (BST=UTC+1)
        val timings = mapOf("Fajr" to "00:30", "Dhuhr" to "12:00")
        val london = AladhanTimingsMapper.buildPrayerTimes(timings, "31-03-2024", "Europe/London")
        // Wall-clock difference = 11h30m = 11.5h
        // UTC difference: Fajr=00:30 UTC, Dhuhr=11:00 UTC → 10h30m
        val wallClockDiff = (11 * 60 + 30) * 60_000L
        val utcDiff = london[1].timestamp - london[0].timestamp
        assertEquals(wallClockDiff - 3_600_000L, utcDiff) // 1h less due to DST jump
    }

    // --- 5D.2: DST fall-back (Europe/London, October 27 2024) ---

    @Test
    fun `DST fall-back — October 27 London has GMT offset UTC+0`() {
        // Oct 27 2024: clocks fall back 02:00→01:00 BST→GMT
        // After the transition, all times are in GMT (UTC+0)
        val timings = mapOf("Dhuhr" to "12:00")
        val london = AladhanTimingsMapper.buildPrayerTimes(timings, "27-10-2024", "Europe/London")
        val utc = AladhanTimingsMapper.buildPrayerTimes(timings, "27-10-2024", "UTC")
        // London GMT 12:00 = UTC 12:00, UTC 12:00 = UTC 12:00 — identical
        assertEquals(utc[0].timestamp, london[0].timestamp)
    }

    @Test
    fun `DST fall-back — October 26 vs October 27 London offset differs`() {
        // Oct 26: still BST (UTC+1).  Oct 27: GMT (UTC+0).
        // Same wall-clock Dhuhr 12:00 — epoch should differ by 25h in UTC (extra hour)
        val timings = mapOf("Dhuhr" to "12:00")
        val oct26 = AladhanTimingsMapper.buildPrayerTimes(timings, "26-10-2024", "Europe/London")
        val oct27 = AladhanTimingsMapper.buildPrayerTimes(timings, "27-10-2024", "Europe/London")
        // oct26 at 12:00 BST = UTC 11:00, oct27 at 12:00 GMT = UTC 12:00
        // Calendar-day advance = 25h (because 1h was gained)
        val dayMs = 24 * 3_600_000L
        assertEquals(dayMs + 3_600_000L, oct27[0].timestamp - oct26[0].timestamp)
    }

    @Test
    fun `DST fall-back — Dhuhr before transition vs Maghrib after transition`() {
        // Oct 27 2024: transition at 02:00 BST → 01:00 GMT
        // Dhuhr at 12:00 is AFTER transition (GMT=UTC+0)
        // Maghrib at 17:00 is also after transition
        // Both should be GMT — wall-clock diff = UTC diff = 5h
        val timings = mapOf("Dhuhr" to "12:00", "Maghrib" to "17:00")
        val london = AladhanTimingsMapper.buildPrayerTimes(timings, "27-10-2024", "Europe/London")
        val wallClockDiff = 5 * 3_600_000L
        val utcDiff = london[1].timestamp - london[0].timestamp
        assertEquals(wallClockDiff, utcDiff)
    }

    // --- 5D.3: Manual timezone change → times recalculate correctly ---

    @Test
    fun `timezone change — London GMT to Mecca AST produces correct offset`() {
        // Jan 15 2024: London=GMT (UTC+0), Mecca=AST (UTC+3)
        val timings = mapOf("Fajr" to "05:00", "Dhuhr" to "12:00")
        val london = AladhanTimingsMapper.buildPrayerTimes(timings, "15-01-2024", "Europe/London")
        val mecca = AladhanTimingsMapper.buildPrayerTimes(timings, "15-01-2024", "Asia/Riyadh")
        // Same wall-clock, different epochs: London=UTC+0, Mecca=UTC+3 → diff = 3h
        assertEquals(3 * 3_600_000L, london[0].timestamp - mecca[0].timestamp)
        assertEquals(3 * 3_600_000L, london[1].timestamp - mecca[1].timestamp)
    }

    @Test
    fun `timezone change — summer London BST to winter London GMT produces correct offset`() {
        // Jul 15: BST (UTC+1).  Jan 15: GMT (UTC+0). Same city, different season.
        val timings = mapOf("Dhuhr" to "12:00")
        val summer = AladhanTimingsMapper.buildPrayerTimes(timings, "15-07-2024", "Europe/London")
        val winter = AladhanTimingsMapper.buildPrayerTimes(timings, "15-01-2024", "Europe/London")

        // Verify each timestamp independently against expected epoch
        val expectedSummer =
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("Europe/London") }
                .parse("2024-07-15 12:00")!!.time
        assertEquals(expectedSummer, summer[0].timestamp)

        val expectedWinter =
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("Europe/London") }
                .parse("2024-01-15 12:00")!!.time
        assertEquals(expectedWinter, winter[0].timestamp)
    }
}
