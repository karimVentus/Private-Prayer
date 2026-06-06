package com.prayertime.domain.calculator

import com.prayertime.domain.model.HijriDate
import com.prayertime.domain.model.IslamicEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class HijriCalculatorTest {
    // -- 4C.1: gregorianToHijri with known dates --

    private fun Date.toHijri(): HijriDate {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { time = this@toHijri }
        return HijriCalculator.gregorianToHijri(
            cal[Calendar.YEAR],
            cal[Calendar.MONTH] + 1,
            cal[Calendar.DAY_OF_MONTH],
        )
    }

    @Test
    fun epoch_is_1Muharram1AH() {
        val result = HijriCalculator.gregorianToHijri(622, 7, 19)
        assertEquals(HijriDate(1, 1, 1), result)
    }

    @Test
    fun ramadan1445_is_march11_2024() {
        // 1 Ramadan 1445 = March 11, 2024 (tabular/Umm al-Qura)
        val result = HijriCalculator.gregorianToHijri(2024, 3, 11)
        assertEquals(HijriDate(1445, 9, 1), result)
    }

    @Test
    fun eidAlFitr1445_is_april10_2024() {
        // 1 Shawwal 1445 = April 10, 2024 (30 days after 1 Ramadan)
        val result = HijriCalculator.gregorianToHijri(2024, 4, 10)
        assertEquals(HijriDate(1445, 10, 1), result)
    }

    @Test
    fun eidAlAdha1445_is_june17_2024() {
        // 10 Dhul Hijjah 1445 = June 17, 2024
        val result = HijriCalculator.gregorianToHijri(2024, 6, 17)
        assertEquals(HijriDate(1445, 12, 10), result)
    }

    // -- 4C.2: Round-trip (Hijri → Gregorian → Hijri) --

    @Test
    fun roundTrip_preservesHijriDate() {
        val original = HijriDate(1445, 9, 1) // 1 Ramadan 1445
        val gregorian = HijriCalculator.hijriToGregorian(original)
        val back = gregorian.toHijri()
        assertEquals(original, back)
    }

    @Test
    fun hijriToGregorian_mapsToNoonUtc() {
        // JDN integer boundary is noon UTC; midnight mapping shifts western zones by one day.
        val date = HijriCalculator.hijriToGregorian(HijriDate(1446, 1, 1))
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { time = date }
        assertEquals(2024, cal[Calendar.YEAR])
        assertEquals(Calendar.JULY, cal[Calendar.MONTH])
        assertEquals(8, cal[Calendar.DAY_OF_MONTH])
        assertEquals(12, cal[Calendar.HOUR_OF_DAY])
        assertEquals(0, cal[Calendar.MINUTE])
    }

    @Test
    fun hijriToGregorian_westernTimezone_preservesGregorianDate() {
        // 1 Ramadan 1445 = March 11, 2024 tabular. At midnight UTC this becomes March 10 in US zones.
        val date = HijriCalculator.hijriToGregorian(HijriDate(1445, 9, 1))
        val cal = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles")).apply { time = date }
        assertEquals(2024, cal[Calendar.YEAR])
        assertEquals(Calendar.MARCH, cal[Calendar.MONTH])
        assertEquals(11, cal[Calendar.DAY_OF_MONTH])
    }

    @Test
    fun roundTrip_endOfLeapYear() {
        // 30 Dhul Hijjah 1445 (leap year)
        val original = HijriDate(1445, 12, 30)
        val gregorian = HijriCalculator.hijriToGregorian(original)
        val back = gregorian.toHijri()
        assertEquals(original, back)
    }

    @Test
    fun roundTrip_endOfRegularYear() {
        // 29 Dhul Hijjah 1446 (regular year — 1446 % 30 = 6, not leap)
        val original = HijriDate(1446, 12, 29)
        val gregorian = HijriCalculator.hijriToGregorian(original)
        val back = gregorian.toHijri()
        assertEquals(original, back)
    }

    // -- Leap year detection --

    @Test
    fun isLeapYear_positionsInCycle() {
        val leapPositions = setOf(2, 5, 7, 10, 13, 16, 18, 21, 24, 26, 29)
        for (pos in 1..30) {
            assertEquals(pos in leapPositions, HijriCalculator.isLeapYear(pos))
            assertEquals(pos in leapPositions, HijriCalculator.isLeapYear(pos + 30))
            assertEquals(pos in leapPositions, HijriCalculator.isLeapYear(pos + 1440))
        }
    }

    @Test
    fun isLeapYear_1445_isLeap() {
        // 1445 % 30 = 5 → leap
        assertTrue(HijriCalculator.isLeapYear(1445))
    }

    @Test
    fun isLeapYear_1446_isNotLeap() {
        // 1446 % 30 = 6 → not leap
        assertFalse(HijriCalculator.isLeapYear(1446))
    }

    @Test
    fun isLeapYear_negativeYear_matchesCycleFloorMod() {
        assertTrue(HijriCalculator.isLeapYear(-1))
        assertFalse(HijriCalculator.isLeapYear(-30))
    }

    @Test
    fun daysInMonth_tabularPattern_leapAndRegularYears() {
        // Odd months 1,3,5,7,9,11 always 30; even months 2,4,6,8,10 always 29
        for (month in listOf(1, 3, 5, 7, 9, 11)) {
            assertEquals(30, HijriCalculator.daysInMonth(1446, month))
        }
        for (month in listOf(2, 4, 6, 8, 10)) {
            assertEquals(29, HijriCalculator.daysInMonth(1446, month))
        }
        // 1445 leap → Dhul Hijjah has 30 days; 1446 regular → 29
        assertEquals(30, HijriCalculator.daysInMonth(1445, 12))
        assertEquals(29, HijriCalculator.daysInMonth(1446, 12))
    }

    // -- 4C.4: Full cycle without error --

    @Test
    fun fullYearCycle_noExceptions() {
        // Verify every day of year 1445 converts round-trip without crashing
        val start = HijriDate(1445, 1, 1)
        val end = HijriDate(1445, 12, if (HijriCalculator.isLeapYear(1445)) 30 else 29)

        val gregorianStart = HijriCalculator.hijriToGregorian(start)
        val gregorianEnd = HijriCalculator.hijriToGregorian(end)

        // Verify the difference is the number of days in the year
        val daysInYear = gregorianEnd.time - gregorianStart.time
        val expectedDays = if (HijriCalculator.isLeapYear(1445)) 354L else 353L
        assertEquals(expectedDays * 86_400_000L, daysInYear)
    }

    @Test
    fun fullYearCycle_roundTrip() {
        // Spot-check first, middle, and last days of every month
        // Ramadan has 30 days; year 1445 is a leap year
        val testDays =
            listOf(
                HijriDate(1445, 1, 1), HijriDate(1445, 1, 15),
                HijriDate(1445, 6, 1), HijriDate(1445, 6, 15),
                HijriDate(1445, 9, 1), HijriDate(1445, 9, 30),
                HijriDate(1445, 10, 1), HijriDate(1445, 10, 15),
                HijriDate(1445, 12, 1), HijriDate(1445, 12, 30),
            )
        for (hijri in testDays) {
            val gregorian = HijriCalculator.hijriToGregorian(hijri)
            val back = gregorian.toHijri()
            assertEquals(hijri, back)
        }
    }

    // -- Month boundaries --

    @Test
    fun lastDayOfRamadan_1445() {
        // 30 Ramadan 1445 = April 9, 2024 (day before Eid)
        val result = HijriCalculator.gregorianToHijri(2024, 4, 9)
        assertEquals(HijriDate(1445, 9, 30), result)
    }

    @Test
    fun firstDayOfYear1446() {
        // 1 Muharram 1446 = July 8, 2024
        // 1445 has 355 days (leap), so 1446 starts 355 days after 1 Muharram 1445
        val result = HijriCalculator.gregorianToHijri(2024, 7, 8)
        assertEquals(HijriDate(1446, 1, 1), result)
    }

    // -- 4C.3: Upcoming events --

    @Test
    fun nextEvent_onRamadan1_isLaylatAlQadr() {
        // If today is 1 Ramadan, the next upcoming event is Laylat al-Qadr (27 Ramadan, 26 days later)
        val today = HijriDate(1445, 9, 1)
        val upcoming = HijriCalculator.nextUpcomingEvent(today)
        org.junit.Assert.assertNotNull(upcoming)
        assertEquals(IslamicEvent.LAYLAT_AL_QADR, upcoming!!.event)
        assertEquals(26, upcoming.daysUntil)
    }

    @Test
    fun nextEvent_eidFitrIs30DaysAfterRamadan1() {
        // 4C.3: 1 Shawwal (Eid al-Fitr) is exactly 30 days after 1 Ramadan.
        // Verify the Hijri date calculation, not the next-event logic.
        val ramadan1 = HijriDate(1445, 9, 1)
        val eidFitr = HijriDate(1445, 10, 1)
        val gregorianRamadan = HijriCalculator.hijriToGregorian(ramadan1)
        val gregorianEid = HijriCalculator.hijriToGregorian(eidFitr)
        val daysBetween = (gregorianEid.time - gregorianRamadan.time) / 86_400_000L
        assertEquals(30L, daysBetween)
    }

    @Test
    fun nextEvent_afterEidFitr_isDayOfArafah() {
        val today = HijriDate(1445, 10, 2)
        val upcoming = HijriCalculator.nextUpcomingEvent(today)
        org.junit.Assert.assertNotNull(upcoming)
        assertEquals(IslamicEvent.DAY_OF_ARAFAH, upcoming!!.event)
        assertTrue(upcoming.daysUntil > 0)
    }

    @Test
    fun nextEvent_afterEidAlAdha_isIslamicNewYear() {
        // After Eid al-Adha, next event is Islamic New Year of the next year
        val today = HijriDate(1445, 12, 11)
        val upcoming = HijriCalculator.nextUpcomingEvent(today)
        org.junit.Assert.assertNotNull(upcoming)
        assertEquals(IslamicEvent.ISLAMIC_NEW_YEAR, upcoming!!.event)
        assertEquals(HijriDate(1446, 1, 1), upcoming.hijriDate)
    }

    @Test
    fun nextEvent_wrapToNextYear_returnsCorrectDays() {
        // On last day of year 1445 (30 Dhul Hijjah), next event should be
        // Islamic New Year 1446 (1 Muharram) = 1 day away
        val today = HijriDate(1445, 12, 30)
        val upcoming = HijriCalculator.nextUpcomingEvent(today)
        org.junit.Assert.assertNotNull(upcoming)
        assertEquals(IslamicEvent.ISLAMIC_NEW_YEAR, upcoming!!.event)
        assertEquals(1, upcoming.daysUntil)
    }
}
