package com.prayertime.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.prayertime.data.LocationDataSourceTestSupport
import com.prayertime.data.local.AppDatabase
import com.prayertime.data.local.InMemoryCityConfigDataSource
import com.prayertime.data.remote.ScenarioPrayerApi
import com.prayertime.domain.model.CityConfig
import com.prayertime.domain.model.FetchError
import com.prayertime.domain.model.Prayer
import com.prayertime.domain.model.PrayerTimesResult
import com.prayertime.domain.model.SaveCityError
import com.prayertime.domain.model.SaveCityResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnlinePrayerTimesRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var citySource: InMemoryCityConfigDataSource
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        LocationDataSourceTestSupport.initializeFromTestResource()
        database =
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        citySource = InMemoryCityConfigDataSource()
        runBlocking { citySource.setOfflineOnly(false) }
    }

    @After
    fun teardown() {
        database.close()
    }

    private fun repo(api: ScenarioPrayerApi) = OnlinePrayerTimesRepository(citySource, database, api)

    private val meccaConfig =
        CityConfig(
            cityName = "Mecca",
            countryCode = "SA",
            timezone = "Asia/Riyadh",
            latitude = 21.4225,
            longitude = 39.8262,
        )

    private val berlinConfig =
        CityConfig(
            cityName = "Berlin",
            countryCode = "DE",
            timezone = "Europe/Berlin",
            latitude = 52.52,
            longitude = 13.405,
        )

    @Test
    fun `online fetch returns city-specific API Fajr not legacy flat mock`() =
        runTest {
            val api = ScenarioPrayerApi()
            val result = repo(api).fetchTodayTimes(meccaConfig)
            assertTrue(result is PrayerTimesResult.Success)
            val fajr = (result as PrayerTimesResult.Success).times.first { it.prayer == Prayer.FAJR }
            assertEquals("04:02", fajr.displayTime)
            assertNotEquals("05:12", fajr.displayTime)
            assertEquals(listOf(ScenarioPrayerApi.Request("Mecca", "SA")), api.timingsCalls)
        }

    @Test
    fun `online fetch Berlin and Mecca return different API wall times`() =
        runTest {
            val api = ScenarioPrayerApi()
            val repository = repo(api)
            val meccaFajr =
                (repository.fetchTodayTimes(meccaConfig) as PrayerTimesResult.Success)
                    .times.first { it.prayer == Prayer.FAJR }
            val berlinFajr =
                (repository.fetchTodayTimes(berlinConfig) as PrayerTimesResult.Success)
                    .times.first { it.prayer == Prayer.FAJR }
            assertEquals("04:02", meccaFajr.displayTime)
            assertEquals("03:18", berlinFajr.displayTime)
            assertNotEquals(meccaFajr.displayTime, berlinFajr.displayTime)
        }

    @Test
    fun `online fetch strips Aladhan timezone suffix in cached display times`() =
        runTest {
            val api = ScenarioPrayerApi()
            val result = repo(api).fetchTodayTimes(meccaConfig) as PrayerTimesResult.Success
            result.times.forEach { prayerTime ->
                assertTrue(
                    "${prayerTime.displayTime} should not contain suffix",
                    !prayerTime.displayTime.contains("("),
                )
            }
        }

    @Test
    fun `online fetch persists API times to Room under city key`() =
        runTest {
            val api = ScenarioPrayerApi()
            repo(api).fetchTodayTimes(meccaConfig)
            val label =
                SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("Asia/Riyadh")
                }.format(Date())
            val rows = database.prayerTimeDao().getByCityAndDate("SA_Mecca", label)
            assertEquals(6, rows.size)
            assertEquals("04:02", rows.first { it.prayer == "FAJR" }.displayTime)
        }

    @Test
    fun `incomplete API response falls back to local adhan calculation`() =
        runTest {
            val api =
                ScenarioPrayerApi(
                    mapOf(
                        "mecca|SA" to ScenarioPrayerApi.incomplete(),
                    ),
                )
            val result = repo(api).fetchTodayTimes(meccaConfig)
            assertTrue(result is PrayerTimesResult.Success)
            val success = result as PrayerTimesResult.Success
            assertEquals(6, success.times.size)
            assertTrue(success.times.any { it.prayer == Prayer.SHURUQ })
            assertNotEquals("04:02", success.times.first { it.prayer == Prayer.FAJR }.displayTime)
        }

    @Test
    fun `API null response falls back to local calculation when coords valid`() =
        runTest {
            val api = ScenarioPrayerApi().apply { forceNullTimings = true }
            val result = repo(api).fetchTodayTimes(meccaConfig)
            assertTrue(result is PrayerTimesResult.Success)
            assertEquals(6, (result as PrayerTimesResult.Success).times.size)
        }

    @Test
    fun `network failure with valid coords falls back to local calculation`() =
        runTest {
            val api = ScenarioPrayerApi().apply { timingsFailure = IOException("timeout") }
            val result = repo(api).fetchTodayTimes(meccaConfig)
            assertTrue(result is PrayerTimesResult.Success)
            assertEquals(6, (result as PrayerTimesResult.Success).times.size)
        }

    @Test
    fun `network failure without coords returns NETWORK when cache empty`() =
        runTest {
            val api = ScenarioPrayerApi().apply { timingsFailure = IOException("timeout") }
            val result =
                repo(api).fetchTodayTimes(
                    CityConfig("Nowhere", "XX", "UTC"),
                )
            assertTrue(result is PrayerTimesResult.Error)
            assertEquals(FetchError.NETWORK, (result as PrayerTimesResult.Error).type)
        }

    @Test
    fun `offline only mode never calls API`() =
        runTest {
            citySource.setOfflineOnly(true)
            val api = ScenarioPrayerApi()
            val result = repo(api).fetchTodayTimes(meccaConfig)
            assertTrue(result is PrayerTimesResult.Success)
            assertTrue(api.timingsCalls.isEmpty())
        }

    @Test
    fun `online save enriches config from API geocode meta`() =
        runTest {
            val api = ScenarioPrayerApi()
            val repository = repo(api)
            val saved = repository.saveCityConfig(CityConfig("Mecca", "SA", "UTC"))
            assertTrue(saved is SaveCityResult.Success)
            val enriched = (saved as SaveCityResult.Success).config
            assertEquals("Asia/Riyadh", enriched.timezone)
            assertEquals(21.4225, enriched.latitude!!, 0.001)
            assertEquals(39.8262, enriched.longitude!!, 0.001)
            assertEquals(enriched, citySource.cityConfig.first())
        }

    @Test
    fun `save then fetchTodayTimes does not call timings API again`() =
        runTest {
            val api = ScenarioPrayerApi()
            val repository = repo(api)
            val saved = repository.saveCityConfig(CityConfig("Mecca", "SA", "UTC"))
            assertTrue(saved is SaveCityResult.Success)
            assertEquals(listOf(ScenarioPrayerApi.Request("Mecca", "SA")), api.timingsWithGeocodeCalls)
            assertTrue(api.timingsCalls.isEmpty())

            val enriched = (saved as SaveCityResult.Success).config
            val result = repository.fetchTodayTimes(enriched)
            assertTrue(result is PrayerTimesResult.Success)
            assertTrue(
                "Expected no second getTimingsByCity after save cached timings",
                api.timingsCalls.isEmpty(),
            )
            assertEquals(listOf(ScenarioPrayerApi.Request("Mecca", "SA")), api.timingsWithGeocodeCalls)
        }

    @Test
    fun `saveCityConfig falls back to local when API geocode not in scenario`() =
        runTest {
            citySource.setOfflineOnly(false)
            // Damascus is not in ScenarioPrayerApi defaults → getTimingsWithGeocode returns null
            val api = ScenarioPrayerApi()
            val result = repo(api).saveCityConfig(CityConfig("Damascus", "SY", "UTC"))
            assertTrue("Expected Success from local fallback, got $result", result is SaveCityResult.Success)
            val config = (result as SaveCityResult.Success).config
            assertEquals("Damascus", config.cityName)
            assertTrue(config.latitude!! > 0.0)
        }

    @Test
    fun `saveCityConfig falls back to local geocode when API geocode fails for known city`() =
        runTest {
            citySource.setOfflineOnly(false)
            val api = ScenarioPrayerApi().apply { geocodeFailure = IOException("timeout") }
            val result = repo(api).saveCityConfig(CityConfig("Berlin", "DE", "UTC"))
            assertTrue("Expected Success from local fallback, got $result", result is SaveCityResult.Success)
            val config = (result as SaveCityResult.Success).config
            assertEquals("Berlin", config.cityName)
            assertEquals("Europe/Berlin", config.timezone)
            assertTrue(config.latitude!! > 50.0)
        }

    @Test
    fun `saveCityConfig preserves wizard-resolved coords when API geocode fails`() =
        runTest {
            citySource.setOfflineOnly(false)
            val draft =
                CityConfig(
                    cityName = "CustomTown",
                    countryCode = "DE",
                    timezone = "Europe/Berlin",
                    latitude = 52.5,
                    longitude = 9.5,
                )
            val api = ScenarioPrayerApi().apply { geocodeFailure = IOException("timeout") }
            val result = repo(api).saveCityConfig(draft)
            assertTrue("Expected Success preserving draft coords, got $result", result is SaveCityResult.Success)
            val saved = (result as SaveCityResult.Success).config
            assertEquals("CustomTown", saved.cityName)
            assertEquals(52.5, saved.latitude!!, 0.01)
            assertEquals(9.5, saved.longitude!!, 0.01)
        }

    @Test
    fun `saveCityConfig returns error when both API geocode and local fail`() =
        runTest {
            citySource.setOfflineOnly(false)
            // NonExistentVillage not in scenario and local geocode also fails
            val api = ScenarioPrayerApi()
            val result = repo(api).saveCityConfig(CityConfig("NonExistentVillage", "DE", "UTC"))
            assertTrue("Expected Error when both API and local fail, got $result", result is SaveCityResult.Error)
            assertEquals(SaveCityError.CITY_NOT_FOUND, (result as SaveCityResult.Error).type)
        }

    // --- 5C.3: Re-enable network ---

    @Test
    fun `5C_3 online repo returns cached data when available`() =
        runTest {
            val api = ScenarioPrayerApi()
            val repository = repo(api)

            val first = repository.fetchTodayTimes(meccaConfig)
            assertTrue(first is PrayerTimesResult.Success)
            assertEquals(1, api.timingsCalls.size)

            val second = repository.fetchTodayTimes(meccaConfig)
            assertTrue(second is PrayerTimesResult.Success)
            assertEquals(1, api.timingsCalls.size) // cache used, no new API call
        }

    @Test
    fun `5C_3 invalidate cache then network fetch returns fresh data`() =
        runTest {
            val api = ScenarioPrayerApi()
            val repository = repo(api)

            repository.fetchTodayTimes(meccaConfig)
            assertEquals(1, api.timingsCalls.size)

            repository.invalidateTodayCache(meccaConfig)
            val fresh = repository.fetchTodayTimes(meccaConfig)
            assertTrue(fresh is PrayerTimesResult.Success)
            assertEquals(2, api.timingsCalls.size)
        }

    @Test
    fun `5C_3 network failure after cache invalidation falls back to local calculation`() =
        runTest {
            val api = ScenarioPrayerApi().apply { timingsFailure = IOException("no network") }
            val repository = repo(api)

            repository.invalidateTodayCache(meccaConfig)

            val result = repository.fetchTodayTimes(meccaConfig)
            assertTrue(result is PrayerTimesResult.Success)
            assertEquals(6, (result as PrayerTimesResult.Success).times.size)
        }
}
