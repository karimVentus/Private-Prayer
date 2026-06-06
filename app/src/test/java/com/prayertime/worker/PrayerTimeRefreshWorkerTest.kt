package com.prayertime.worker

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.prayertime.data.local.AppPreferencesDataSource
import com.prayertime.domain.model.CityConfig
import com.prayertime.domain.model.FetchError
import com.prayertime.domain.model.PrayerTimesResult
import com.prayertime.testing.FakePrayerTimesRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PrayerTimeRefreshWorkerTest {
    private lateinit var context: Context
    private lateinit var fakeRepo: FakePrayerTimesRepository
    private lateinit var preferences: AppPreferencesDataSource
    private val adhanEnabled = MutableStateFlow(false)

    @Before
    fun setup() {
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        context = ApplicationProvider.getApplicationContext()
        // Grant POST_NOTIFICATIONS so areNotificationsAllowed() returns true even when
        // AdhanNotificationHelperTest (which runs earlier) has denied it in the shared shadow.
        Shadows.shadowOf(context.applicationContext as Application)
            .grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        preferences = mockk(relaxed = true)
        every { preferences.adhanNotificationsEnabled } returns adhanEnabled
        every { preferences.adhanSound } returns flowOf("adhan")
        coEvery { preferences.setAdhanNotificationsEnabled(any()) } coAnswers {
            adhanEnabled.value = firstArg()
        }
        fakeRepo = FakePrayerTimesRepository.forWorker()
    }

    @After
    fun tearDown() {
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    }

    private fun buildWorker(): PrayerTimeRefreshWorker =
        TestListenableWorkerBuilder<PrayerTimeRefreshWorker>(context)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ): ListenableWorker? =
                        if (workerClassName == PrayerTimeRefreshWorker::class.java.name) {
                            PrayerTimeRefreshWorker(appContext, workerParameters, fakeRepo, preferences)
                        } else {
                            null
                        }
                },
            ).build()

    @Test
    fun `doWork fetches today times when city config exists`() =
        runBlocking {
            fakeRepo.workerCityConfig!!.value = CityConfig("Mecca", "SA", "Asia/Riyadh", 21.42, 39.83)
            fakeRepo.fetchResult = FakePrayerTimesRepository.defaultSuccessResult()
            buildWorker().doWork()
            assertTrue(fakeRepo.fetchInvoked)
        }

    @Test
    fun `doWork returns success when city config exists and fetch succeeds`() =
        runBlocking {
            fakeRepo.workerCityConfig!!.value = CityConfig("Mecca", "SA", "Asia/Riyadh", 21.42, 39.83)
            fakeRepo.fetchResult = FakePrayerTimesRepository.defaultSuccessResult()
            val result = buildWorker().doWork()
            assertEquals(ListenableWorker.Result.success(), result)
        }

    @Test
    fun `doWork returns success when no city config is saved`() =
        runBlocking {
            fakeRepo.workerCityConfig!!.value = null
            val result = buildWorker().doWork()
            assertEquals(ListenableWorker.Result.success(), result)
        }

    @Test
    fun `doWork returns retry when fetch fails with error`() =
        runBlocking {
            fakeRepo.workerCityConfig!!.value = CityConfig("Mecca", "SA", "Asia/Riyadh", 21.42, 39.83)
            fakeRepo.fetchResult = PrayerTimesResult.Error(FetchError.NETWORK)
            val result = buildWorker().doWork()
            assertEquals(ListenableWorker.Result.retry(), result)
        }

    @Test
    fun `schedules adhan alarms when adhan is enabled`() =
        runBlocking {
            adhanEnabled.value = true
            fakeRepo.workerCityConfig!!.value = CityConfig("Mecca", "SA", "Asia/Riyadh", 21.42, 39.83)
            fakeRepo.fetchResult = FakePrayerTimesRepository.defaultSuccessResult()
            val shadowAlarm = shadowAlarmManager()
            val before = shadowAlarm.scheduledAlarms.size
            buildWorker().doWork()
            assertTrue(shadowAlarm.scheduledAlarms.size > before)
        }

    @Test
    fun `does NOT schedule alarms when adhan is disabled`() =
        runBlocking {
            adhanEnabled.value = false
            fakeRepo.workerCityConfig!!.value = CityConfig("Mecca", "SA", "Asia/Riyadh", 21.42, 39.83)
            fakeRepo.fetchResult = FakePrayerTimesRepository.defaultSuccessResult()
            val shadowAlarm = shadowAlarmManager()
            val before = shadowAlarm.scheduledAlarms.size
            buildWorker().doWork()
            assertEquals(before, shadowAlarm.scheduledAlarms.size)
        }

    @Test
    fun `skips alarm scheduling when city config is null`() =
        runBlocking {
            adhanEnabled.value = true
            fakeRepo.workerCityConfig!!.value = null
            val shadowAlarm = shadowAlarmManager()
            val before = shadowAlarm.scheduledAlarms.size
            buildWorker().doWork()
            assertEquals(before, shadowAlarm.scheduledAlarms.size)
        }

    private fun shadowAlarmManager(): ShadowAlarmManager = Shadows.shadowOf(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
}
