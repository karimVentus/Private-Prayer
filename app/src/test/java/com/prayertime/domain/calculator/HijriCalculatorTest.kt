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
    // -- gregorianToHijri with known Umm al-Qura dates (OpenJDK Hijrah-umalqura) --

    private fun Date.toHijri(): HijriDate {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { time = this@toHijri }
        return HijriCalculator.gregorianToHijri(
            cal[Calendar.YEAR],
            cal[Calendar.MONTH] + 1,
            cal[Calendar.DAY_OF_MONTH],
        )
    }

    @Test
    fun epoch_is_1Muharram1AH_tabularFallback() {
        // 622 CE is outside the bundled Umm al-Qura table (1300–1600 AH).
        val result = HijriCalculator.gregorianToHijri(622, 7, 19)
        assertEquals(HijriDate(1, 1, 1), result)
    }

    @Test
    fun ramadan1445_is_march11_2024() {
        val result = HijriCalculator.gregorianToHijri(2024, 3, 11)
        assertEquals(HijriDate(1445, 9, 1), result)
    }

    @Test
    fun eidAlFitr1445_is_april10_2024() {
        val result = HijriCalculator.gregorianToHijri(2024, 4, 10)
        assertEquals(HijriDate(1445, 10, 1), result)
    }

    @Test
    fun eidAlAdha1445_is_june17_2024() {
        // 10 Dhul Hijjah 1445 = June 16; June 17 is the 11th.
        val tenth = HijriCalculator.gregorianToHijri(2024, 6, 16)
        assertEquals(HijriDate(1445, 12, 10), tenth)
        val result = HijriCalculator.gregorianToHijri(2024, 6, 17)
        assertEquals(HijriDate(1445, 12, 11), result)
    }

    @Test
    fun june18_2026_is_3Muharram1448() {
        val result = HijriCalculator.gregorianToHijri(2026, 6, 18)
        assertEquals(HijriDate(1448, 1, 3), result)
    }

    @Test
    fun todayInTimezone_berlin_matchesGregorianCivilDate() {
        val result = HijriCalculator.todayInTimezone("Europe/Berlin")
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"))
        val expected =
            HijriCalculator.gregorianToHijri(
                cal[Calendar.YEAR],
                cal[Calendar.MONTH] + 1,
                cal[Calendar.DAY_OF_MONTH],
            )
        assertEquals(expected, result)
    }

    // -- Round-trip (Hijri → Gregorian → Hijri) --

    @Test
    fun roundTrip_preservesHijriDate() {
        val original = HijriDate(1445, 9, 1)
        val gregorian = HijriCalculator.hijriToGregorian(original)
        val back = gregorian.toHijri()
        assertEquals(original, back)
    }

    @Test
    fun hijriToGregorian_mapsToNoonUtc() {
        val date = HijriCalculator.hijriToGregorian(HijriDate(1446, 1, 1))
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { time = date }
        assertEquals(2024, cal[Calendar.YEAR])
        assertEquals(Calendar.JULY, cal[Calendar.MONTH])
        assertEquals(7, cal[Calendar.DAY_OF_MONTH])
        assertEquals(12, cal[Calendar.HOUR_OF_DAY])
        assertEquals(0, cal[Calendar.MINUTE])
    }

    @Test
    fun hijriToGregorian_westernTimezone_preservesGregorianDate() {
        val date = HijriCalculator.hijriToGregorian(HijriDate(1445, 9, 1))
        val cal = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles")).apply { time = date }
        assertEquals(2024, cal[Calendar.YEAR])
        assertEquals(Calendar.MARCH, cal[Calendar.MONTH])
        assertEquals(11, cal[Calendar.DAY_OF_MONTH])
    }

    @Test
    fun roundTrip_endOfLeapYear() {
        val original = HijriDate(1445, 12, 30)
        val gregorian = HijriCalculator.hijriToGregorian(original)
        val back = gregorian.toHijri()
        assertEquals(original, back)
    }

    @Test
    fun roundTrip_endOfRegularYear() {
        val original = HijriDate(1446, 12, 29)
        val gregorian = HijriCalculator.hijriToGregorian(original)
        val back = gregorian.toHijri()
        assertEquals(original, back)
    }

    // -- Leap year / month lengths (Umm al-Qura table) --

    @Test
    fun isLeapYear_1445_has30DayDhulHijjah() {
        assertTrue(HijriCalculator.isLeapYear(1445))
        assertEquals(30, HijriCalculator.daysInMonth(1445, 12))
    }

    @Test
    fun isLeapYear_1446_has29DayDhulHijjah() {
        assertFalse(HijriCalculator.isLeapYear(1446))
        assertEquals(29, HijriCalculator.daysInMonth(1446, 12))
    }

    @Test
    fun isLeapYear_tabularFallback_negativeYear() {
        assertTrue(HijriCalculator.isLeapYear(-1))
        assertFalse(HijriCalculator.isLeapYear(-30))
    }

    @Test
    fun daysInMonth_ummAlQura1446_matchesTable() {
        assertEquals(29, HijriCalculator.daysInMonth(1446, 1))
        assertEquals(30, HijriCalculator.daysInMonth(1446, 2))
        assertEquals(30, HijriCalculator.daysInMonth(1446, 3))
        assertEquals(29, HijriCalculator.daysInMonth(1446, 12))
    }

    @Test
    fun fullYearCycle_noExceptions() {
        val start = HijriDate(1445, 1, 1)
        val end = HijriDate(1445, 12, 30)

        val gregorianStart = HijriCalculator.hijriToGregorian(start)
        val gregorianEnd = HijriCalculator.hijriToGregorian(end)

        val daysInYear = gregorianEnd.time - gregorianStart.time
        val expectedDays = 353L * 86_400_000L
        assertEquals(expectedDays, daysInYear)
    }

    @Test
    fun fullYearCycle_roundTrip() {
        val testDays =
            listOf(
                HijriDate(1445, 1, 1),
                HijriDate(1445, 1, 15),
                HijriDate(1445, 6, 1),
                HijriDate(1445, 6, 15),
                HijriDate(1445, 9, 1),
                HijriDate(1445, 9, 30),
                HijriDate(1445, 10, 1),
                HijriDate(1445, 10, 15),
                HijriDate(1445, 12, 1),
                HijriDate(1445, 12, 30),
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
        val result = HijriCalculator.gregorianToHijri(2024, 4, 9)
        assertEquals(HijriDate(1445, 9, 30), result)
    }

    @Test
    fun firstDayOfYear1446() {
        val result = HijriCalculator.gregorianToHijri(2024, 7, 7)
        assertEquals(HijriDate(1446, 1, 1), result)
    }

    // -- Upcoming events --

    @Test
    fun nextEvent_onRamadan1_isLaylatAlQadr() {
        val today = HijriDate(1445, 9, 1)
        val upcoming = HijriCalculator.nextUpcomingEvent(today)
        org.junit.Assert.assertNotNull(upcoming)
        assertEquals(IslamicEvent.LAYLAT_AL_QADR, upcoming!!.event)
        assertEquals(26, upcoming.daysUntil)
    }

    @Test
    fun nextEvent_eidFitrIs30DaysAfterRamadan1() {
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
        val today = HijriDate(1445, 12, 11)
        val upcoming = HijriCalculator.nextUpcomingEvent(today)
        org.junit.Assert.assertNotNull(upcoming)
        assertEquals(IslamicEvent.ISLAMIC_NEW_YEAR, upcoming!!.event)
        assertEquals(HijriDate(1446, 1, 1), upcoming.hijriDate)
    }

    @Test
    fun nextEvent_wrapToNextYear_returnsCorrectDays() {
        val today = HijriDate(1445, 12, 30)
        val upcoming = HijriCalculator.nextUpcomingEvent(today)
        org.junit.Assert.assertNotNull(upcoming)
        assertEquals(IslamicEvent.ISLAMIC_NEW_YEAR, upcoming!!.event)
        assertEquals(1, upcoming.daysUntil)
    }
}
