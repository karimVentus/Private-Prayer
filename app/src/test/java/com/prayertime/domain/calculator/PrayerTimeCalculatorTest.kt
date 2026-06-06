package com.prayertime.domain.calculator

import com.prayertime.domain.model.Prayer
import com.prayertime.domain.model.PrayerTime
import com.prayertime.domain.model.PrayerTimesResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class PrayerTimeCalculatorTest {
    private val now = 1_750_000_000_000L
    private val hour = 3_600_000L
    private val dayMillis = 24 * 60 * 60 * 1000L
    private val utc = TimeZone.getTimeZone("UTC")

    private val sampleTimes =
        listOf(
            PrayerTime(Prayer.FAJR, "05:12", now - 5 * hour),
            PrayerTime(Prayer.SHURUQ, "06:30", now - 3 * hour),
            PrayerTime(Prayer.DHUHR, "12:30", now + 2 * hour),
            PrayerTime(Prayer.ASR, "15:45", now + 5 * hour),
            PrayerTime(Prayer.MAGHRIB, "18:30", now + 8 * hour),
            PrayerTime(Prayer.ISHA, "19:45", now + 9 * hour),
        )

    @Test
    fun `getNextPrayer returns correct prayer when current time is between prayers`() {
        val next = PrayerTimeCalculator.getNextPrayer(sampleTimes, now, utc)
        assertEquals(Prayer.DHUHR, next)
    }

    @Test
    fun `getNextPrayer returns first prayer when all times are in the past (wrap to next day)`() {
        val pastNow = now + 20 * hour
        val next = PrayerTimeCalculator.getNextPrayer(sampleTimes, pastNow, utc)
        assertEquals(Prayer.FAJR, next)
    }

    @Test
    fun `getNextPrayer returns null for empty list`() {
        val next = PrayerTimeCalculator.getNextPrayer(emptyList(), now, utc)
        assertEquals(null, next)
    }

    @Test
    fun `countdown returns correct remaining time`() {
        val countdown = PrayerTimeCalculator.getCountdownToNext(sampleTimes, now, utc)
        assertEquals(2 * hour, countdown)
    }

    @Test
    fun `countdown wraps to tomorrow first prayer when all times are past`() {
        // 1 hour after Isha (now+9h) — all times are past
        val pastNow = now + 10 * hour
        // Tomorrow's Fajr = original Fajr (now-5h) + 24h = now+19h
        // Countdown = (now+19h) - (now+10h) = 9h
        val countdown = PrayerTimeCalculator.getCountdownToNext(sampleTimes, pastNow, utc)
        assertEquals(9 * hour, countdown)
    }

    @Test
    fun `buildResult returns success with correct next prayer`() {
        val result = PrayerTimeCalculator.buildResult(sampleTimes, now, utc)
        assertTrue(result is PrayerTimesResult.Success)
        val success = result as PrayerTimesResult.Success
        assertEquals(Prayer.DHUHR, success.nextPrayer)
        assertEquals(2 * hour, success.countdown)
        assertEquals(6, success.times.size)
    }

    @Test
    fun `buildResult returns error for empty list`() {
        val result = PrayerTimeCalculator.buildResult(emptyList(), now)
        assertTrue(result is PrayerTimesResult.Error)
    }

    @Test
    fun `getNextPrayer at 13_00 when Asr is at 13_08 returns ASR`() {
        val asr1308 = 13 * 3600 + 8 * 60
        val times =
            listOf(
                PrayerTime(Prayer.FAJR, "05:12", now - 8 * hour),
                PrayerTime(Prayer.DHUHR, "12:30", now - hour),
                PrayerTime(Prayer.ASR, "15:45", now + asr1308.toLong() * 1000),
                PrayerTime(Prayer.MAGHRIB, "18:30", now + 5 * hour + 30 * 60 * 1000L),
                PrayerTime(Prayer.ISHA, "19:45", now + 6 * hour + 45 * 60 * 1000L),
            )
        val next = PrayerTimeCalculator.getNextPrayer(times, now, utc)
        assertEquals(Prayer.ASR, next)
    }

    @Test
    fun `getNextPrayer returns SHURUQ when current time is after FAJR but before SHURUQ`() {
        val afterFajr = now - 4 * hour
        val next = PrayerTimeCalculator.getNextPrayer(sampleTimes, afterFajr, utc)
        assertEquals(Prayer.SHURUQ, next)
    }

    @Test
    fun `SHURUQ countdown is correct when current time is before SHURUQ`() {
        val afterFajr = now - 4 * hour
        val countdown = PrayerTimeCalculator.getCountdownToNext(sampleTimes, afterFajr, utc)
        assertEquals(hour, countdown)
    }

    @Test
    fun `countdown at exact prayer time is non-negative`() {
        val exactAsr =
            listOf(
                PrayerTime(Prayer.FAJR, "05:12", now - 10 * hour),
                PrayerTime(Prayer.ASR, "15:45", now),
            )
        val countdown = PrayerTimeCalculator.getCountdownToNext(exactAsr, now, utc)
        assertTrue(countdown >= 0L)
    }

    // --- 2E.1 / audit 3.4: countdown at exact 00:00:00 city midnight ---

    @Test
    fun `countdown tick sequence crosses city midnight and triggers day refresh at 00_00_00`() {
        val berlin = TimeZone.getTimeZone("Europe/Berlin")
        val cal =
            Calendar.getInstance(berlin).apply {
                set(2026, Calendar.JUNE, 3, 5, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
        val fajr = cal.timeInMillis
        cal.set(2026, Calendar.JUNE, 3, 6, 30, 0)
        val shuruq = cal.timeInMillis
        cal.set(2026, Calendar.JUNE, 3, 12, 30, 0)
        val dhuhr = cal.timeInMillis
        cal.set(2026, Calendar.JUNE, 3, 15, 45, 0)
        val asr = cal.timeInMillis
        cal.set(2026, Calendar.JUNE, 3, 18, 30, 0)
        val maghrib = cal.timeInMillis
        cal.set(2026, Calendar.JUNE, 3, 22, 0, 0)
        val isha = cal.timeInMillis
        val times =
            listOf(
                PrayerTime(Prayer.FAJR, "05:00", fajr),
                PrayerTime(Prayer.SHURUQ, "06:30", shuruq),
                PrayerTime(Prayer.DHUHR, "12:30", dhuhr),
                PrayerTime(Prayer.ASR, "15:45", asr),
                PrayerTime(Prayer.MAGHRIB, "18:30", maghrib),
                PrayerTime(Prayer.ISHA, "22:00", isha),
            )

        cal.set(2026, Calendar.JUNE, 3, 23, 59, 59)
        cal.set(Calendar.MILLISECOND, 0)
        val oneSecondBeforeMidnight = cal.timeInMillis
        assertFalse(
            PrayerTimeCalculator.needsPrayerDayRefresh(fajr, oneSecondBeforeMidnight, berlin),
        )
        assertEquals(Prayer.FAJR, PrayerTimeCalculator.getNextPrayer(times, oneSecondBeforeMidnight, berlin))
        val countdownBefore =
            PrayerTimeCalculator.getCountdownToNext(times, oneSecondBeforeMidnight, berlin)
        val tomorrowFajr = PrayerTimeCalculator.advanceOneCalendarDay(fajr, berlin)
        assertEquals(tomorrowFajr - oneSecondBeforeMidnight, countdownBefore)
        assertTrue(countdownBefore > 0L)

        cal.set(2026, Calendar.JUNE, 4, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val atMidnight = cal.timeInMillis
        assertTrue(
            PrayerTimeCalculator.needsPrayerDayRefresh(fajr, atMidnight, berlin),
        )
        val countdownAtMidnight = PrayerTimeCalculator.getCountdownToNext(times, atMidnight, berlin)
        assertEquals(countdownBefore - 1_000L, countdownAtMidnight)
        assertTrue(countdownAtMidnight > 0L)
    }

    @Test
    fun `countdown is one second before next prayer then advances after prayer instant`() {
        val dhuhrTs = now + 2 * hour
        val asrTs = now + 5 * hour
        val times =
            listOf(
                PrayerTime(Prayer.FAJR, "05:12", now - 8 * hour),
                PrayerTime(Prayer.DHUHR, "12:30", dhuhrTs),
                PrayerTime(Prayer.ASR, "15:45", asrTs),
            )
        assertEquals(Prayer.DHUHR, PrayerTimeCalculator.getNextPrayer(times, dhuhrTs - 1_000L, utc))
        assertEquals(1_000L, PrayerTimeCalculator.getCountdownToNext(times, dhuhrTs - 1_000L, utc))
        assertEquals(Prayer.ASR, PrayerTimeCalculator.getNextPrayer(times, dhuhrTs, utc))
        assertEquals(asrTs - dhuhrTs, PrayerTimeCalculator.getCountdownToNext(times, dhuhrTs, utc))
        assertEquals(Prayer.ASR, PrayerTimeCalculator.getNextPrayer(times, dhuhrTs + 1_000L, utc))
        assertEquals(asrTs - dhuhrTs - 1_000L, PrayerTimeCalculator.getCountdownToNext(times, dhuhrTs + 1_000L, utc))
    }

    // --- Midnight boundary (2E.1) — drives onRefreshNeeded via needsPrayerDayRefresh ---

    @Test
    fun `needsPrayerDayRefresh true at city midnight boundary`() {
        val berlin = TimeZone.getTimeZone("Europe/Berlin")
        val cal =
            Calendar.getInstance(berlin).apply {
                set(2026, Calendar.JUNE, 3, 23, 59, 59)
                set(Calendar.MILLISECOND, 0)
            }
        val beforeMidnight = cal.timeInMillis
        cal.set(2026, Calendar.JUNE, 4, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val atMidnight = cal.timeInMillis

        assertTrue(
            PrayerTimeCalculator.needsPrayerDayRefresh(beforeMidnight, atMidnight, berlin),
        )
    }

    @Test
    fun `needsPrayerDayRefresh false while still same city day`() {
        val berlin = TimeZone.getTimeZone("Europe/Berlin")
        val cal =
            Calendar.getInstance(berlin).apply {
                set(2026, Calendar.JUNE, 3, 12, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
        val noon = cal.timeInMillis
        cal.set(2026, Calendar.JUNE, 3, 23, 0, 0)
        val evening = cal.timeInMillis

        assertFalse(PrayerTimeCalculator.needsPrayerDayRefresh(noon, evening, berlin))
    }

    @Test
    fun `isSameDay returns true for same timestamp`() {
        val cal =
            Calendar.getInstance().apply {
                set(2026, Calendar.JUNE, 3, 12, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
        val noon = cal.timeInMillis
        assertTrue(PrayerTimeCalculator.isSameDay(noon, noon))
    }

    @Test
    fun `isSameDay returns false across local-timezone midnight`() {
        val cal =
            Calendar.getInstance().apply {
                set(2026, Calendar.JUNE, 3, 23, 59, 59)
                set(Calendar.MILLISECOND, 0)
            }
        val beforeMidnight = cal.timeInMillis
        cal.set(2026, Calendar.JUNE, 4, 0, 0, 1)
        cal.set(Calendar.MILLISECOND, 0)
        val afterMidnight = cal.timeInMillis
        assertFalse(PrayerTimeCalculator.isSameDay(beforeMidnight, afterMidnight))
    }

    @Test
    fun `isSameDay returns true for times on same local day`() {
        val cal =
            Calendar.getInstance().apply {
                set(2026, Calendar.JUNE, 3, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
        val startOfDay = cal.timeInMillis
        cal.set(2026, Calendar.JUNE, 3, 23, 59, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endOfDay = cal.timeInMillis
        assertTrue(PrayerTimeCalculator.isSameDay(startOfDay, endOfDay))
    }

    @Test
    fun `isSameDay returns false across local-timezone year boundary`() {
        val cal =
            Calendar.getInstance().apply {
                set(2025, Calendar.DECEMBER, 31, 23, 59, 59)
                set(Calendar.MILLISECOND, 0)
            }
        val oldYear = cal.timeInMillis
        cal.set(2026, Calendar.JANUARY, 1, 0, 0, 1)
        cal.set(Calendar.MILLISECOND, 0)
        val newYear = cal.timeInMillis
        assertFalse(PrayerTimeCalculator.isSameDay(oldYear, newYear))
    }

    // --- 2E.2: Device TZ ≠ city TZ ---

    @Test
    fun `needsPrayerDayRefresh uses city TZ not device default`() {
        val london = TimeZone.getTimeZone("Europe/London")
        val utc = TimeZone.getTimeZone("UTC")
        val cal =
            Calendar.getInstance(london).apply {
                set(2024, Calendar.MARCH, 31, 23, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
        val anchor = cal.timeInMillis
        cal.set(2024, Calendar.APRIL, 1, 0, 30, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val afterCityMidnight = cal.timeInMillis

        assertTrue(
            PrayerTimeCalculator.needsPrayerDayRefresh(anchor, afterCityMidnight, london),
        )
        assertFalse(
            PrayerTimeCalculator.needsPrayerDayRefresh(anchor, afterCityMidnight, utc),
        )
    }

    @Test
    fun `isSameDay cross-timezone same pair spans midnight in UTC but not Auckland`() {
        val cal =
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(2026, Calendar.JUNE, 3, 22, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
        val t1 = cal.timeInMillis // June 3 22:00 UTC
        cal.set(2026, Calendar.JUNE, 4, 2, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val t2 = cal.timeInMillis // June 4 02:00 UTC

        val utc = TimeZone.getTimeZone("UTC")
        val auckland = TimeZone.getTimeZone("Pacific/Auckland")

        // In UTC: June 3 22:00 vs June 4 02:00 → different day
        assertFalse(PrayerTimeCalculator.isSameDay(t1, t2, utc))
        // In Auckland (UTC+12): June 4 10:00 vs June 4 14:00 → same day
        assertTrue(PrayerTimeCalculator.isSameDay(t1, t2, auckland))
    }

    @Test
    fun `isSameDay matches manual Calendar computation in any timezone`() {
        val tokyo = TimeZone.getTimeZone("Asia/Tokyo")
        val newYork = TimeZone.getTimeZone("America/New_York")
        val london = TimeZone.getTimeZone("Europe/London")

        val utcCal =
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(2026, Calendar.JUNE, 3, 22, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
        val t1 = utcCal.timeInMillis
        utcCal.set(2026, Calendar.JUNE, 4, 2, 0, 0)
        utcCal.set(Calendar.MILLISECOND, 0)
        val t2 = utcCal.timeInMillis

        for (tz in listOf(tokyo, newYork, london, TimeZone.getTimeZone("UTC"))) {
            val expected =
                Calendar.getInstance(tz).apply { timeInMillis = t1 }
                    .let { c1 ->
                        val c2 = Calendar.getInstance(tz).apply { timeInMillis = t2 }
                        c1[Calendar.DAY_OF_YEAR] == c2[Calendar.DAY_OF_YEAR] &&
                            c1[Calendar.YEAR] == c2[Calendar.YEAR]
                    }
            val actual = PrayerTimeCalculator.isSameDay(t1, t2, tz)
            assertEquals("Mismatch for ${tz.id}", expected, actual)
        }
    }

    // --- 2E.3: DST transition day (Europe/London, March 31st) ---

    @Test
    fun `isSameDay works on DST spring-forward day in London`() {
        val london = TimeZone.getTimeZone("Europe/London")
        val cal =
            Calendar.getInstance(london).apply {
                set(2024, Calendar.MARCH, 31, 0, 30, 0)
                set(Calendar.MILLISECOND, 0)
            }
        val earlyMorning = cal.timeInMillis
        cal.set(2024, Calendar.MARCH, 31, 23, 30, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val lateEvening = cal.timeInMillis

        assertTrue(
            PrayerTimeCalculator.isSameDay(earlyMorning, lateEvening, london),
        )
    }

    @Test
    fun `isSameDay before and after DST gap still same day in London`() {
        val london = TimeZone.getTimeZone("Europe/London")
        val cal =
            Calendar.getInstance(london).apply {
                set(2024, Calendar.MARCH, 31, 0, 59, 59)
                set(Calendar.MILLISECOND, 0)
            }
        val beforeGap = cal.timeInMillis
        cal.set(2024, Calendar.MARCH, 31, 3, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val afterGap = cal.timeInMillis

        assertTrue(
            PrayerTimeCalculator.isSameDay(beforeGap, afterGap, london),
        )
    }

    @Test
    fun `isSameDay correctly separates day before and day of DST in London`() {
        val london = TimeZone.getTimeZone("Europe/London")
        val cal =
            Calendar.getInstance(london).apply {
                set(2024, Calendar.MARCH, 30, 23, 30, 0)
                set(Calendar.MILLISECOND, 0)
            }
        val dayBefore = cal.timeInMillis
        cal.set(2024, Calendar.MARCH, 31, 0, 30, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val dstDay = cal.timeInMillis

        assertFalse(
            PrayerTimeCalculator.isSameDay(dayBefore, dstDay, london),
        )
    }

    @Test
    fun `needsPrayerDayRefresh true across London DST day midnight`() {
        val london = TimeZone.getTimeZone("Europe/London")
        val cal =
            Calendar.getInstance(london).apply {
                set(2024, Calendar.MARCH, 31, 23, 30, 0)
                set(Calendar.MILLISECOND, 0)
            }
        val beforeMidnight = cal.timeInMillis
        cal.set(2024, Calendar.APRIL, 1, 0, 30, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val afterMidnight = cal.timeInMillis

        assertTrue(
            PrayerTimeCalculator.needsPrayerDayRefresh(beforeMidnight, afterMidnight, london),
        )
    }

    @Test
    fun `getCountdownToNext works over DST spring-forward boundary`() {
        val london = TimeZone.getTimeZone("Europe/London")
        val cal =
            Calendar.getInstance(london).apply {
                set(2024, Calendar.MARCH, 31, 1, 30, 0)
                set(Calendar.MILLISECOND, 0)
            }
        val beforeDstGap = cal.timeInMillis
        cal.set(2024, Calendar.MARCH, 31, 3, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val afterDstGap = cal.timeInMillis

        val times =
            listOf(
                PrayerTime(Prayer.FAJR, "03:00", afterDstGap),
            )

        val countdown = PrayerTimeCalculator.getCountdownToNext(times, beforeDstGap, london)
        assertTrue("Countdown must be positive across DST gap: $countdown", countdown > 0L)
        assertEquals(afterDstGap - beforeDstGap, countdown)
    }

    @Test
    fun `getCountdownToNext wrap uses calendar day not fixed millis on London spring-forward`() {
        val london = TimeZone.getTimeZone("Europe/London")
        val cal =
            Calendar.getInstance(london).apply {
                set(2024, Calendar.MARCH, 30, 5, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
        val fajr = cal.timeInMillis
        cal.set(2024, Calendar.MARCH, 30, 22, 0, 0)
        val isha = cal.timeInMillis
        cal.set(2024, Calendar.MARCH, 30, 23, 0, 0)
        val afterIsha = cal.timeInMillis
        val times =
            listOf(
                PrayerTime(Prayer.FAJR, "05:00", fajr),
                PrayerTime(Prayer.ISHA, "22:00", isha),
            )

        val expected = PrayerTimeCalculator.advanceOneCalendarDay(fajr, london) - afterIsha
        val countdown = PrayerTimeCalculator.getCountdownToNext(times, afterIsha, london)
        assertEquals(expected, countdown)
        assertTrue(countdown != fajr + dayMillis - afterIsha)
    }

    @Test
    fun `dateLabelDaysAgo subtracts calendar days not fixed millis on London spring-forward week`() {
        val london = TimeZone.getTimeZone("Europe/London")
        val cal =
            Calendar.getInstance(london).apply {
                set(2024, Calendar.MARCH, 31, 12, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
        val now = cal.timeInMillis
        val calendarCutoff = PrayerDayLabels.daysAgo(7, london, now)
        cal.add(Calendar.DAY_OF_MONTH, -7)
        val expected =
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = london }.format(cal.time)
        assertEquals(expected, calendarCutoff)
    }

    // --- audit 3.4: buildResult countdown on London DST day via full prayer schedule ---

    @Test
    fun `buildResult countdown on London DST day matches next prayer after spring-forward gap`() {
        val london = TimeZone.getTimeZone("Europe/London")
        val cal =
            Calendar.getInstance(london).apply {
                set(2024, Calendar.MARCH, 31, 12, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
        val times =
            LocalPrayerTimeCalculator.calculate(
                latitude = 51.5074,
                longitude = -0.1278,
                timezone = "Europe/London",
                date = cal.time,
            )
        val timestamps = times.map { it.timestamp }
        for (i in 1 until timestamps.size) {
            assertTrue("Prayer timestamps out of order on DST day", timestamps[i] > timestamps[i - 1])
        }

        cal.set(2024, Calendar.MARCH, 31, 1, 30, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val beforeGap = cal.timeInMillis
        val result = PrayerTimeCalculator.buildResult(times, beforeGap, london)
        assertTrue(result is PrayerTimesResult.Success)
        val success = result as PrayerTimesResult.Success
        val nextTimestamp = times.first { it.timestamp > beforeGap }.timestamp
        assertEquals(nextTimestamp - beforeGap, success.countdown)
        assertEquals(times.first { it.timestamp > beforeGap }.prayer, success.nextPrayer)
    }

    // --- 5D: DST transitions & timezone changes on advanceOneCalendarDay ---

    // 5D.1 — DST spring-forward (March 31 2024): day is 23h long
    @Test
    fun `advanceOneCalendarDay across London spring-forward produces 23h day`() {
        val london = TimeZone.getTimeZone("Europe/London")
        val cal =
            Calendar.getInstance(london).apply {
                set(2024, Calendar.MARCH, 30, 12, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
        val mar30Noon = cal.timeInMillis

        val nextDay = PrayerTimeCalculator.advanceOneCalendarDay(mar30Noon, london)
        // Mar 30 12:00 GMT → Mar 31 12:00 BST = 23h later in UTC
        val expectedDiff = 23 * 3_600_000L
        assertEquals(expectedDiff, nextDay - mar30Noon)
    }

    // 5D.2 — DST fall-back (October 27 2024): day is 25h long
    @Test
    fun `advanceOneCalendarDay across London fall-back produces 25h day`() {
        val london = TimeZone.getTimeZone("Europe/London")
        val cal =
            Calendar.getInstance(london).apply {
                set(2024, Calendar.OCTOBER, 26, 12, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
        val oct26Noon = cal.timeInMillis

        val nextDay = PrayerTimeCalculator.advanceOneCalendarDay(oct26Noon, london)
        // Oct 26 12:00 BST → Oct 27 12:00 GMT = 25h later in UTC
        val expectedDiff = 25 * 3_600_000L
        assertEquals(expectedDiff, nextDay - oct26Noon)
    }

    // 5D.3 — Manual timezone change: advanceOneCalendarDay respects new TZ
    @Test
    fun `advanceOneCalendarDay preserves wall-clock time in each timezone`() {
        val london = TimeZone.getTimeZone("Europe/London")
        val mecca = TimeZone.getTimeZone("Asia/Riyadh")

        // 12:00 noon on Jan 15 in London (GMT = UTC+0)
        val cal =
            Calendar.getInstance(london).apply {
                set(2024, Calendar.JANUARY, 15, 12, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
        val londonNoon = cal.timeInMillis

        val nextLondon = PrayerTimeCalculator.advanceOneCalendarDay(londonNoon, london)
        val nextMecca = PrayerTimeCalculator.advanceOneCalendarDay(londonNoon, mecca)

        // Both advance 1 calendar day, but since the starting instant is noon London (GMT),
        // adding a day in London gives Jan 16 12:00 GMT (+24h)
        assertEquals(24 * 3_600_000L, nextLondon - londonNoon)

        // Adding a day in Mecca (UTC+3): 12:00 GMT = 15:00 Mecca on Jan 15.
        //   +1 calendar day = 15:00 Mecca on Jan 16 = Jan 16 12:00 UTC (+24h, same epoch)
        assertEquals(24 * 3_600_000L, nextMecca - londonNoon)

        // But the wall-clock representations differ
        val lonCal = Calendar.getInstance(london).apply { timeInMillis = nextLondon }
        assertEquals(12, lonCal[Calendar.HOUR_OF_DAY])
        val mecCal = Calendar.getInstance(mecca).apply { timeInMillis = nextMecca }
        assertEquals(15, mecCal[Calendar.HOUR_OF_DAY])
    }

    @Test
    fun `advanceOneCalendarDay preserves wall-clock hour across DST`() {
        val london = TimeZone.getTimeZone("Europe/London")

        // Pre-DST: Mar 30 05:00 GMT → Mar 31 05:00 BST
        val cal =
            Calendar.getInstance(london).apply {
                set(2024, Calendar.MARCH, 30, 5, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
        val mar30Fajr = cal.timeInMillis
        val mar31Fajr = PrayerTimeCalculator.advanceOneCalendarDay(mar30Fajr, london)
        val mar31Cal = Calendar.getInstance(london).apply { timeInMillis = mar31Fajr }
        assertEquals(5, mar31Cal[Calendar.HOUR_OF_DAY])
        assertEquals(0, mar31Cal[Calendar.MINUTE])
        assertEquals(23 * 3_600_000L, mar31Fajr - mar30Fajr)

        // Post-DST fall-back: Oct 26 05:00 BST → Oct 27 05:00 GMT
        cal.set(2024, Calendar.OCTOBER, 26, 5, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val oct26Fajr = cal.timeInMillis
        val oct27Fajr = PrayerTimeCalculator.advanceOneCalendarDay(oct26Fajr, london)
        val oct27Cal = Calendar.getInstance(london).apply { timeInMillis = oct27Fajr }
        assertEquals(5, oct27Cal[Calendar.HOUR_OF_DAY])
        assertEquals(0, oct27Cal[Calendar.MINUTE])
        assertEquals(25 * 3_600_000L, oct27Fajr - oct26Fajr)
    }
}
