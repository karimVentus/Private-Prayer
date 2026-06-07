package com.prayertime.ui.city

import com.prayertime.data.LocationDataSourceTestSupport
import com.prayertime.data.local.AppPreferencesDataSource
import com.prayertime.data.local.InMemoryCityConfigDataSource
import com.prayertime.data.repository.LocalLocationRepository
import com.prayertime.domain.model.Country
import com.prayertime.domain.repository.LocationRepository
import com.prayertime.domain.usecase.SearchLocationsUseCase
import com.prayertime.testing.FakePrayerTimesRepository
import com.prayertime.testing.clearViewModelForTest
import com.prayertime.widget.WidgetUpdater
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CitySetupViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var citySource: InMemoryCityConfigDataSource
    private lateinit var repository: FakePrayerTimesRepository
    private lateinit var locationRepository: LocationRepository
    private val widgetUpdater = mockk<WidgetUpdater>(relaxed = true)
    private val preferences =
        mockk<AppPreferencesDataSource> {
            every { appLanguageTag } returns flowOf(null)
        }
    private val activeViewModels = mutableListOf<CitySetupViewModel>()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        LocationDataSourceTestSupport.initializeFromTestResource()
        locationRepository = LocalLocationRepository()
        citySource = InMemoryCityConfigDataSource()
        repository = FakePrayerTimesRepository.forCitySetup(citySource)
    }

    @After
    fun teardown() {
        activeViewModels.forEach { it.clearViewModelForTest() }
        activeViewModels.clear()
        Dispatchers.resetMain()
    }

    private fun viewModel(): CitySetupViewModel =
        CitySetupViewModel(
            repository,
            locationRepository,
            SearchLocationsUseCase(locationRepository),
            preferences,
            widgetUpdater,
        ).also {
            activeViewModels.add(it)
        }

    @Test
    fun `default wizard step is CountrySelection`() =
        runTest(testDispatcher) {
            assertEquals(WizardStep.CountrySelection, viewModel().wizardStep.value)
        }

    @Test
    fun `country search query filters countries`() =
        runTest(testDispatcher) {
            val vm = viewModel()
            vm.onCountrySearchQueryChanged("Sy")
            val filtered = vm.filteredCountries.value
            assertTrue(filtered.any { it.name == "Syria" })
            assertTrue(filtered.all { it.name.contains("Sy", ignoreCase = true) })
        }

    @Test
    fun `empty country search query returns at least 150 countries`() =
        runTest(testDispatcher) {
            val vm = viewModel()
            vm.onCountrySearchQueryChanged("")
            assertTrue(vm.filteredCountries.value.size >= 150)
        }

    @Test
    fun `selecting country advances wizard to CitySelection`() =
        runTest(testDispatcher) {
            val vm = viewModel()
            vm.selectCountry(Country("Syria", "SY"))
            val step = vm.wizardStep.value
            assertTrue(step is WizardStep.CitySelection)
            assertEquals("SY", (step as WizardStep.CitySelection).country.code)
        }

    @Test
    fun `city search query filters cities`() =
        runTest(testDispatcher) {
            val vm = viewModel()
            vm.selectCountry(Country("Syria", "SY"))
            vm.onCitySearchQueryChanged("Dam")
            val filtered = vm.filteredCities.value
            assertTrue(filtered.any { it.canonicalName == "Damascus" })
            assertFalse(filtered.any { it.canonicalName == "Aleppo" })
            assertTrue(
                filtered.all {
                    it.canonicalName.contains("Dam", ignoreCase = true) ||
                        it.displayName.contains("Dam", ignoreCase = true)
                },
            )
        }

    @Test
    fun `clearSelectedCountry returns to country selection`() =
        runTest(testDispatcher) {
            val vm = viewModel()
            vm.selectCountry(Country("Syria", "SY"))
            vm.clearSelectedCountry()
            assertTrue(vm.wizardStep.value is WizardStep.CountrySelection)
        }

    @Test
    fun `custom city fallback shown when city query is not blank`() =
        runTest(testDispatcher) {
            val vm = viewModel()
            vm.selectCountry(Country("Syria", "SY"))
            assertFalse(vm.showCustomCityFallback)
            vm.onCitySearchQueryChanged("CustomTown")
            assertTrue(vm.showCustomCityFallback)
        }

    @Test
    fun `saveCity resolves Arabic display name to canonical coords`() =
        runTest(testDispatcher) {
            val arPreferences =
                mockk<AppPreferencesDataSource> {
                    every { appLanguageTag } returns flowOf("ar")
                }
            val vm =
                CitySetupViewModel(
                    repository,
                    locationRepository,
                    SearchLocationsUseCase(locationRepository),
                    arPreferences,
                    widgetUpdater,
                ).also { activeViewModels.add(it) }
            vm.selectCountry(Country("Syria", "SY"))
            vm.saveCity("دمشق")
            advanceUntilIdle()
            val config = citySource.cityConfig.first()
            assertEquals("Damascus", config?.cityName)
            assertEquals("Asia/Damascus", config?.timezone)
            assertEquals(33.513, config?.latitude!!, 0.001)
        }

    @Test
    fun `saveCity persists enriched config`() =
        runTest(testDispatcher) {
            val vm = viewModel()
            vm.selectCountry(Country("Syria", "SY"))
            vm.saveCity("Damascus")
            advanceUntilIdle()
            val config = citySource.cityConfig.first()
            assertEquals("Damascus", config?.cityName)
            assertEquals("SY", config?.countryCode)
            assertEquals("Asia/Damascus", config?.timezone)
            assertEquals(33.513, config?.latitude!!, 0.001)
        }

    @Test
    fun `saveCity offline rejects unknown city before repository`() =
        runTest(testDispatcher) {
            citySource.setOfflineOnly(true)
            val vm = viewModel()
            vm.selectCountry(Country("Germany", "DE"))
            vm.saveCity("NonExistentVillage")
            advanceUntilIdle()
            assertEquals(null, citySource.cityConfig.first())
            assertEquals(com.prayertime.R.string.error_save_city_not_found, vm.saveError.value)
        }

    @Test
    fun `saveCity passes catalog timezone not UTC placeholder`() =
        runTest(testDispatcher) {
            val vm = viewModel()
            vm.selectCountry(Country("Germany", "DE"))
            vm.saveCity("Hameln")
            advanceUntilIdle()
            val config = citySource.cityConfig.first()
            assertEquals("Europe/Berlin", config?.timezone)
        }

    @Test
    fun `saveCity does nothing when no country selected`() =
        runTest(testDispatcher) {
            val vm = viewModel()
            vm.saveCity("Damascus")
            advanceUntilIdle()
            assertEquals(null, citySource.cityConfig.first())
        }

    @Test
    fun `saveCity network error exposes online message when network mode enabled`() =
        runTest(testDispatcher) {
            val failingRepo = FakePrayerTimesRepository.failingSave(citySource)
            val vm =
                CitySetupViewModel(
                    failingRepo,
                    locationRepository,
                    SearchLocationsUseCase(locationRepository),
                    preferences,
                    widgetUpdater,
                ).also {
                    activeViewModels.add(it)
                }
            citySource.setOfflineOnly(false)
            vm.selectCountry(Country("Syria", "SY"))
            vm.saveCity("Damascus")
            advanceUntilIdle()

            assertEquals(com.prayertime.R.string.error_save_network_online, vm.saveError.value)
        }
}
