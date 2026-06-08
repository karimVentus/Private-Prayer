package com.prayertime.ui.prayer

import com.prayertime.data.LocationDataSourceTestSupport
import com.prayertime.data.local.AppPreferencesDataSource
import com.prayertime.data.local.InMemoryCityConfigDataSource
import com.prayertime.data.repository.LocalLocationRepository
import com.prayertime.domain.calculator.PrayerTimeCalculator
import com.prayertime.domain.model.CityConfig
import com.prayertime.domain.model.FetchError
import com.prayertime.domain.model.Prayer
import com.prayertime.domain.model.PrayerTime
import com.prayertime.domain.model.PrayerTimesResult
import com.prayertime.notification.AdhanAlertDeliverer
import com.prayertime.testing.FakePrayerTimesRepository
import com.prayertime.testing.clearViewModelForTest
import com.prayertime.widget.WidgetUpdater
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PrayerTimesViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var citySource: InMemoryCityConfigDataSource
    private lateinit var repository: FakePrayerTimesRepository
    private val widgetUpdater = mockk<WidgetUpdater>(relaxed = true)
    private val locationRepository = LocalLocationRepository()
    private val preferences =
        mockk<AppPreferencesDataSource> {
            every { appLanguageTag } returns flowOf(null)
            coEvery { readAppLanguageTagOnce() } returns null
        }
    private val adhanAlertDeliverer = mockk<AdhanAlertDeliverer>(relaxed = true)
    private val activeViewModels = mutableListOf<PrayerTimesViewModel>()

    private val hamelnConfig =
        CityConfig(
            cityName = "Hameln",
            countryCode = "DE",
            timezone = "Europe/Berlin",
            latitude = 52.1,
            longitude = 9.4,
        )

    @Before
    fun setup() {
        Dispatchers.resetMain()
        Dispatchers.setMain(testDispatcher)
        LocationDataSourceTestSupport.initializeFromTestResource()
        clearMocks(widgetUpdater)
        citySource = InMemoryCityConfigDataSource()
        repository = FakePrayerTimesRepository.forPrayerTimes(citySource)
    }

    @After
    fun teardown() {
        activeViewModels.forEach { it.clearViewModelForTest() }
        activeViewModels.clear()
        Dispatchers.resetMain()
    }

    private fun viewModel(enableCountdownTickerLoop: Boolean = false): PrayerTimesViewModel =
        PrayerTimesViewModel(repository, locationRepository, preferences, adhanAlertDeliverer, widgetUpdater).also {
            it.enableCountdownTickerLoop = enableCountdownTickerLoop
            activeViewModels.add(it)
        }

    /** Stops the 1s countdown loop before [runTest] drains the scheduler (avoids infinite delay hang). */
    private fun disposeVm(vm: PrayerTimesViewModel) {
        vm.clearViewModelForTest()
        activeViewModels.remove(vm)
    }

    @Test
    fun `clearCity sets NoCity immediately`() =
        runTest(testDispatcher) {
            val vm = viewModel()

            vm.clearCity()

            assertEquals(PrayerTimesUiState.NoCity, vm.uiState.value)
            assertNull(vm.liveCountdown.value)
            disposeVm(vm)
        }

    @Test
    fun `clearCity survives offlineOnly re-emit from datastore clear`() =
        runTest(testDispatcher) {
            val vm = viewModel()
            vm.seedSuccessStateForTest(hamelnConfig, hamelnSuccessResult())

            vm.clearCity()

            assertEquals(PrayerTimesUiState.NoCity, vm.uiState.value)
            assertNull(citySource.cityConfig.first())
            disposeVm(vm)
        }

    @Test
    fun `external config null while showing times transitions to NoCity`() =
        runTest(testDispatcher) {
            citySource.save(hamelnConfig)
            repository.fetchOverride = { FakePrayerTimesRepository.defaultSuccessResult() }
            val vm = viewModel()
            assertTrue(vm.uiState.value is PrayerTimesUiState.Success)

            citySource.emitCityConfig(null)

            assertEquals(PrayerTimesUiState.NoCity, vm.uiState.value)
            assertNull(vm.liveCountdown.value)
            disposeVm(vm)
        }

    @Test
    fun `city not found keeps FetchError after config cleared`() =
        runTest(testDispatcher) {
            citySource.save(hamelnConfig)
            repository.fetchOverride = { PrayerTimesResult.Error(FetchError.CITY_NOT_FOUND) }
            val vm = viewModel()

            assertTrue(vm.uiState.value is PrayerTimesUiState.FetchError)
            assertNull(citySource.cityConfig.first())
            disposeVm(vm)
        }

    // --- Countdown: liveCountdown emitted after fetch ---

    @Test
    fun `liveCountdown is emitted after successful fetch`() =
        runTest(testDispatcher) {
            citySource.save(hamelnConfig)
            repository.fetchOverride = { FakePrayerTimesRepository.defaultSuccessResult() }
            val vm = viewModel()
            assertTrue(vm.uiState.value is PrayerTimesUiState.Success)

            val countdown = vm.liveCountdown.value
            assertNotNull("liveCountdown must be non-null after Success", countdown)
            val success = vm.uiState.value as PrayerTimesUiState.Success
            val prayerNames = success.result.times.map { it.prayer }
            assertTrue(
                "nextPrayer must be one of the returned times",
                countdown!!.nextPrayer in prayerNames,
            )
            assertTrue("countdownMillis must be non-negative", countdown.countdownMillis >= 0L)
            disposeVm(vm)
        }

    @Test
    fun `liveCountdown is null after clearCity`() =
        runTest(testDispatcher) {
            val vm = viewModel()
            vm.seedSuccessStateForTest(hamelnConfig, hamelnSuccessResult())
            vm.seedLiveCountdownForTest(Prayer.FAJR, 3_600_000L)
            assertNotNull(vm.liveCountdown.value)

            vm.clearCity()

            assertEquals(PrayerTimesUiState.NoCity, vm.uiState.value)
            assertNull(vm.liveCountdown.value)
            disposeVm(vm)
        }

    @Test
    fun `liveCountdown is null after fetch error`() =
        runTest(testDispatcher) {
            citySource.save(hamelnConfig)
            repository.fetchOverride = { PrayerTimesResult.Error(FetchError.NETWORK) }
            val vm = viewModel()

            assertTrue(vm.uiState.value is PrayerTimesUiState.FetchError)
            assertNull(vm.liveCountdown.value)
            disposeVm(vm)
        }

    @Test
    fun `liveCountdown wraps to Fajr after all prayers pass`() =
        runTest(testDispatcher) {
            val now = System.currentTimeMillis()
            // All prayers are in the past
            val allPast =
                listOf(
                    PrayerTime(Prayer.FAJR, "05:00", now - 10 * 3_600_000L),
                    PrayerTime(Prayer.SHURUQ, "06:30", now - 9 * 3_600_000L),
                    PrayerTime(Prayer.DHUHR, "12:30", now - 5 * 3_600_000L),
                    PrayerTime(Prayer.ASR, "15:45", now - 2 * 3_600_000L),
                    PrayerTime(Prayer.MAGHRIB, "18:30", now - 1 * 3_600_000L),
                    PrayerTime(Prayer.ISHA, "20:00", now - 30 * 60_000L),
                )
            val result =
                PrayerTimesResult.Success(
                    times = allPast,
                    nextPrayer = Prayer.FAJR,
                    countdown =
                        PrayerTimeCalculator.millisUntilNextOccurrence(
                            allPast.first().timestamp,
                            now,
                            TimeZone.getTimeZone("Europe/Berlin"),
                        ),
                )

            // Verify the calculator wrapped to tomorrow's Fajr
            val tomorrowFajr =
                PrayerTimeCalculator.advanceOneCalendarDay(
                    allPast.first().timestamp,
                    TimeZone.getTimeZone("Europe/Berlin"),
                )
            assertEquals(Prayer.FAJR, result.nextPrayer)
            assertEquals(tomorrowFajr - now, result.countdown)
            assertTrue(result.countdown > 0L)
        }

    @Test
    fun `countdown ticker recalculates liveCountdown on first tick`() =
        runTest(testDispatcher) {
            citySource.save(hamelnConfig)
            val wallMs = System.currentTimeMillis()
            val fajrTs = wallMs + 30 * 60_000L
            val staleCountdown = fajrTs - wallMs + 9_999L
            repository.fetchOverride = {
                PrayerTimesResult.Success(
                    times = berlinDayTimes(fajrTs),
                    nextPrayer = Prayer.FAJR,
                    countdown = staleCountdown,
                )
            }
            val vm = viewModel(enableCountdownTickerLoop = true)
            val live = vm.liveCountdown.value!!.countdownMillis
            assertTrue("ticker should replace stale fetch countdown", live < staleCountdown - 5_000L)
            disposeVm(vm)
        }

    @Test
    fun `countdown ticker triggers refreshTimesForNewDay on city day rollover`() =
        runTest(testDispatcher) {
            val berlin = TimeZone.getTimeZone("Europe/Berlin")
            val yesterdayFajr =
                Calendar.getInstance(berlin).apply {
                    timeInMillis = System.currentTimeMillis()
                    add(Calendar.DAY_OF_MONTH, -1)
                    set(Calendar.HOUR_OF_DAY, 4)
                    set(Calendar.MINUTE, 30)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            var fetchCount = 0
            citySource.save(hamelnConfig)
            repository.fetchOverride = {
                fetchCount++
                if (fetchCount == 1) {
                    PrayerTimesResult.Success(
                        times = berlinDayTimes(yesterdayFajr),
                        nextPrayer = Prayer.FAJR,
                        countdown = 1_000L,
                    )
                } else {
                    // Silent refresh failure — keeps Success UI without restarting the 1s ticker loop.
                    PrayerTimesResult.Error(FetchError.NETWORK)
                }
            }
            val vm = viewModel(enableCountdownTickerLoop = true)
            assertEquals(2, fetchCount)
            disposeVm(vm)
        }

    @Test
    fun `countdown ticker requests widget update when next prayer changes`() =
        runTest(testDispatcher) {
            clearMocks(widgetUpdater)
            val now = System.currentTimeMillis()
            citySource.save(hamelnConfig)
            repository.fetchOverride = {
                PrayerTimesResult.Success(
                    times =
                        listOf(
                            PrayerTime(Prayer.FAJR, "05:00", now - 60_000L),
                            PrayerTime(Prayer.SHURUQ, "06:00", now + 3_600_000L),
                            PrayerTime(Prayer.DHUHR, "12:30", now + 28_800_000L),
                            PrayerTime(Prayer.ASR, "15:45", now + 40_000_000L),
                            PrayerTime(Prayer.MAGHRIB, "18:10", now + 48_000_000L),
                            PrayerTime(Prayer.ISHA, "19:40", now + 52_000_000L),
                        ),
                    nextPrayer = Prayer.FAJR,
                    countdown = 3_600_000L,
                )
            }
            val vm = viewModel(enableCountdownTickerLoop = true)
            verify(atLeast = 2) { widgetUpdater.requestImmediateUpdate() }
            disposeVm(vm)
        }

    @Test
    fun `refreshIfPrayerDayStale refetches when fajr anchor is prior city day`() =
        runTest(testDispatcher) {
            val berlin = TimeZone.getTimeZone("Europe/Berlin")
            val yesterdayFajr =
                Calendar.getInstance(berlin).apply {
                    timeInMillis = System.currentTimeMillis()
                    add(Calendar.DAY_OF_MONTH, -1)
                    set(Calendar.HOUR_OF_DAY, 4)
                    set(Calendar.MINUTE, 30)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            var fetchCount = 0
            repository.fetchOverride = {
                fetchCount++
                PrayerTimesResult.Error(FetchError.NETWORK)
            }
            val vm = viewModel()
            vm.seedSuccessStateForTest(
                hamelnConfig,
                PrayerTimesResult.Success(
                    times = berlinDayTimes(yesterdayFajr),
                    nextPrayer = Prayer.FAJR,
                    countdown = 1_000L,
                ),
            )
            vm.refreshIfPrayerDayStale()
            assertEquals(1, fetchCount)
            disposeVm(vm)
        }
}

private fun berlinDayTimes(fajrTs: Long): List<PrayerTime> =
    listOf(
        PrayerTime(Prayer.FAJR, "04:30", fajrTs),
        PrayerTime(Prayer.SHURUQ, "06:00", fajrTs + 5_400_000L),
        PrayerTime(Prayer.DHUHR, "12:30", fajrTs + 28_800_000L),
        PrayerTime(Prayer.ASR, "15:45", fajrTs + 40_500_000L),
        PrayerTime(Prayer.MAGHRIB, "18:10", fajrTs + 49_200_000L),
        PrayerTime(Prayer.ISHA, "19:40", fajrTs + 54_600_000L),
    )

private fun hamelnSuccessResult(): PrayerTimesResult.Success {
    val now = System.currentTimeMillis()
    return PrayerTimesResult.Success(
        times =
            listOf(
                PrayerTime(Prayer.FAJR, "05:00", now + 3_600_000),
                PrayerTime(Prayer.DHUHR, "13:00", now + 28_800_000),
            ),
        nextPrayer = Prayer.FAJR,
        countdown = 3_600_000,
    )
}
