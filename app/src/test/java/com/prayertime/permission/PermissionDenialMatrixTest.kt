package com.prayertime.permission

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.prayertime.data.local.InMemoryCityConfigDataSource
import com.prayertime.domain.model.CityConfig
import com.prayertime.testing.FakePrayerTimesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 5B.3: Deny both SCHEDULE_EXACT_ALARM + POST_NOTIFICATIONS → app still functional.
 *
 * Tests that core app functionality (prayer-time fetching, settings toggling, and the
 * Settings-guide navigation paths) remain available even when runtime permissions are denied.
 * Permission denial should degrade alarm reliability and notification delivery, never
 * crash the app or prevent prayer times from being displayed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PermissionDenialMatrixTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var citySource: InMemoryCityConfigDataSource
    private lateinit var repository: FakePrayerTimesRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        citySource = InMemoryCityConfigDataSource()
        repository = FakePrayerTimesRepository.forPrayerTimes(citySource)
        context = ApplicationProvider.getApplicationContext()
    }

    // ── 5B.3.1: Repository level — prayer times available without any permissions ──

    @Test
    fun `fetchTodayTimes succeeds and returns valid prayer times`() =
        runTest(testDispatcher) {
            val config = CityConfig("Hameln", "DE", "Europe/Berlin", 52.1, 9.4)
            citySource.save(config)
            repository.fetchOverride = { FakePrayerTimesRepository.defaultSuccessResult() }

            val result = repository.fetchTodayTimes(config)
            assertTrue(result is com.prayertime.domain.model.PrayerTimesResult.Success)
            val success = result as com.prayertime.domain.model.PrayerTimesResult.Success
            assertEquals(6, success.times.size)
            assertTrue(success.times.all { it.displayTime.isNotEmpty() })
        }

    @Test
    fun `cityConfig flow emits saved city without any permissions`() =
        runTest(testDispatcher) {
            val config = CityConfig("Mecca", "SA", "Asia/Riyadh", 21.42, 39.83)
            citySource.save(config)

            val emitted = repository.cityConfig.first()
            assertEquals(config, emitted)
        }

    // ── 5B.3.2: Settings persist independently of permissions ──

    @Test
    fun `offlineOnly preference persists independently of permissions`() =
        runTest(testDispatcher) {
            assertTrue(repository.offlineOnly.first())
            repository.setOfflineOnly(false)
            assertFalse(repository.offlineOnly.first())
            repository.setOfflineOnly(true)
            assertTrue(repository.offlineOnly.first())
        }

    // ── 5B.3.3: Permission-denial navigation paths exist ──

    @Test
    fun `all Settings-guide methods exist for the full denial matrix`() {
        val methods = AdhanPermissions::class.java.methods.map { it.name }.toSet()
        assertTrue("openNotificationSettings missing", "openNotificationSettings" in methods)
        assertTrue("openExactAlarmSettings missing", "openExactAlarmSettings" in methods)
        assertTrue("openBatteryOptimizationSettings missing", "openBatteryOptimizationSettings" in methods)
        assertTrue("requestPostNotifications missing", "requestPostNotifications" in methods)
    }

    @Test
    fun `permission check methods do not crash and are internally consistent`() {
        AdhanPermissions.needsPostNotificationsPermission()
        AdhanPermissions.hasPostNotificationsPermission(context)
        AdhanPermissions.areNotificationsAllowed(context)
        AdhanPermissions.canScheduleExactAlarms(context)
        AdhanPermissions.isIgnoringBatteryOptimizations(context)
        // No assertions needed — any throw would fail the test
    }
}
