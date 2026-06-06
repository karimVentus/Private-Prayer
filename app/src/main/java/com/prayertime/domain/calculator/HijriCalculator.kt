package com.prayertime.domain.calculator

import com.prayertime.domain.model.HijriDate
import com.prayertime.domain.model.IslamicEvent
import com.prayertime.domain.model.UpcomingEvent
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/**
 * Tabular Islamic calendar (Kuwaiti algorithm) — arithmetic approximation of Umm al-Qura.
 *
 * 30-year cycle with 11 leap years (2, 5, 7, 10, 13, 16, 18, 21, 24, 26, 29).
 * Regular year = 354 days, leap year = 355 days (Dhul Hijjah gains one day).
 * Epoch: 1 Muharram 1 AH = July 19, 622 CE (Gregorian proleptic).
 */
object HijriCalculator {
    /** JDN of the Islamic epoch: Gregorian July 19, 622 CE. */
    private const val ISLAMIC_EPOCH_JDN = 1_948_440

    /** Days in a 30-year Islamic cycle. */
    private const val CYCLE_DAYS = 10_631

    // Leap-year residue classes mod 30 (0 = year 30 of cycle, non-leap).
    private val leapYears = setOf(2, 5, 7, 10, 13, 16, 18, 21, 24, 26, 29)

    // -- Public API --

    fun gregorianToHijri(
        year: Int,
        month: Int,
        day: Int,
    ): HijriDate {
        val jdn = gregorianToJdn(year, month, day)
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
        // Should not reach; fallback to last day of year
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
        daysIntoYear += hijriDate.day - 1 // day is 1-indexed

        val jdn = ISLAMIC_EPOCH_JDN + daysBeforeYear + daysIntoYear
        return jdnToGregorian(jdn)
    }

    /** True when [hijriYear] is a leap year in the tabular 30-year cycle. */
    fun isLeapYear(hijriYear: Int): Boolean = Math.floorMod(hijriYear, 30) in leapYears

    /** Days in [month] (1–12) for [hijriYear] — same rules as [monthLengths]. */
    fun daysInMonth(
        hijriYear: Int,
        month: Int,
    ): Int {
        require(month in 1..12) { "month must be 1..12, got $month" }
        return monthLengths(hijriYear)[month - 1]
    }

    /**
     * Returns the next upcoming Islamic event on or after [today], or null if
     * none can be computed.
     */
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
            isLeapYear = ::isLeapYear,
        )

    // -- Internal --

    /**
     * Day-of-year ordinal (0-indexed) for ordering events within/across years.
     * Used by [nextUpcomingEvent] to compare dates on a linear timeline.
     */
    private fun ordinalInYear(date: HijriDate): Int {
        val lengths = monthLengths(date.year)
        var ordinal = 0
        for (m in 1 until date.month) {
            ordinal += lengths[m - 1]
        }
        ordinal += date.day - 1
        return ordinal
    }

    private fun monthLengths(hijriYear: Int): List<Int> {
        val leap = isLeapYear(hijriYear)
        return listOf(30, 29, 30, 29, 30, 29, 30, 29, 30, 29, 30, if (leap) 30 else 29)
    }

    /** Gregorian (year, month, day) → Julian Day Number at noon UTC (Fliegel & Van Flandern). */
    private fun gregorianToJdn(
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

    /** JDN -> Gregorian Date. JDN integer boundary is at noon UTC, so we add 12 h to map to correct noon. */
    private fun jdnToGregorian(jdn: Int): Date {
        // JDN 2_440_588 = 1970-01-01 noon UTC.
        // Unix epoch 1970-01-01 00:00 UTC = JDN 2_440_587.5.
        // (jdn - 2_440_588) * 86_400_000 + 43_200_000  =>  maps integer JDN to its correct noon.
        val millis = (jdn - 2_440_588).toLong() * 86_400_000L + 43_200_000L
        return Date(millis)
    }
}

private fun selectBestUpcomingEvent(
    todayHijri: HijriDate,
    ordinalInYear: (HijriDate) -> Int,
    isLeapYear: (Int) -> Boolean,
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
                    isLeapYear = isLeapYear,
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
    isLeapYear: (Int) -> Boolean,
): RankedUpcomingCandidate? {
    val eventYear = todayYear + yearOffset
    val eventDate = HijriDate(eventYear, event.month, event.day)
    val eventOrdinal = ordinalInYear(eventDate)
    val adjustedOrdinal =
        if (yearOffset == 0) {
            eventOrdinal
        } else {
            eventOrdinal + if (isLeapYear(todayYear)) 355 else 354
        }
    if (adjustedOrdinal <= todayOrdinal) return null
    return RankedUpcomingCandidate(
        event = UpcomingEvent(event, eventDate, adjustedOrdinal - todayOrdinal),
        ordinal = adjustedOrdinal,
    )
}
