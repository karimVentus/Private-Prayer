package com.prayertime.ui.city

import androidx.test.core.app.ApplicationProvider
import com.prayertime.data.LocationDataSource
import com.prayertime.data.local.AppPreferencesDataSource
import com.prayertime.data.local.InMemoryCityConfigDataSource
import com.prayertime.data.repository.LocalLocationRepository
import com.prayertime.domain.usecase.SearchLocationsUseCase
import com.prayertime.testing.FakePrayerTimesRepository
import com.prayertime.testing.clearViewModelForTest
import com.prayertime.widget.WidgetUpdater
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CitySetupViewModelAssetLoadTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val activeViewModels = mutableListOf<CitySetupViewModel>()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        LocationDataSource.resetForTests()
    }

    @After
    fun teardown() {
        activeViewModels.forEach { it.clearViewModelForTest() }
        activeViewModels.clear()
        Dispatchers.resetMain()
        LocationDataSource.resetForTests()
    }

    @Test
    fun `filteredCountries loads from packaged assets after awaitReady`() =
        runTest(testDispatcher) {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            LocationDataSource.initialize(context)
            val locationRepository = LocalLocationRepository()
            val vm =
                CitySetupViewModel(
                    FakePrayerTimesRepository.forCitySetup(InMemoryCityConfigDataSource()),
                    locationRepository,
                    SearchLocationsUseCase(locationRepository),
                    mockk<AppPreferencesDataSource> {
                        every { appLanguageTag } returns flowOf(null)
                    },
                    mockk<WidgetUpdater>(relaxed = true),
                ).also { activeViewModels.add(it) }

            advanceUntilIdle()

            assertTrue(vm.catalogReady.value)
            assertTrue(
                "Expected countries from assets, got ${vm.filteredCountries.value.size}",
                vm.filteredCountries.value.size >= 150,
            )
        }
}
