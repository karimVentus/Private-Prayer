package com.prayertime.alarm

import com.prayertime.data.local.InMemoryCityConfigDataSource
import com.prayertime.data.repository.PrayerTimesRepository
import com.prayertime.domain.model.CityConfig
import com.prayertime.domain.model.FetchError
import com.prayertime.domain.model.Prayer
import com.prayertime.domain.model.PrayerTimesResult
import com.prayertime.testing.FakePrayerTimesRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BootPrayerTimesResolverTest {
    private val hamelnConfig =
        CityConfig(
            cityName = "Hameln",
            countryCode = "DE",
            timezone = "Europe/Berlin",
            latitude = 52.1,
            longitude = 9.4,
        )

    @Test
    fun `resolve returns fetch success when network succeeds`() =
        runTest {
            val expected = FakePrayerTimesRepository.defaultSuccessResult()
            val repo = stubRepository(fetch = expected)
            assertEquals(expected, BootPrayerTimesResolver.resolve(repo, hamelnConfig))
        }

    @Test
    fun `resolve returns cache when fetch fails and cache hit`() =
        runTest {
            val cached = FakePrayerTimesRepository.defaultSuccessResult()
            val repo =
                stubRepository(
                    fetch = PrayerTimesResult.Error(FetchError.NETWORK),
                    cached = cached,
                )
            assertEquals(cached, BootPrayerTimesResolver.resolve(repo, hamelnConfig))
        }

    @Test
    fun `resolve calculates locally when fetch fails and cache empty`() =
        runTest {
            val repo =
                stubRepository(
                    fetch = PrayerTimesResult.Error(FetchError.NETWORK),
                    cached = null,
                )
            when (val resolved = BootPrayerTimesResolver.resolve(repo, hamelnConfig)) {
                is PrayerTimesResult.Success -> {
                    assertEquals(6, resolved.times.size)
                    assertTrue(resolved.times.any { it.prayer == Prayer.FAJR })
                }
                is PrayerTimesResult.Error -> error("Expected local fallback Success")
            }
        }

    @Test
    fun `localFallback returns error without coordinates`() {
        val config =
            CityConfig(
                cityName = "Unknown",
                countryCode = "XX",
                timezone = "UTC",
                latitude = null,
                longitude = null,
            )
        assertEquals(
            FetchError.UNKNOWN,
            (BootPrayerTimesResolver.localFallback(config) as PrayerTimesResult.Error).type,
        )
    }

    private fun stubRepository(
        fetch: PrayerTimesResult,
        cached: PrayerTimesResult? = null,
    ): PrayerTimesRepository {
        val fake = FakePrayerTimesRepository.forPrayerTimes(InMemoryCityConfigDataSource())
        fake.fetchOverride = { fetch }
        fake.cachedTodayOverride = cached
        return fake
    }
}
