package com.prayertime.domain.calculator

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** City-local calendar date labels for Room cache cleanup and engine date keys. */
internal object PrayerDayLabels {
    fun format(
        timestamp: Long,
        timezone: TimeZone,
    ): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = timezone }.format(Date(timestamp))

    fun daysAgo(
        days: Int,
        timezone: TimeZone,
        now: Long = System.currentTimeMillis(),
    ): String {
        val cal = Calendar.getInstance(timezone).apply { timeInMillis = now }
        cal.add(Calendar.DAY_OF_MONTH, -days)
        return format(cal.timeInMillis, timezone)
    }
}
