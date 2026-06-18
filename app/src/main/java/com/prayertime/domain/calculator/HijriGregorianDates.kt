package com.prayertime.domain.calculator

import java.util.Date

/** Gregorian ↔ JDN helpers shared by Umm al-Qura and tabular Hijri calendars. */
internal object HijriGregorianDates {
    /** JDN of 1970-01-01; matches [java.time.LocalDate.toEpochDay]. */
    const val EPOCH_DAY_JDN_OFFSET = 2_440_588

    fun gregorianToEpochDay(
        year: Int,
        month: Int,
        day: Int,
    ): Int = gregorianToJdn(year, month, day) - EPOCH_DAY_JDN_OFFSET

    fun gregorianToJdn(
        year: Int,
        month: Int,
        day: Int,
    ): Int {
        var y = year
        var m = month
        if (m <= 2) {
            y--
            m += 12
        }
        val a = y / 100
        val b = 2 - a + a / 4
        return (36525 * (y + 4716)) / 100 + (306001 * (m + 1)) / 10000 + day + b - 1524
    }

    fun jdnToGregorian(jdn: Int): Date {
        val millis = (jdn - EPOCH_DAY_JDN_OFFSET).toLong() * 86_400_000L + 43_200_000L
        return Date(millis)
    }
}
