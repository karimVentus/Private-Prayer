package com.prayertime.domain.calculator

import com.prayertime.domain.model.HijriDate

/** Umm al-Qura conversions for years 1300–1600 AH (bundled month-start table). */
internal object UmmAlQuraCalendar {
    fun isEpochDayInRange(epochDay: Int): Boolean = epochDay in UmmAlQuraEpochMonths.MIN_EPOCH_DAY until UmmAlQuraEpochMonths.MAX_EPOCH_DAY

    fun isHijriYearInRange(hijriYear: Int): Boolean = hijriYear in UmmAlQuraEpochMonths.MIN_HIJRI_YEAR..UmmAlQuraEpochMonths.MAX_HIJRI_YEAR

    fun epochDayToHijri(epochDay: Int): HijriDate {
        val starts = UmmAlQuraEpochMonths.monthStartEpochDays
        var epochMonth = starts.binarySearch(epochDay)
        if (epochMonth < 0) {
            epochMonth = -epochMonth - 2
        }
        val hsem = UmmAlQuraEpochMonths.HIJRAH_START_EPOCH_MONTH
        val year = (epochMonth + hsem) / 12
        val month = (epochMonth + hsem) % 12 + 1
        val day = epochDay - starts[epochMonth] + 1
        return HijriDate(year, month, day)
    }

    fun hijriToEpochDay(hijriDate: HijriDate): Int {
        val epochMonth = yearToEpochMonth(hijriDate.year) + (hijriDate.month - 1)
        val starts = UmmAlQuraEpochMonths.monthStartEpochDays
        require(epochMonth in 0 until starts.lastIndex) {
            "Hijri date out of Umm al-Qura range: $hijriDate"
        }
        return starts[epochMonth] + (hijriDate.day - 1)
    }

    fun monthLength(
        hijriYear: Int,
        month: Int,
    ): Int {
        val epochMonth = yearToEpochMonth(hijriYear) + (month - 1)
        val starts = UmmAlQuraEpochMonths.monthStartEpochDays
        return starts[epochMonth + 1] - starts[epochMonth]
    }

    private fun yearToEpochMonth(hijriYear: Int): Int = hijriYear * 12 - UmmAlQuraEpochMonths.HIJRAH_START_EPOCH_MONTH
}
