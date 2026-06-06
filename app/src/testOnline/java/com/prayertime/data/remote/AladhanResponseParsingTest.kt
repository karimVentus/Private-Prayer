package com.prayertime.data.remote

import com.prayertime.data.repository.AladhanTimingsMapper
import com.prayertime.domain.model.Prayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Gson fixture → [AladhanTimingsMapper] pipeline; exercises real Aladhan JSON shapes. */
class AladhanResponseParsingTest {
    @Test
    fun `Mecca fixture parses suffix times and maps Sunrise to SHURUQ`() {
        val api = AladhanTestFixtures.loadResource("aladhan/mecca_2024-06-03.json")!!
        val times = AladhanTimingsMapper.buildPrayerTimes(api.timingsMap, api.date, api.timezone)

        assertEquals(6, times.size)
        assertEquals(Prayer.FAJR, times[0].prayer)
        assertEquals("04:02", times[0].displayTime)
        assertEquals(Prayer.SHURUQ, times[1].prayer)
        assertEquals("05:23", times[1].displayTime)
    }

    @Test
    fun `Mecca fixture Fajr timestamp uses API timezone not UTC wall clock`() {
        val api = AladhanTestFixtures.loadResource("aladhan/mecca_2024-06-03.json")!!
        val times = AladhanTimingsMapper.buildPrayerTimes(api.timingsMap, api.date, api.timezone)

        val expected =
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("Asia/Riyadh") }
                .parse("2024-06-03 04:02")!!
                .time
        assertEquals(expected, times.first { it.prayer == Prayer.FAJR }.timestamp)
    }

    @Test
    fun `Berlin and Mecca fixtures produce different Fajr wall times and UTC epochs`() {
        val mecca = AladhanTestFixtures.loadResource("aladhan/mecca_2024-06-03.json")!!
        val berlin = AladhanTestFixtures.loadResource("aladhan/berlin_2024-06-03.json")!!

        val meccaTimes = AladhanTimingsMapper.buildPrayerTimes(mecca.timingsMap, mecca.date, mecca.timezone)
        val berlinTimes = AladhanTimingsMapper.buildPrayerTimes(berlin.timingsMap, berlin.date, berlin.timezone)

        val meccaFajr = meccaTimes.first { it.prayer == Prayer.FAJR }
        val berlinFajr = berlinTimes.first { it.prayer == Prayer.FAJR }

        assertEquals("04:02", meccaFajr.displayTime)
        assertEquals("03:18", berlinFajr.displayTime)
        assertNotEquals(meccaFajr.displayTime, berlinFajr.displayTime)
        assertNotEquals(meccaFajr.timestamp, berlinFajr.timestamp)
    }

    @Test
    fun `Berlin fixture uses Europe Berlin timezone from meta`() {
        val api = AladhanTestFixtures.loadResource("aladhan/berlin_2024-06-03.json")!!
        assertEquals("Europe/Berlin", api.timezone)

        val times = AladhanTimingsMapper.buildPrayerTimes(api.timingsMap, api.date, api.timezone)
        val expected =
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("Europe/Berlin") }
                .parse("2024-06-03 03:18")!!
                .time
        assertEquals(expected, times.first { it.prayer == Prayer.FAJR }.timestamp)
    }

    @Test
    fun `partial timings produce fewer than 6 prayers`() {
        val partial =
            mapOf(
                "Fajr" to "04:02",
                "Dhuhr" to "12:15",
                "Isha" to "20:17",
            )
        val times = AladhanTimingsMapper.buildPrayerTimes(partial, "03-06-2024", "Asia/Riyadh")
        assertEquals(3, times.size)
        assertEquals(Prayer.FAJR, times[0].prayer)
        assertEquals(Prayer.DHUHR, times[1].prayer)
        assertEquals(Prayer.ISHA, times[2].prayer)
    }

    @Test
    fun `legacy flat mock times differ from Mecca fixture`() {
        val legacy = ScenarioPrayerApi.legacyFlatMock()
        val mecca = AladhanTestFixtures.loadResource("aladhan/mecca_2024-06-03.json")!!

        assertNotEquals(
            legacy.timingsMap["Fajr"],
            mecca.timingsMap["Fajr"],
        )
        val legacyMapped = AladhanTimingsMapper.buildPrayerTimes(legacy.timingsMap, legacy.date, "UTC")
        val meccaMapped = AladhanTimingsMapper.buildPrayerTimes(mecca.timingsMap, mecca.date, mecca.timezone)
        assertNotEquals(
            legacyMapped.first { it.prayer == Prayer.FAJR }.displayTime,
            meccaMapped.first { it.prayer == Prayer.FAJR }.displayTime,
        )
    }

    @Test
    fun `fixture timestamps stay chronological`() {
        val api = AladhanTestFixtures.loadResource("aladhan/mecca_2024-06-03.json")!!
        val times = AladhanTimingsMapper.buildPrayerTimes(api.timingsMap, api.date, api.timezone)
        for (i in 1 until times.size) {
            assertTrue(times[i].timestamp > times[i - 1].timestamp)
        }
    }
}
