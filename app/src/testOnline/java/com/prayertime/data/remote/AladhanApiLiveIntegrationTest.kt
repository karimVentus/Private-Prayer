package com.prayertime.data.remote

import com.prayertime.data.repository.AladhanTimingsMapper
import com.prayertime.domain.model.Prayer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * End-to-end HTTP against [api.aladhan.com](https://api.aladhan.com) — Retrofit, Gson, and [AladhanApi].
 * Complements [AladhanApiMockWebServerTest] (synthetic fixtures on [MockWebServer]).
 * Skips when offline or when `PRAYERTIME_LIVE_HTTP=0`.
 */
class AladhanApiLiveIntegrationTest {
    private lateinit var api: AladhanApi

    @Before
    fun setup() {
        LiveAladhanTestSupport.assumeLiveApiReachable()
        api = LiveAladhanTestSupport.createApi()
    }

    @Test
    fun getTimingsByCity_liveMecca_returnsSixPrayersWithTimezone() =
        runBlocking {
            val result = api.getTimingsByCity("Mecca", "SA")

            assertNotNull(result)
            assertEquals("Asia/Riyadh", result!!.timezone)
            assertEquals(6, result.timingsMap.size)
            for (key in listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha")) {
                assertTrue("$key missing from live response", result.timingsMap.containsKey(key))
                assertTrue(result.timingsMap.getValue(key).isNotBlank())
            }
        }

    @Test
    fun getTimingsWithGeocode_liveMecca_returnsMetaCoordinatesAndTimings() =
        runBlocking {
            val result = api.getTimingsWithGeocode("Mecca", "SA")

            assertNotNull(result)
            assertEquals("Asia/Riyadh", result!!.timezone)
            assertTrue(result.latitude in -90.0..90.0)
            assertTrue(result.longitude in -180.0..180.0)
            assertEquals(6, result.timingsMap.size)
        }

    @Test
    fun liveMeccaResponse_mapsToSixChronologicalPrayerTimes() =
        runBlocking {
            val response = api.getTimingsByCity("Mecca", "SA")
            assertNotNull(response)

            val times =
                AladhanTimingsMapper.buildPrayerTimes(
                    response!!.timingsMap,
                    response.date,
                    response.timezone,
                )

            assertEquals(6, times.size)
            assertEquals(Prayer.FAJR, times[0].prayer)
            assertEquals(Prayer.SHURUQ, times[1].prayer)
            assertEquals(Prayer.ISHA, times[5].prayer)
            val timestamps = times.map { it.timestamp }
            for (i in 1 until timestamps.size) {
                assertTrue(
                    "Prayer times out of order at index $i",
                    timestamps[i] > timestamps[i - 1],
                )
            }
        }
}
