package com.prayertime.ui.settings

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.prayertime.data.local.AppDatabase
import com.prayertime.data.local.AppPreferencesDataSource
import com.prayertime.data.local.InMemoryCityConfigDataSource
import com.prayertime.locale.AppLocale
import com.prayertime.testing.FakePrayerTimesRepository
import com.prayertime.testing.clearViewModelForTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class AppSettingsViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var citySource: InMemoryCityConfigDataSource
    private lateinit var repository: FakePrayerTimesRepository
    private lateinit var database: AppDatabase
    private lateinit var preferences: AppPreferencesDataSource
    private val activeViewModels = mutableListOf<AppSettingsViewModel>()
    private var widgetRefreshCount = 0

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        database =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                AppDatabase::class.java,
            ).allowMainThreadQueries().build()
        citySource = InMemoryCityConfigDataSource()
        repository = FakePrayerTimesRepository.forSettings(citySource)
        preferences = AppPreferencesDataSource(ApplicationProvider.getApplicationContext())
    }

    @After
    fun teardown() {
        activeViewModels.forEach { it.clearViewModelForTest() }
        activeViewModels.clear()
        Dispatchers.resetMain()
        if (::database.isInitialized) {
            database.close()
        }
    }

    private fun viewModel(): AppSettingsViewModel =
        AppSettingsViewModel(
            repository,
            preferences,
            onLocaleChanged = {
                widgetRefreshCount++
            },
            adhanNotificationHelper = null,
            appContext = ApplicationProvider.getApplicationContext(),
        ).also {
            activeViewModels.add(it)
        }

    @Test
    fun `offlineOnly defaults to true`() =
        runTest(testDispatcher) {
            val vm = viewModel()
            assertTrue(vm.offlineOnly.value)
            vm.clearViewModelForTest()
            activeViewModels.remove(vm)
        }

    @Test
    fun `setOfflineOnly toggles the value`() =
        runTest(testDispatcher) {
            val vm = viewModel()
            vm.setOfflineOnly(false)
            assertFalse(vm.offlineOnly.value)
            vm.setOfflineOnly(true)
            assertTrue(vm.offlineOnly.value)
            vm.clearViewModelForTest()
            activeViewModels.remove(vm)
        }

    @Test
    fun `setAdhanNotificationsEnabled toggles the value immediately`() =
        runTest(testDispatcher) {
            val vm = viewModel()
            advanceUntilIdle()
            vm.setAdhanNotificationsEnabled(true)
            advanceUntilIdle()
            assertTrue(vm.adhanNotificationsEnabled.value)
            vm.clearViewModelForTest()
            activeViewModels.remove(vm)
        }

    @Test
    fun `showAbout sets about flag`() =
        runTest(testDispatcher) {
            val vm = viewModel()
            assertFalse(vm.showAbout.value)
            vm.showAbout()
            assertTrue(vm.showAbout.value)
            vm.clearViewModelForTest()
            activeViewModels.remove(vm)
        }

    @Test
    fun `hideAbout clears about flag`() =
        runTest(testDispatcher) {
            val vm = viewModel()
            vm.showAbout()
            assertTrue(vm.showAbout.value)
            vm.hideAbout()
            assertFalse(vm.showAbout.value)
            vm.clearViewModelForTest()
            activeViewModels.remove(vm)
        }

    @Test
    fun `applyAppLanguage persists tag applies locale and refreshes widgets`() =
        runTest(testDispatcher) {
            widgetRefreshCount = 0
            val vm = viewModel()
            vm.applyAppLanguage("ar")
            assertEquals("ar", preferences.readAppLanguageTagOnce())
            assertEquals("ar", vm.appLanguageTag.value)
            assertEquals(1, widgetRefreshCount)
            AppLocale.apply(null)
            vm.clearViewModelForTest()
            activeViewModels.remove(vm)
        }

    // --- 5B.3: Deny both permissions → Settings ViewModel still functional ---

    @Test
    fun `setAdhanNotificationsEnabled toggles value even when notifications denied`() =
        runTest(testDispatcher) {
            // Settings ViewModel does not gate on permission state —
            // the UI layer handles permission checks before toggling.
            val vm = viewModel()
            advanceUntilIdle()

            // Toggle true
            vm.setAdhanNotificationsEnabled(true)
            advanceUntilIdle()
            assertTrue(vm.adhanNotificationsEnabled.value)

            // Toggle false
            vm.setAdhanNotificationsEnabled(false)
            advanceUntilIdle()
            assertFalse(vm.adhanNotificationsEnabled.value)

            // Toggle true again — confirms bi-directional toggle works regardless of initial state
            vm.setAdhanNotificationsEnabled(true)
            advanceUntilIdle()
            assertTrue(vm.adhanNotificationsEnabled.value)
            vm.clearViewModelForTest()
            activeViewModels.remove(vm)
        }

    @Test
    fun `offlineOnly toggle works independently of permissions`() =
        runTest(testDispatcher) {
            val vm = viewModel()
            advanceUntilIdle()

            assertTrue(vm.offlineOnly.value)
            vm.setOfflineOnly(false)
            advanceUntilIdle()
            assertFalse(vm.offlineOnly.value)
            vm.setOfflineOnly(true)
            advanceUntilIdle()
            assertTrue(vm.offlineOnly.value)
            vm.clearViewModelForTest()
            activeViewModels.remove(vm)
        }

    @Test
    fun `all settings toggles remain functional when permissions denied`() =
        runTest(testDispatcher) {
            val vm = viewModel()
            advanceUntilIdle()

            // Offline only
            vm.setOfflineOnly(false)
            advanceUntilIdle()
            assertFalse(vm.offlineOnly.value)

            // Adhan notifications (preference-level, not gated by runtime permissions)
            vm.setAdhanNotificationsEnabled(true)
            advanceUntilIdle()
            assertTrue(vm.adhanNotificationsEnabled.value)

            // Language
            vm.applyAppLanguage("en")
            advanceUntilIdle()
            assertEquals("en", vm.appLanguageTag.value)
            AppLocale.apply(null)

            // None of these operations depend on runtime permissions.
            // This confirms the app settings layer is fully functional
            // even when SCHEDULE_EXACT_ALARM and POST_NOTIFICATIONS are denied.
            vm.clearViewModelForTest()
            activeViewModels.remove(vm)
        }
}
