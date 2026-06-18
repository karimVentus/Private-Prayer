package com.prayertime.domain.calculator

import com.prayertime.domain.model.HijriDate
import com.prayertime.domain.model.IslamicEvent
import com.prayertime.domain.model.UpcomingEvent
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/**
 * Umm al-Qura Hijri calendar (Saudi official) for years 1300–1600 AH via a bundled
 * month-start table (OpenJDK `hijrah-config-islamic-umalqura`). Outside that range,
 * falls back to the tabular 30-year Kuwaiti algorithm.
 */
object HijriCalculator {
    fun gregorianToHijri(
        year: Int,
        month: Int,
        day: Int,
    ): HijriDate {
        val epochDay = HijriGregorianDates.gregorianToEpochDay(year, month, day)
        if (UmmAlQuraCalendar.isEpochDayInRange(epochDay)) {
            return UmmAlQuraCalendar.epochDayToHijri(epochDay)
        }
        return TabularHijriCalendar.gregorianToHijri(year, month, day)
    }

    fun hijriToGregorian(hijriDate: HijriDate): Date {
        if (UmmAlQuraCalendar.isHijriYearInRange(hijriDate.year)) {
            val epochDay = UmmAlQuraCalendar.hijriToEpochDay(hijriDate)
            return HijriGregorianDates.jdnToGregorian(epochDay + HijriGregorianDates.EPOCH_DAY_JDN_OFFSET)
        }
        return TabularHijriCalendar.hijriToGregorian(hijriDate)
    }

    /** Today's Hijri date in the civil calendar of [timezone] (e.g. city TZ). */
    fun todayInTimezone(timezone: String): HijriDate {
        val cal = Calendar.getInstance(TimeZone.getTimeZone(timezone))
        return gregorianToHijri(
            cal[Calendar.YEAR],
            cal[Calendar.MONTH] + 1,
            cal[Calendar.DAY_OF_MONTH],
        )
    }

    /** True when Dhul Hijjah has 30 days in [hijriYear]. */
    fun isLeapYear(hijriYear: Int): Boolean = daysInMonth(hijriYear, 12) == 30

    fun daysInMonth(
        hijriYear: Int,
        month: Int,
    ): Int {
        require(month in 1..12) { "month must be 1..12, got $month" }
        if (UmmAlQuraCalendar.isHijriYearInRange(hijriYear)) {
            return UmmAlQuraCalendar.monthLength(hijriYear, month)
        }
        return TabularHijriCalendar.monthLengths(hijriYear)[month - 1]
    }

    fun nextUpcomingEvent(
        today: Date,
        timezone: TimeZone = TimeZone.getTimeZone("UTC"),
    ): UpcomingEvent? {
        val cal = Calendar.getInstance(timezone).apply { time = today }
        val todayHijri =
            gregorianToHijri(
                cal[Calendar.YEAR],
                cal[Calendar.MONTH] + 1,
                cal[Calendar.DAY_OF_MONTH],
            )
        return nextUpcomingEvent(todayHijri)
    }

    fun nextUpcomingEvent(todayHijri: HijriDate): UpcomingEvent? =
        selectBestUpcomingEvent(
            todayHijri = todayHijri,
            ordinalInYear = ::ordinalInYear,
        )

    private fun ordinalInYear(date: HijriDate): Int {
        var ordinal = 0
        for (m in 1 until date.month) {
            ordinal += daysInMonth(date.year, m)
        }
        ordinal += date.day - 1
        return ordinal
    }
}

private fun selectBestUpcomingEvent(
    todayHijri: HijriDate,
    ordinalInYear: (HijriDate) -> Int,
): UpcomingEvent? {
    val todayYear = todayHijri.year
    val todayOrdinal = ordinalInYear(todayHijri)
    return IslamicEvent.entries
        .flatMap { event ->
            (0..1).mapNotNull { yearOffset ->
                rankedUpcomingCandidate(
                    event = event,
                    yearOffset = yearOffset,
                    todayYear = todayYear,
                    todayOrdinal = todayOrdinal,
                    ordinalInYear = ordinalInYear,
                )
            }
        }
        .minByOrNull { it.ordinal }
        ?.event
}

private data class RankedUpcomingCandidate(
    val event: UpcomingEvent,
    val ordinal: Int,
)

private fun rankedUpcomingCandidate(
    event: IslamicEvent,
    yearOffset: Int,
    todayYear: Int,
    todayOrdinal: Int,
    ordinalInYear: (HijriDate) -> Int,
): RankedUpcomingCandidate? {
    val eventYear = todayYear + yearOffset
    val eventDate = HijriDate(eventYear, event.month, event.day)
    val eventOrdinal = ordinalInYear(eventDate)
    val todayYearLength = (1..12).sumOf { month -> HijriCalculator.daysInMonth(todayYear, month) }
    val adjustedOrdinal =
        if (yearOffset == 0) {
            eventOrdinal
        } else {
            eventOrdinal + todayYearLength
        }
    if (adjustedOrdinal <= todayOrdinal) return null
    return RankedUpcomingCandidate(
        event = UpcomingEvent(event, eventDate, adjustedOrdinal - todayOrdinal),
        ordinal = adjustedOrdinal,
    )
}
