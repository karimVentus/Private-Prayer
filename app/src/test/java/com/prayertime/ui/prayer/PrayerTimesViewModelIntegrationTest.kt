package com.prayertime.ui.prayer

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.prayertime.data.LocationDataSourceTestSupport
import com.prayertime.data.local.AppDatabase
import com.prayertime.data.local.AppPreferencesDataSource
import com.prayertime.data.local.InMemoryCityConfigDataSource
import com.prayertime.data.repository.LocalLocationRepository
import com.prayertime.data.repository.LocalPrayerTimesRepository
import com.prayertime.domain.model.CityConfig
import com.prayertime.domain.model.Prayer
import com.prayertime.domain.model.SaveCityResult
import com.prayertime.notification.AdhanAlertDeliverer
import com.prayertime.testing.clearViewModelForTest
import com.prayertime.widget.WidgetUpdater
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.Executor

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PrayerTimesViewModelIntegrationTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var database: AppDatabase
    private lateinit var citySource: InMemoryCityConfigDataSource
    private lateinit var repository: LocalPrayerTimesRepository
    private val widgetUpdater = mockk<WidgetUpdater>(relaxed = true)
    private val locationRepository = LocalLocationRepository()
    private val preferences =
        mockk<AppPreferencesDataSource> {
            every { appLanguageTag } returns flowOf(null)
            coEvery { readAppLanguageTagOnce() } returns null
        }
    private val adhanAlertDeliverer = mockk<AdhanAlertDeliverer>(relaxed = true)
    private var vm: PrayerTimesViewModel? = null

    @Before
    fun setup() {
        Dispatchers.resetMain()
        Dispatchers.setMain(testDispatcher)
        LocationDataSourceTestSupport.initializeFromTestResource()
        citySource = InMemoryCityConfigDataSource()
        val syncExecutor = Executor { command -> command.run() }
        database =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                AppDatabase::class.java,
            ).setQueryExecutor(syncExecutor)
                .setTransactionExecutor(syncExecutor)
                .allowMainThreadQueries()
                .build()
        repository = LocalPrayerTimesRepository(citySource, database)
    }

    @After
    fun teardown() {
        vm?.clearViewModelForTest()
        vm = null
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun `loads six calculated prayer times from real offline repository`() {
        vm = PrayerTimesViewModel(repository, locationRepository, preferences, adhanAlertDeliverer, widgetUpdater)
        runBlocking {
            val saved =
                repository.saveCityConfig(
                    CityConfig("Hameln", "DE", "Europe/Berlin"),
                )
            assertTrue(saved is SaveCityResult.Success)
        }

        val state = vm!!.uiState.value
        assertTrue("Expected Success but was $state", state is PrayerTimesUiState.Success)
        val success = state as PrayerTimesUiState.Success
        assertEquals("Hameln, Germany", success.city)
        assertEquals("Europe/Berlin", success.timezone)
        assertEquals(6, success.result.times.size)
        assertEquals(Prayer.FAJR, success.result.times.first().prayer)
        assertEquals(Prayer.ISHA, success.result.times.last().prayer)
        assertTrue(success.result.countdown >= 0)
        assertTrue(vm!!.liveCountdown.value != null)
    }
}
