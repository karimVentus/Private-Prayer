package com.prayertime.widget

import com.prayertime.data.local.AppPreferencesDataSource
import com.prayertime.data.repository.PrayerTimesRepository
import com.prayertime.domain.model.CityConfig
import com.prayertime.domain.model.FetchError
import com.prayertime.domain.model.Prayer
import com.prayertime.domain.model.PrayerTime
import com.prayertime.domain.model.PrayerTimesResult
import com.prayertime.ui.theme.AppTheme
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetSnapshotLoaderTest {
    private val repository = mockk<PrayerTimesRepository>()
    private val preferences = mockk<AppPreferencesDataSource>()
    private val loader = WidgetSnapshotLoader(repository, preferences)

    init {
        coEvery { preferences.readAppThemeOnce() } returns AppTheme.DEFAULT_STORAGE_KEY
    }

    private val config =
        CityConfig(
            cityName = "Hameln",
            countryCode = "DE",
            timezone = "Europe/Berlin",
            latitude = 52.1,
            longitude = 9.35,
        )

    private val times =
        listOf(
            PrayerTime(Prayer.FAJR, "04:00", 1_700_000_000_000L),
            PrayerTime(Prayer.SHURUQ, "05:30", 1_700_005_400_000L),
        )

    @Test
    fun noCity_returnsEmptyState() =
        runTest {
            coEvery { repository.cityConfig } returns flowOf(null)
            val snapshot = loader.load()
            assertEquals(WidgetSnapshot.State.NO_CITY, snapshot.state)
        }

    @Test
    fun success_returnsTimesAndCityLabel() =
        runTest {
            coEvery { repository.cityConfig } returns flowOf(config)
            coEvery { repository.fetchTodayTimes(config) } returns
                PrayerTimesResult.Success(times, Prayer.FAJR, 60_000L)
            val snapshot = loader.load()
            assertEquals(WidgetSnapshot.State.READY, snapshot.state)
            assertEquals("Hameln, DE", snapshot.cityLabel)
            assertEquals(2, snapshot.times.size)
            assertEquals(Prayer.FAJR, snapshot.nextPrayer)
            assertNotNull(snapshot.hijriDate)
        }

    @Test
    fun error_withoutCache_returnsErrorStateWithHijri() =
        runTest {
            coEvery { repository.cityConfig } returns flowOf(config)
            coEvery { repository.fetchTodayTimes(config) } returns
                PrayerTimesResult.Error(FetchError.NETWORK)
            coEvery { repository.getCachedTodayTimes(config) } returns null
            val snapshot = loader.load()
            assertEquals(WidgetSnapshot.State.ERROR, snapshot.state)
            assertTrue(snapshot.times.isEmpty())
            assertNotNull(snapshot.hijriDate)
        }

    @Test
    fun error_withCache_returnsStaleState() =
        runTest {
            coEvery { repository.cityConfig } returns flowOf(config)
            coEvery { repository.fetchTodayTimes(config) } returns
                PrayerTimesResult.Error(FetchError.NETWORK)
            coEvery { repository.getCachedTodayTimes(config) } returns
                PrayerTimesResult.Success(times, Prayer.FAJR, 60_000L)
            val snapshot = loader.load()
            assertEquals(WidgetSnapshot.State.STALE, snapshot.state)
            assertEquals(2, snapshot.times.size)
            assertEquals(Prayer.FAJR, snapshot.nextPrayer)
            assertNotNull(snapshot.hijriDate)
        }
}
