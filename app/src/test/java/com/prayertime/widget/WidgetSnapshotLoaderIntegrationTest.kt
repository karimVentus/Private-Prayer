package com.prayertime.widget

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.prayertime.data.LocationDataSourceTestSupport
import com.prayertime.data.local.AppDatabase
import com.prayertime.data.local.AppPreferencesDataSource
import com.prayertime.data.local.InMemoryCityConfigDataSource
import com.prayertime.data.repository.LocalLocationRepository
import com.prayertime.data.repository.LocalPrayerTimesRepository
import com.prayertime.domain.calculator.PrayerTimeCalculator
import com.prayertime.domain.model.CityConfig
import com.prayertime.domain.model.PrayerTimesResult
import com.prayertime.domain.model.SaveCityResult
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetSnapshotLoaderIntegrationTest {
    private lateinit var database: AppDatabase
    private lateinit var citySource: InMemoryCityConfigDataSource
    private lateinit var repository: LocalPrayerTimesRepository
    private lateinit var loader: WidgetSnapshotLoader

    @Before
    fun setup() {
        LocationDataSourceTestSupport.initializeFromTestResource()
        citySource = InMemoryCityConfigDataSource()
        database =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                AppDatabase::class.java,
            ).allowMainThreadQueries().build()
        repository = LocalPrayerTimesRepository(citySource, database)
        val preferences = AppPreferencesDataSource(ApplicationProvider.getApplicationContext())
        loader = WidgetSnapshotLoader(repository, preferences, LocalLocationRepository())
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `load returns ready snapshot with calculated times`() =
        runTest {
            val saved =
                repository.saveCityConfig(
                    CityConfig("Hameln", "DE", "Europe/Berlin"),
                )
            assertTrue(saved is SaveCityResult.Success)

            val snapshot = loader.load()

            assertEquals(WidgetSnapshot.State.READY, snapshot.state)
            assertEquals("Hameln, Germany", snapshot.cityLabel)
            assertEquals("Europe/Berlin", snapshot.timezone)
            assertEquals(6, snapshot.times.size)
            assertNotNull(snapshot.nextPrayer)
            assertNotNull(snapshot.hijriDate)
            assertTrue(snapshot.countdownMillis >= 0)

            val expected =
                PrayerTimeCalculator.buildResult(
                    snapshot.times,
                    timezone = TimeZone.getTimeZone(snapshot.timezone),
                ) as PrayerTimesResult.Success
            assertEquals(expected.nextPrayer, snapshot.nextPrayer)
        }

    @Test
    fun `load chains through builder for all widget sizes`() =
        runTest {
            repository.saveCityConfig(CityConfig("Hameln", "DE", "Europe/Berlin"))
            val snapshot = loader.load()
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val preferences = AppPreferencesDataSource(context)
            val builder = WidgetRemoteViewsBuilder(context, preferences)

            WidgetSize.entries.forEach { size ->
                val views = builder.build(snapshot, size)
                assertNotNull(views)
            }
        }
}
