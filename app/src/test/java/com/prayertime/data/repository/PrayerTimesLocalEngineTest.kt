package com.prayertime.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.prayertime.data.LocationDataSourceTestSupport
import com.prayertime.data.local.AppDatabase
import com.prayertime.data.local.InMemoryCityConfigDataSource
import com.prayertime.data.local.PrayerTimeEntity
import com.prayertime.domain.calculator.PrayerDayLabels
import com.prayertime.domain.model.CityConfig
import com.prayertime.domain.model.Prayer
import com.prayertime.domain.model.PrayerTimesResult
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PrayerTimesLocalEngineTest {
    private lateinit var database: AppDatabase
    private lateinit var engine: PrayerTimesLocalEngine

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        LocationDataSourceTestSupport.initializeFromTestResource()
        database =
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        engine = PrayerTimesLocalEngine(database)
    }

    @After
    fun teardown() {
        database.close()
    }

    // --- resolveLocalCoordinates ---

    @Test
    fun `resolveLocalCoordinates keeps pre-resolved coordinates`() =
        runTest {
            val config = CityConfig("UnknownVillage", "DE", "Europe/Berlin", 52.0, 9.0)
            val result = engine.resolveLocalCoordinates(config)
            assertTrue(result is GeocodeResult.Success)
            assertEquals(52.0, (result as GeocodeResult.Success).config.latitude!!, 0.01)
        }

    // --- fetchLocalTimes: cache hit ---

    @Test
    fun `fetchLocalTimes returns cached result when cache exists`() =
        runTest {
            val cityKey = "SA_Mecca"
            val todayLabel = todayLabelUtc()
            val config = CityConfig("Mecca", "SA", "UTC", 21.42, 39.83)

            // Seed cache
            val cachedTimes =
                listOf(
                    entity("FAJR", "05:00", 1000, todayLabel, cityKey),
                    entity("DUHA", "06:30", 2000, todayLabel, cityKey),
                    entity("DHUHR", "12:30", 3000, todayLabel, cityKey),
                    entity("ASR", "15:45", 4000, todayLabel, cityKey),
                    entity("MAGHRIB", "18:10", 5000, todayLabel, cityKey),
                    entity("ISHA", "19:40", 6000, todayLabel, cityKey),
                )
            database.prayerTimeDao().insertAll(cachedTimes)

            val cache = engine.getCachedTimes(cityKey, todayLabel, "UTC")
            assertNotNull("Cache should exist", cache)

            // fetchLocalTimes should return cache, not recalculate
            val result = engine.fetchLocalTimes(config, cityKey, todayLabel, cache)
            assertTrue("Expected Success from cache", result is PrayerTimesResult.Success)
            val times = (result as PrayerTimesResult.Success).times
            assertEquals(6, times.size)
            assertEquals(1000, times[0].timestamp) // From cache, not recalculated
            assertEquals(Prayer.SHURUQ, times[1].prayer) // legacy "DUHA" cache row
            assertEquals(6, database.prayerTimeDao().getByCityAndDate(cityKey, todayLabel).size)
        }

    @Test
    fun `fetchLocalTimes without cache calculates and runs cleanup`() =
        runTest {
            val cityKey = "DE_Hameln"
            val todayLabel = todayLabelUtc()
            val oldDate = daysAgoLabel(10)
            val config = CityConfig("Hameln", "DE", "Europe/Berlin", 52.104, 9.356)

            database.prayerTimeDao().insertAll(
                listOf(
                    PrayerTimeEntity(
                        prayer = "FAJR",
                        displayTime = "05:00",
                        timestamp = 1,
                        dateLabel = oldDate,
                        cityKey = cityKey,
                    ),
                ),
            )

            val result = engine.fetchLocalTimes(config, cityKey, todayLabel, cache = null)
            assertTrue(result is PrayerTimesResult.Success)
            assertTrue(database.prayerTimeDao().getByCityAndDate(cityKey, oldDate).isEmpty())
            assertEquals(6, database.prayerTimeDao().getByCityAndDate(cityKey, todayLabel).size)
        }

    // --- cleanupOldEntries ---

    @Test
    fun `cleanupOldEntries removes entries older than 7 days`() =
        runTest {
            val cityKey = "DE_Hameln"
            val today = todayLabelUtc()
            val eightDaysAgo = daysAgoLabel(8)

            // Seed old + new entries
            database.prayerTimeDao().insertAll(
                listOf(
                    entity("FAJR", "05:00", 1, eightDaysAgo, cityKey),
                ),
            )
            database.prayerTimeDao().insertAll(
                listOf(
                    entity("FAJR", "05:00", 1, today, cityKey),
                ),
            )

            engine.cleanupOldEntries(cityKey, "UTC")

            val oldEntries = database.prayerTimeDao().getByCityAndDate(cityKey, eightDaysAgo)
            val newEntries = database.prayerTimeDao().getByCityAndDate(cityKey, today)
            assertTrue("Old entries should be cleaned", oldEntries.isEmpty())
            assertEquals(1, newEntries.size)
        }

    @Test
    fun `cleanupOldEntries keeps entries exactly 7 days old`() =
        runTest {
            val cityKey = "DE_Hameln"
            val sevenDaysAgo = daysAgoLabel(7)

            database.prayerTimeDao().insertAll(
                listOf(
                    entity("FAJR", "05:00", 1, sevenDaysAgo, cityKey),
                ),
            )

            engine.cleanupOldEntries(cityKey, "UTC")

            val remaining = database.prayerTimeDao().getByCityAndDate(cityKey, sevenDaysAgo)
            assertEquals("7-day-old entries should survive cleanup", 1, remaining.size)
        }

    @Test
    fun `cleanupOldEntries cutoff uses calendar days on London spring-forward week`() =
        runTest {
            val london = TimeZone.getTimeZone("Europe/London")
            val cityKey = "GB_London"
            val cal =
                Calendar.getInstance(london).apply {
                    set(2024, Calendar.MARCH, 31, 12, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
            val now = cal.timeInMillis
            val cutoff = PrayerDayLabels.daysAgo(7, london, now)
            cal.add(Calendar.DAY_OF_MONTH, -7)
            val expectedCutoff =
                SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = london }.format(cal.time)
            assertEquals(expectedCutoff, cutoff)

            database.prayerTimeDao().insertAll(
                listOf(
                    entity("FAJR", "05:00", 1, expectedCutoff, cityKey),
                    entity("FAJR", "05:00", 1, "2024-03-23", cityKey),
                ),
            )

            engine.cleanupOldEntries(cityKey, "Europe/London", now)

            assertEquals(1, database.prayerTimeDao().getByCityAndDate(cityKey, expectedCutoff).size)
            assertTrue(database.prayerTimeDao().getByCityAndDate(cityKey, "2024-03-23").isEmpty())
        }

    @Test
    fun `cleanupOldEntries only affects specified city`() =
        runTest {
            val oldDate = daysAgoLabel(10)

            database.prayerTimeDao().insertAll(
                listOf(
                    entity("FAJR", "05:00", 1, oldDate, "DE_Hameln"),
                ),
            )
            database.prayerTimeDao().insertAll(
                listOf(
                    entity("FAJR", "05:00", 1, oldDate, "SY_Damascus"),
                ),
            )

            engine.cleanupOldEntries("DE_Hameln", "UTC")

            val hameln = database.prayerTimeDao().getByCityAndDate("DE_Hameln", oldDate)
            val damascus = database.prayerTimeDao().getByCityAndDate("SY_Damascus", oldDate)
            assertTrue("Hameln old entries cleaned", hameln.isEmpty())
            assertEquals("Damascus unaffected", 1, damascus.size)
        }

    // --- clearCityCache ---

    @Test
    fun `clearCityCache deletes only target city data`() =
        runTest {
            val today = todayLabelUtc()
            database.prayerTimeDao().insertAll(
                listOf(
                    entity("FAJR", "05:00", 1, today, "DE_Hameln"),
                    entity("FAJR", "05:00", 1, today, "SY_Damascus"),
                ),
            )

            engine.clearCityCache("DE_Hameln")

            val hameln = database.prayerTimeDao().getByCityAndDate("DE_Hameln", today)
            val damascus = database.prayerTimeDao().getByCityAndDate("SY_Damascus", today)
            assertTrue("Hameln cleared", hameln.isEmpty())
            assertEquals("Damascus survives", 1, damascus.size)
        }

    @Test
    fun `clearAllPrayerTimeCache deletes everything`() =
        runTest {
            val today = todayLabelUtc()
            database.prayerTimeDao().insertAll(
                listOf(
                    entity("FAJR", "05:00", 1, today, "DE_Hameln"),
                    entity("FAJR", "05:00", 1, today, "SY_Damascus"),
                ),
            )

            engine.clearAllPrayerTimeCache()

            val hameln = database.prayerTimeDao().getByCityAndDate("DE_Hameln", today)
            val damascus = database.prayerTimeDao().getByCityAndDate("SY_Damascus", today)
            assertTrue("All cleared", hameln.isEmpty() && damascus.isEmpty())
        }

    // --- getLatestDateLabel ---

    @Test
    fun `getLatestDateLabel returns newest date for city`() =
        runTest {
            val cityKey = "DE_Hameln"
            database.prayerTimeDao().insertAll(
                listOf(
                    entity("FAJR", "05:00", 1, "2026-01-01", cityKey),
                ),
            )
            database.prayerTimeDao().insertAll(
                listOf(
                    entity("FAJR", "05:00", 1, "2026-06-15", cityKey),
                ),
            )
            database.prayerTimeDao().insertAll(
                listOf(
                    entity("FAJR", "05:00", 1, "2026-03-10", cityKey),
                ),
            )

            val latest = database.prayerTimeDao().getLatestDateLabel(cityKey)
            assertEquals("2026-06-15", latest)
        }

    @Test
    fun `getLatestDateLabel returns null for unknown city`() =
        runTest {
            val result = database.prayerTimeDao().getLatestDateLabel("XX_Nope")
            assertNull(result)
        }

    @Test
    fun `getLatestDateLabel scoped to cityKey not global`() =
        runTest {
            database.prayerTimeDao().insertAll(
                listOf(
                    PrayerTimeEntity(
                        prayer = "FAJR",
                        displayTime = "05:00",
                        timestamp = 1,
                        dateLabel = "2026-01-01",
                        cityKey = "DE_Hameln",
                    ),
                ),
            )
            database.prayerTimeDao().insertAll(
                listOf(
                    PrayerTimeEntity(
                        prayer = "FAJR",
                        displayTime = "05:00",
                        timestamp = 1,
                        dateLabel = "2026-12-31",
                        cityKey = "SY_Damascus",
                    ),
                ),
            )

            val hameln = database.prayerTimeDao().getLatestDateLabel("DE_Hameln")
            val damascus = database.prayerTimeDao().getLatestDateLabel("SY_Damascus")
            assertEquals("2026-01-01", hameln)
            assertEquals("2026-12-31", damascus)
        }

    // --- Helpers ---

    private fun entity(
        prayer: String,
        displayTime: String,
        timestamp: Long,
        dateLabel: String,
        cityKey: String,
    ) = PrayerTimeEntity(
        prayer = prayer,
        displayTime = displayTime,
        timestamp = timestamp,
        dateLabel = dateLabel,
        cityKey = cityKey,
    )

    private fun todayLabelUtc(): String {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(Date())
    }

    private fun daysAgoLabel(days: Int): String {
        val cal = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.add(java.util.Calendar.DAY_OF_YEAR, -days)
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(cal.time)
    }

    @Test
    fun getCachedTimes_dedupesDuplicatePrayerRows() =
        runTest {
            val cityKey = "DE_Hameln"
            val today = todayLabelUtc()
            database.prayerTimeDao().insertAll(
                listOf(
                    entity("FAJR", "02:47", 1L, today, cityKey),
                    entity("FAJR", "02:47", 2L, today, cityKey),
                    entity("SHURUQ", "05:05", 3L, today, cityKey),
                    entity("SHURUQ", "05:05", 4L, today, cityKey),
                    entity("DHUHR", "13:21", 5L, today, cityKey),
                    entity("DHUHR", "13:21", 6L, today, cityKey),
                ),
            )
            assertEquals(6, database.prayerTimeDao().getByCityAndDate(cityKey, today).size)

            val result = engine.getCachedTimes(cityKey, today, "UTC") as PrayerTimesResult.Success

            assertEquals(3, result.times.size)
            assertEquals(listOf(Prayer.FAJR, Prayer.SHURUQ, Prayer.DHUHR), result.times.map { it.prayer })
        }

    // ═══════════════════════════════════════════════════════════════════════
    // 5C: Offline robustness (airplane mode)
    // ═══════════════════════════════════════════════════════════════════════

    // 5C.1 — Airplane mode at launch: cached times returned

    @Test
    fun `5C_1 — cached times survive with offline-only LocalPrayerTimesRepository`() =
        runTest {
            val cityKey = "SA_Mecca"
            val todayLabel = todayLabelUtc()
            val config = CityConfig("Mecca", "SA", "UTC", 21.42, 39.83)

            // Seed full 6-prayer cache
            val cachedTimes =
                listOf(
                    entity("FAJR", "05:00", 1_000_000, todayLabel, cityKey),
                    entity("DUHA", "06:30", 2_000_000, todayLabel, cityKey),
                    entity("DHUHR", "12:30", 3_000_000, todayLabel, cityKey),
                    entity("ASR", "15:45", 4_000_000, todayLabel, cityKey),
                    entity("MAGHRIB", "18:10", 5_000_000, todayLabel, cityKey),
                    entity("ISHA", "19:40", 6_000_000, todayLabel, cityKey),
                )
            database.prayerTimeDao().insertAll(cachedTimes)

            // Offline repo: fetchTodayTimes checks cache first → returns it
            val citySource = InMemoryCityConfigDataSource()
            citySource.save(config)
            val repo =
                LocalPrayerTimesRepository.withEngine(citySource, engine)

            val result = repo.fetchTodayTimes(config)

            assertTrue("Airplane mode: cache should be returned", result is PrayerTimesResult.Success)
            val times = (result as PrayerTimesResult.Success).times
            assertEquals(6, times.size)
            assertEquals(1_000_000, times.first().timestamp) // from cache, not recalculated
        }

    @Test
    fun `5C_1 — getCachedTodayTimes returns cache without computation`() =
        runTest {
            val cityKey = "DE_Berlin"
            val todayLabel = todayLabelUtc()
            val config = CityConfig("Berlin", "DE", "UTC", 52.52, 13.405)

            val cachedTimes =
                listOf(
                    entity("FAJR", "05:00", 1L, todayLabel, cityKey),
                    entity("SHURUQ", "06:30", 2L, todayLabel, cityKey),
                    entity("DHUHR", "12:30", 3L, todayLabel, cityKey),
                    entity("ASR", "15:45", 4L, todayLabel, cityKey),
                    entity("MAGHRIB", "18:10", 5L, todayLabel, cityKey),
                    entity("ISHA", "19:40", 6L, todayLabel, cityKey),
                )
            database.prayerTimeDao().insertAll(cachedTimes)

            val cache = engine.getCachedTimes(cityKey, todayLabel, "UTC")
            assertNotNull("Cache must be accessible in airplane mode", cache)
            assertTrue(cache is PrayerTimesResult.Success)
            assertEquals(6, (cache as PrayerTimesResult.Success).times.size)
        }

    // 5C.2 — Airplane mode for 7 days: multi-day cache survives

    @Test
    fun `5C_2 — cache from 6 days ago survives 7-day cleanup window`() =
        runTest {
            val cityKey = "DE_Hameln"
            val today = todayLabelUtc()
            val sixDaysAgoLabel = daysAgoLabel(6)

            // Seed cache entries from today and 6 days ago
            database.prayerTimeDao().insertAll(
                listOf(
                    entity("FAJR", "05:00", 1L, today, cityKey),
                    entity("FAJR", "05:00", 1L, sixDaysAgoLabel, cityKey),
                ),
            )

            engine.cleanupOldEntries(cityKey, "UTC")

            val todayCache = database.prayerTimeDao().getByCityAndDate(cityKey, today)
            val sixDaysAgoCache = database.prayerTimeDao().getByCityAndDate(cityKey, sixDaysAgoLabel)
            assertEquals("Today's cache must survive", 1, todayCache.size)
            assertEquals("6-day-old cache must survive cleanup", 1, sixDaysAgoCache.size)
        }

    @Test
    fun `5C_2 — cache from 8 days ago is removed by cleanup`() =
        runTest {
            val cityKey = "DE_Hameln"
            val today = todayLabelUtc()
            val eightDaysAgoLabel = daysAgoLabel(8)

            database.prayerTimeDao().insertAll(
                listOf(
                    entity("FAJR", "05:00", 1L, today, cityKey),
                    entity("ISHA", "19:40", 1L, eightDaysAgoLabel, cityKey),
                ),
            )

            engine.cleanupOldEntries(cityKey, "UTC")

            val eightDaysAgoCache = database.prayerTimeDao().getByCityAndDate(cityKey, eightDaysAgoLabel)
            assertTrue("8-day-old cache must be removed", eightDaysAgoCache.isEmpty())
        }

    @Test
    fun `5C_2 — 7 consecutive days of cache accessible after offline week`() =
        runTest {
            val cityKey = "SA_Mecca"
            val today = todayLabelUtc()

            // Simulate a week offline: seed cache for each of the last 7 days
            for (daysBack in 0..6) {
                val label = daysAgoLabel(daysBack)
                database.prayerTimeDao().insertAll(
                    listOf(
                        entity("FAJR", "05:00", 1L, label, cityKey),
                        entity("ISHA", "19:40", 1L, label, cityKey),
                    ),
                )
            }

            // Also seed day 8 (should be cleaned)
            database.prayerTimeDao().insertAll(
                listOf(
                    entity("FAJR", "05:00", 1L, daysAgoLabel(8), cityKey),
                ),
            )

            engine.cleanupOldEntries(cityKey, "UTC")

            // Days 0-6 must survive (7 days), day 8 must be gone
            for (daysBack in 0..6) {
                val label = daysAgoLabel(daysBack)
                val entries = database.prayerTimeDao().getByCityAndDate(cityKey, label)
                assertEquals("Day $daysBack ago ($label) must survive", 2, entries.size)
            }
            val day8Entries = database.prayerTimeDao().getByCityAndDate(cityKey, daysAgoLabel(8))
            assertTrue("Day 8 must be cleaned", day8Entries.isEmpty())
        }

    // 5C.3 — Re-enable network: fresh fetch replaces stale cache

    @Test
    fun `5C_3 — invalidateTodayCache clears cache so next fetch recalculates`() =
        runTest {
            val cityKey = "DE_Hameln"
            val todayLabel = todayLabelUtc()
            val config = CityConfig("Hameln", "DE", "Europe/Berlin", 52.104, 9.356)

            // Cache some old timestamps
            database.prayerTimeDao().insertAll(
                listOf(
                    entity("FAJR", "02:00", 999_999L, todayLabel, cityKey),
                    entity("DHUHR", "13:00", 2_999_999L, todayLabel, cityKey),
                    entity("ASR", "17:00", 3_999_999L, todayLabel, cityKey),
                    entity("MAGHRIB", "21:00", 4_999_999L, todayLabel, cityKey),
                    entity("ISHA", "23:00", 5_999_999L, todayLabel, cityKey),
                    entity("SHURUQ", "05:00", 1_999_999L, todayLabel, cityKey),
                ),
            )

            val cachedBefore = engine.getCachedTimes(cityKey, todayLabel, "Europe/Berlin")
            assertNotNull("Cache should exist before invalidation", cachedBefore)

            // Simulate re-enabling network → invalidate today's cache
            engine.invalidateTodayCache(cityKey, todayLabel)
            val cachedAfter = engine.getCachedTimes(cityKey, todayLabel, "Europe/Berlin")
            assertNull("Cache must be gone after invalidation (fresh fetch needed)", cachedAfter)

            // Fresh fetch should recalculate (not just return old cache)
            val fresh = engine.fetchLocalTimes(config, cityKey, todayLabel, cache = null)
            assertTrue("Fresh fetch must succeed", fresh is PrayerTimesResult.Success)
            val freshTimes = (fresh as PrayerTimesResult.Success).times
            // Fresh timestamps must differ from the stale cache
            assertTrue(freshTimes.all { it.prayer !in listOf(Prayer.SHURUQ) || it.timestamp != 1_999_999L })
        }

    @Test
    fun `5C_3 — clearAllCaches then fetch recalculates from scratch`() =
        runTest {
            val cityKey = "SA_Mecca"
            val todayLabel = todayLabelUtc()
            val config = CityConfig("Mecca", "SA", "UTC", 21.42, 39.83)

            // Cache some data for two cities
            database.prayerTimeDao().insertAll(
                listOf(
                    entity("FAJR", "05:00", 1L, todayLabel, cityKey),
                    entity("FAJR", "05:00", 1L, todayLabel, "DE_Berlin"),
                ),
            )

            engine.clearAllPrayerTimeCache()

            val mecca = database.prayerTimeDao().getByCityAndDate(cityKey, todayLabel)
            val berlin = database.prayerTimeDao().getByCityAndDate("DE_Berlin", todayLabel)
            assertTrue("All cache must be cleared", mecca.isEmpty() && berlin.isEmpty())

            // After clearing, fresh fetch should compute new times
            val fresh = engine.fetchLocalTimes(config, cityKey, todayLabel, cache = null)
            assertTrue("Fresh calc after clear-all must succeed", fresh is PrayerTimesResult.Success)
        }
}
