package com.prayertime.domain.calculator

import com.prayertime.domain.model.HijriDate
import java.util.Date

/** Tabular 30-year Kuwaiti algorithm for Hijri years outside the Umm al-Qura table. */
internal object TabularHijriCalendar {
    private const val ISLAMIC_EPOCH_JDN = 1_948_440
    private const val CYCLE_DAYS = 10_631
    private val leapYears = setOf(2, 5, 7, 10, 13, 16, 18, 21, 24, 26, 29)

    fun gregorianToHijri(
        year: Int,
        month: Int,
        day: Int,
    ): HijriDate {
        val jdn = HijriGregorianDates.gregorianToJdn(year, month, day)
        val days = jdn - ISLAMIC_EPOCH_JDN

        val hijriYear = ((30L * days + 10646) / CYCLE_DAYS).toInt()
        val daysBeforeYear =
            ((hijriYear - 1).toLong() * 354 + (11L * hijriYear + 3) / 30).toInt()
        var dayOfYear = days - daysBeforeYear

        val lengths = monthLengths(hijriYear)
        for ((idx, len) in lengths.withIndex()) {
            if (dayOfYear < len) {
                return HijriDate(hijriYear, idx + 1, dayOfYear + 1)
            }
            dayOfYear -= len
        }
        return HijriDate(hijriYear, 12, lengths.last())
    }

    fun hijriToGregorian(hijriDate: HijriDate): Date {
        val daysBeforeYear =
            ((hijriDate.year - 1).toLong() * 354 + (11L * hijriDate.year + 3) / 30).toInt()
        var daysIntoYear = 0
        val lengths = monthLengths(hijriDate.year)
        for (m in 1 until hijriDate.month) {
            daysIntoYear += lengths[m - 1]
        }
        daysIntoYear += hijriDate.day - 1

        val jdn = ISLAMIC_EPOCH_JDN + daysBeforeYear + daysIntoYear
        return HijriGregorianDates.jdnToGregorian(jdn)
    }

    fun monthLengths(hijriYear: Int): List<Int> {
        val leap = Math.floorMod(hijriYear, 30) in leapYears
        return listOf(30, 29, 30, 29, 30, 29, 30, 29, 30, 29, 30, if (leap) 30 else 29)
    }
}
