package com.prayertime.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.prayertime.data.LocationDataSourceTestSupport
import com.prayertime.data.local.AppDatabase
import com.prayertime.data.local.CityConfigSerializer
import com.prayertime.data.local.PrayerTimeEntity
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Offline [LocalPrayerTimesRepository] — geocode, cache, adhan-java. Network parsing: [AladhanTimingsMapperTest], [OnlinePrayerTimesRepositoryTest]. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PrayerTimesRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var cityConfigSerializer: CityConfigSerializer
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        LocationDataSourceTestSupport.initializeFromTestResource()
        database =
            Room.inMemoryDatabaseBuilder(
                context, AppDatabase::class.java,
            ).allowMainThreadQueries().build()
        cityConfigSerializer = CityConfigSerializer(context)
        runBlocking { cityConfigSerializer.resetCityStore() }
    }

    @After
    fun teardown() {
        runBlocking { cityConfigSerializer.resetCityStore() }
        database.close()
    }

    private fun localRepo() = LocalPrayerTimesRepository(cityConfigSerializer, database)

    @Test
    fun `save and read city config uses local geocode`() =
        runTest {
            val repo = localRepo()
            val config = CityConfig("Damascus", "SY", "Asia/Damascus")
            val saved = repo.saveCityConfig(config)
            assertTrue(saved is SaveCityResult.Success)
            val enriched = (saved as SaveCityResult.Success).config
            assertEquals(33.513, enriched.latitude!!, 0.001)
            assertEquals(36.292, enriched.longitude!!, 0.001)
            assertEquals("Asia/Damascus", enriched.timezone)
            assertEquals(enriched, repo.cityConfig.first())
        }

    @Test
    fun `local cache serves when coordinates unavailable for calculation`() =
        runTest {
            val repo = localRepo()
            val todayLabel =
                SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date())

            val now = System.currentTimeMillis()
            val hour = 3_600_000L
            val cityKey = "TT_Test"
            val seedTimes =
                listOf(
                    PrayerTimeEntity(
                        prayer = "FAJR",
                        displayTime = "05:00",
                        timestamp = now + hour,
                        dateLabel = todayLabel,
                        cityKey = cityKey,
                    ),
                    PrayerTimeEntity(
                        prayer = "SHURUQ",
                        displayTime = "06:30",
                        timestamp = now + 2 * hour,
                        dateLabel = todayLabel,
                        cityKey = cityKey,
                    ),
                    PrayerTimeEntity(
                        prayer = "DHUHR",
                        displayTime = "12:30",
                        timestamp = now + 3 * hour,
                        dateLabel = todayLabel,
                        cityKey = cityKey,
                    ),
                    PrayerTimeEntity(
                        prayer = "ASR",
                        displayTime = "15:45",
                        timestamp = now + 4 * hour,
                        dateLabel = todayLabel,
                        cityKey = cityKey,
                    ),
                    PrayerTimeEntity(
                        prayer = "MAGHRIB",
                        displayTime = "18:10",
                        timestamp = now + 5 * hour,
                        dateLabel = todayLabel,
                        cityKey = cityKey,
                    ),
                    PrayerTimeEntity(
                        prayer = "ISHA",
                        displayTime = "19:40",
                        timestamp = now + 6 * hour,
                        dateLabel = todayLabel,
                        cityKey = cityKey,
                    ),
                )
            database.prayerTimeDao().insertAll(seedTimes)

            val cached = database.prayerTimeDao().getByCityAndDate(cityKey, todayLabel)
            assertTrue("Room should have cached data, found ${cached.size}", cached.isNotEmpty())

            val noCoordsConfig = CityConfig("Test", "TT", "UTC")
            val result = repo.fetchTodayTimes(noCoordsConfig)
            assertTrue("Expected Success but got $result", result is PrayerTimesResult.Success)
            assertEquals(6, (result as PrayerTimesResult.Success).times.size)
        }

    @Test
    fun `local calculation with valid coordinates yields 6 prayers including Duha`() =
        runTest {
            val repo = localRepo()
            val mecca =
                CityConfig(
                    cityName = "Mecca",
                    countryCode = "SA",
                    timezone = "Asia/Riyadh",
                    latitude = 21.4225,
                    longitude = 39.8262,
                )
            val result = repo.fetchTodayTimes(mecca)
            assertTrue("Expected Success but got $result", result is PrayerTimesResult.Success)
            val success = result as PrayerTimesResult.Success
            assertEquals(6, success.times.size)
            val prayers = success.times.map { it.prayer }
            assertTrue("Missing Shuruq in $prayers", prayers.contains(Prayer.SHURUQ))
        }

    @Test
    fun `Damascus and Mecca timing golden — 6 prayers, chronological, within today`() =
        runTest {
            val repo = localRepo()
            val damascus =
                CityConfig("Damascus", "SY", "Asia/Damascus", 33.513, 36.292)
            val mecca =
                CityConfig("Mecca", "SA", "Asia/Riyadh", 21.4225, 39.8262)

            for ((label, config) in listOf("Damascus" to damascus, "Mecca" to mecca)) {
                val result = repo.fetchTodayTimes(config)
                assertTrue("$label: expected Success, got $result", result is PrayerTimesResult.Success)
                val times = (result as PrayerTimesResult.Success).times

                assertEquals("$label: expected 6 prayers", 6, times.size)

                for (i in 1 until times.size) {
                    assertTrue(
                        "$label: ${times[i].prayer} not after ${times[i - 1].prayer}",
                        times[i].timestamp > times[i - 1].timestamp,
                    )
                }

                val prayerSet = times.map { it.prayer }.toSet()
                assertTrue("$label: missing Shuruq", prayerSet.contains(Prayer.SHURUQ))
                assertTrue("$label: missing FAJR", prayerSet.contains(Prayer.FAJR))
                assertTrue("$label: missing ISHA", prayerSet.contains(Prayer.ISHA))

                val shuruqIdx = times.indexOfFirst { it.prayer == Prayer.SHURUQ }
                val fajrIdx = times.indexOfFirst { it.prayer == Prayer.FAJR }
                val dhuhrIdx = times.indexOfFirst { it.prayer == Prayer.DHUHR }
                assertTrue("$label: Shuruq not between FAJR and DHUHR", shuruqIdx in (fajrIdx + 1) until dhuhrIdx)

                val now = System.currentTimeMillis()
                times.forEach { pt ->
                    val diff = pt.timestamp - now
                    assertTrue(
                        "$label: ${pt.prayer} timestamp outside ±24h of now",
                        diff in -86_400_000L..86_400_000L,
                    )
                }
            }
        }

    @Test
    fun `offline only mode uses local geocode and never calls API`() =
        runTest {
            val repo = localRepo()
            val config = CityConfig("Damascus", "SY", "UTC")
            val saved = repo.saveCityConfig(config)
            assertTrue("Save should succeed in offline mode: $saved", saved is SaveCityResult.Success)
            val enriched = (saved as SaveCityResult.Success).config
            assertEquals(33.513, enriched.latitude!!, 0.001)
            assertEquals(36.292, enriched.longitude!!, 0.001)
            assertEquals("Asia/Damascus", enriched.timezone)

            val result = repo.fetchTodayTimes(enriched)
            assertTrue("Expected Success but got $result", result is PrayerTimesResult.Success)
            val success = result as PrayerTimesResult.Success
            assertEquals(6, success.times.size)
            val prayers = success.times.map { it.prayer }
            assertTrue("Missing Shuruq in $prayers", prayers.contains(Prayer.SHURUQ))
        }

    @Test
    fun `offline city change keeps prior city cache and builds new city cache`() =
        runTest {
            val repo = localRepo()

            val firstSaved = repo.saveCityConfig(CityConfig("Damascus", "SY", "UTC"))
            assertTrue(firstSaved is SaveCityResult.Success)
            val firstConfig = (firstSaved as SaveCityResult.Success).config
            val firstFetch = repo.fetchTodayTimes(firstConfig)
            assertTrue(firstFetch is PrayerTimesResult.Success)

            val firstLatestLabel = database.prayerTimeDao().getLatestDateLabel("SY_Damascus")
            assertTrue(firstLatestLabel != null)
            val firstRows = database.prayerTimeDao().getByCityAndDate("SY_Damascus", firstLatestLabel!!)
            assertEquals(6, firstRows.size)

            repo.clearCityConfig()
            assertNull(repo.cityConfig.first())
            assertEquals(6, database.prayerTimeDao().getByCityAndDate("SY_Damascus", firstLatestLabel).size)

            val secondSaved = repo.saveCityConfig(CityConfig("Hameln", "DE", "UTC"))
            assertTrue(secondSaved is SaveCityResult.Success)
            val secondConfig = (secondSaved as SaveCityResult.Success).config
            assertNotEquals(firstConfig.cityName, secondConfig.cityName)

            val secondFetch = repo.fetchTodayTimes(secondConfig)
            assertTrue(secondFetch is PrayerTimesResult.Success)

            val secondLatestLabel = database.prayerTimeDao().getLatestDateLabel("DE_Hameln")
            assertTrue(secondLatestLabel != null)
            val secondRows = database.prayerTimeDao().getByCityAndDate("DE_Hameln", secondLatestLabel!!)
            assertEquals(6, secondRows.size)
        }

    @Test
    fun `invalidateTodayCache drops today rows but clearCityConfig keeps Room cache`() =
        runTest {
            val repo = localRepo()
            val config = (repo.saveCityConfig(CityConfig("Hameln", "DE", "UTC")) as SaveCityResult.Success).config
            repo.fetchTodayTimes(config)
            val label = database.prayerTimeDao().getLatestDateLabel("DE_Hameln")!!
            assertEquals(6, database.prayerTimeDao().getByCityAndDate("DE_Hameln", label).size)

            repo.invalidateTodayCache(config)
            assertTrue(database.prayerTimeDao().getByCityAndDate("DE_Hameln", label).isEmpty())

            repo.fetchTodayTimes(config)
            assertEquals(6, database.prayerTimeDao().getByCityAndDate("DE_Hameln", label).size)

            repo.clearCityConfig()
            assertEquals(6, database.prayerTimeDao().getByCityAndDate("DE_Hameln", label).size)
        }

    @Test
    fun `traveler round trip Hameln Berlin Hameln reuses Hameln cache`() =
        runTest {
            val repo = localRepo()
            val label =
                SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("Europe/Berlin")
                }.format(Date())

            val hamelnConfig = (repo.saveCityConfig(CityConfig("Hameln", "DE", "UTC")) as SaveCityResult.Success).config
            repo.fetchTodayTimes(hamelnConfig)
            val hamelnBefore = database.prayerTimeDao().getByCityAndDate("DE_Hameln", label)
            assertEquals(6, hamelnBefore.size)
            val hamelnFajrTs = hamelnBefore.first { it.prayer == "FAJR" }.timestamp

            repo.clearCityConfig()
            val berlinConfig = (repo.saveCityConfig(CityConfig("Berlin", "DE", "UTC")) as SaveCityResult.Success).config
            repo.fetchTodayTimes(berlinConfig)
            assertEquals(6, database.prayerTimeDao().getByCityAndDate("DE_Berlin", label).size)
            assertEquals(6, database.prayerTimeDao().getByCityAndDate("DE_Hameln", label).size)

            repo.clearCityConfig()
            val hamelnAgain = (repo.saveCityConfig(CityConfig("Hameln", "DE", "UTC")) as SaveCityResult.Success).config
            val hamelnAfter = database.prayerTimeDao().getByCityAndDate("DE_Hameln", label)
            assertEquals(6, hamelnAfter.size)
            assertEquals(hamelnFajrTs, hamelnAfter.first { it.prayer == "FAJR" }.timestamp)

            val fetchResult = repo.fetchTodayTimes(hamelnAgain)
            assertTrue(fetchResult is PrayerTimesResult.Success)
        }

    @Test
    fun `offline save succeeds for listed German city`() =
        runTest {
            val repo = localRepo()
            val config = CityConfig("Flensburg", "DE", "UTC")
            val saved = repo.saveCityConfig(config)
            assertTrue("Expected Success for Flensburg but got $saved", saved is SaveCityResult.Success)
            val enriched = (saved as SaveCityResult.Success).config
            assertEquals(54.785, enriched.latitude!!, 0.01)
            assertEquals(9.437, enriched.longitude!!, 0.01)
            assertEquals("Europe/Berlin", enriched.timezone)
        }

    @Test
    fun `offline save rejects fallback city with CITY_NOT_FOUND`() =
        runTest {
            val repo = localRepo()
            val result = repo.saveCityConfig(CityConfig("NonExistentVillage", "DE", "UTC"))
            assertTrue(result is SaveCityResult.Error)
            assertEquals(SaveCityError.CITY_NOT_FOUND, (result as SaveCityResult.Error).type)
            assertNull(repo.cityConfig.first())
        }

    @Test
    fun `offline save rejects typo city with CITY_NOT_FOUND`() =
        runTest {
            val repo = localRepo()
            for (typo in listOf("Osnabruc", "Saarbruken", "Hamburrg")) {
                val result = repo.saveCityConfig(CityConfig(typo, "DE", "UTC"))
                assertTrue("Typo '$typo': expected Error, got $result", result is SaveCityResult.Error)
                assertEquals("Typo '$typo': expected CITY_NOT_FOUND", SaveCityError.CITY_NOT_FOUND, (result as SaveCityResult.Error).type)
            }
        }

    @Test
    fun `offline save succeeds for ascii german picker cities with umlaut keys`() =
        runTest {
            val repo = localRepo()
            for (city in listOf("Osnabruck", "Saarbrucken")) {
                val saved = repo.saveCityConfig(CityConfig(city, "DE", "UTC"))
                assertTrue("$city: expected Success, got $saved", saved is SaveCityResult.Success)
            }
        }

    @Test
    fun `offline save rejects unknown city with CITY_NOT_FOUND`() =
        runTest {
            val repo = localRepo()
            val result = repo.saveCityConfig(CityConfig("NonExistentVillage", "DE", "UTC"))
            assertTrue("Expected Error for Fallback city, got $result", result is SaveCityResult.Error)
            assertEquals(SaveCityError.CITY_NOT_FOUND, (result as SaveCityResult.Error).type)
            assertNull(repo.cityConfig.first())
        }

    @Test
    fun `local geocode resolves known city successfully`() =
        runTest {
            val repo = localRepo()
            val result = repo.saveCityConfig(CityConfig("Damascus", "SY", "UTC"))
            assertTrue("Expected success from local fallback but got $result", result is SaveCityResult.Success)
            val enriched = (result as SaveCityResult.Success).config
            assertEquals(33.513, enriched.latitude!!, 0.001)
            assertEquals(36.292, enriched.longitude!!, 0.001)
        }

    @Test
    fun `null coordinates on fetch returns MISSING_COORDINATES error`() =
        runTest {
            val repo = localRepo()
            val result = repo.fetchTodayTimes(CityConfig("Nowhere", "XX", "UTC"))
            assertTrue(result is PrayerTimesResult.Error)
            assertEquals(FetchError.MISSING_COORDINATES, (result as PrayerTimesResult.Error).type)
        }
}
