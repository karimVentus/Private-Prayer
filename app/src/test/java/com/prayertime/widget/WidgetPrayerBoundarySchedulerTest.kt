package com.prayertime.widget

import com.prayertime.domain.calculator.PrayerTimeCalculator
import com.prayertime.domain.model.Prayer
import com.prayertime.domain.model.PrayerTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class WidgetPrayerBoundarySchedulerTest {
    @Test
    fun nextBoundaryTimestamp_returnsNextFuturePrayer() {
        val now = System.currentTimeMillis()
        val times =
            listOf(
                PrayerTime(Prayer.FAJR, "04:00", now - 3_600_000L),
                PrayerTime(Prayer.DHUHR, "12:30", now + 3_600_000L),
            )

        assertEquals(
            times[1].timestamp,
            WidgetPrayerBoundaryScheduler.nextBoundaryTimestamp(times, "UTC", now),
        )
    }

    @Test
    fun nextBoundaryTimestamp_wrapsToTomorrowAfterIsha() {
        val now = System.currentTimeMillis()
        val fajr = PrayerTime(Prayer.FAJR, "04:00", now - 86_400_000L)
        val isha = PrayerTime(Prayer.ISHA, "19:40", now - 3_600_000L)
        val times = listOf(fajr, isha)

        assertEquals(
            PrayerTimeCalculator.advanceOneCalendarDay(fajr.timestamp, TimeZone.getTimeZone("UTC")),
            WidgetPrayerBoundaryScheduler.nextBoundaryTimestamp(times, "UTC", now),
        )
    }

    @Test
    fun nextBoundaryTimestamp_usesCalendarDayOnLondonSpringForward() {
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

        val boundary =
            WidgetPrayerBoundaryScheduler.nextBoundaryTimestamp(times, "Europe/London", afterIsha)
        val expected = PrayerTimeCalculator.advanceOneCalendarDay(fajr, london)
        assertEquals(expected, boundary)
        assertNotEquals(fajr + 24 * 60 * 60 * 1000L, boundary)
    }

    @Test
    fun nextBoundaryTimestamp_returnsNullWhenTimesEmpty() {
        assertNull(WidgetPrayerBoundaryScheduler.nextBoundaryTimestamp(emptyList(), "UTC"))
    }

    @Test
    fun nextCountdownTickTimestamp_alignsToNextMinuteBoundary() {
        val now = 1_700_000_000_123L
        assertEquals(1_700_000_040_000L, WidgetCountdownRefreshScheduler.nextTickTimestamp(now))
    }

    @Test
    fun nextBoundaryTimestamp_returnsNullWhenTimezoneMissing() {
        val now = System.currentTimeMillis()
        val times = listOf(PrayerTime(Prayer.FAJR, "04:00", now + 3_600_000L))
        assertNull(WidgetPrayerBoundaryScheduler.nextBoundaryTimestamp(times, ""))
    }
}
