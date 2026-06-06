package com.prayertime.domain.calculator

import com.prayertime.domain.model.Prayer
import com.prayertime.domain.model.PrayerTime
import com.prayertime.domain.model.PrayerTimesResult
import java.util.Calendar
import java.util.TimeZone

object PrayerTimeCalculator {
    /** Adds one calendar day in [timezone] — correct across DST transitions. */
    fun advanceOneCalendarDay(
        timestamp: Long,
        timezone: TimeZone,
    ): Long {
        val cal = Calendar.getInstance(timezone).apply { timeInMillis = timestamp }
        cal.add(Calendar.DAY_OF_MONTH, 1)
        return cal.timeInMillis
    }

    /** Milliseconds from [now] until the next [eventTimestamp] (today or tomorrow in [timezone]). */
    fun millisUntilNextOccurrence(
        eventTimestamp: Long,
        now: Long,
        timezone: TimeZone,
    ): Long =
        if (eventTimestamp > now) {
            eventTimestamp - now
        } else {
            maxOf(0L, advanceOneCalendarDay(eventTimestamp, timezone) - now)
        }

    /** Next prayer boundary: upcoming today, or first prayer tomorrow after the last one passes. */
    fun nextBoundaryTimestamp(
        times: List<PrayerTime>,
        timezone: TimeZone,
        now: Long = System.currentTimeMillis(),
    ): Long? =
        times.firstOrNull { it.timestamp > now }?.timestamp
            ?: times.firstOrNull()?.let { advanceOneCalendarDay(it.timestamp, timezone) }

    fun isSameDay(
        timestamp: Long,
        now: Long = System.currentTimeMillis(),
        timezone: TimeZone = TimeZone.getDefault(),
    ): Boolean {
        val cal = Calendar.getInstance(timezone).apply { timeInMillis = timestamp }
        val tsDay = cal[Calendar.DAY_OF_YEAR] to cal[Calendar.YEAR]
        val todayCal = Calendar.getInstance(timezone).apply { timeInMillis = now }
        val today = todayCal[Calendar.DAY_OF_YEAR] to todayCal[Calendar.YEAR]
        return tsDay == today
    }

    /** True when [now] is on a different calendar day than the day of [anchorTimestamp] in [timezone]. */
    fun needsPrayerDayRefresh(
        anchorTimestamp: Long,
        now: Long = System.currentTimeMillis(),
        timezone: TimeZone = TimeZone.getDefault(),
    ): Boolean = !isSameDay(anchorTimestamp, now, timezone)

    fun buildResult(
        times: List<PrayerTime>,
        now: Long = System.currentTimeMillis(),
        timezone: TimeZone = TimeZone.getDefault(),
    ): PrayerTimesResult {
        val next =
            resolveNext(times, now, timezone)
                ?: return PrayerTimesResult.Error(com.prayertime.domain.model.FetchError.UNKNOWN)
        return PrayerTimesResult.Success(times, next.prayer, next.countdown)
    }

    fun getNextPrayer(
        times: List<PrayerTime>,
        now: Long = System.currentTimeMillis(),
        timezone: TimeZone,
    ): Prayer? = resolveNext(times, now, timezone)?.prayer

    fun getCountdownToNext(
        times: List<PrayerTime>,
        now: Long = System.currentTimeMillis(),
        timezone: TimeZone = TimeZone.getDefault(),
    ): Long = resolveNext(times, now, timezone)?.countdown ?: 0L

    private data class NextSnapshot(
        val prayer: Prayer,
        val countdown: Long,
    )

    private fun resolveNext(
        times: List<PrayerTime>,
        now: Long,
        timezone: TimeZone,
    ): NextSnapshot? {
        if (times.isEmpty()) return null
        val upcoming = times.firstOrNull { it.timestamp > now }
        return if (upcoming != null) {
            NextSnapshot(upcoming.prayer, maxOf(0L, upcoming.timestamp - now))
        } else {
            val first = times.first()
            NextSnapshot(
                first.prayer,
                millisUntilNextOccurrence(first.timestamp, now, timezone),
            )
        }
    }
}
