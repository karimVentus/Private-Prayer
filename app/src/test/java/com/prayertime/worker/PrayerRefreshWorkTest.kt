package com.prayertime.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.prayertime.data.local.AppPreferencesDataSource
import com.prayertime.domain.model.CityConfig
import com.prayertime.domain.model.FetchError
import com.prayertime.domain.model.PrayerTimesResult
import com.prayertime.testing.FakePrayerTimesRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PrayerRefreshWorkTest {
    private lateinit var context: Context
    private lateinit var fakeRepo: FakePrayerTimesRepository
    private lateinit var preferences: AppPreferencesDataSource
    private lateinit var workerFactory: WorkerFactory

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        fakeRepo = FakePrayerTimesRepository.forWorker()
        preferences = AppPreferencesDataSource(context)
        workerFactory =
            object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ): ListenableWorker? =
                    if (workerClassName == PrayerTimeRefreshWorker::class.java.name) {
                        PrayerTimeRefreshWorker(
                            appContext,
                            workerParameters,
                            fakeRepo,
                            preferences,
                        )
                    } else {
                        null
                    }
            }
        val config =
            Configuration.Builder()
                .setExecutor(SynchronousExecutor())
                .setWorkerFactory(workerFactory)
                .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        val wm = WorkManager.getInstance(context)
        // Full suite shares one Robolectric process; KEEP would no-op over a cancelled unique work.
        wm.cancelUniqueWork(PrayerRefreshWork.UNIQUE_WORK_NAME).result.get()
        wm.pruneWork().result.get()
    }

    private fun workManager(): WorkManager = WorkManager.getInstance(context)

    private fun buildRefreshWorker(): PrayerTimeRefreshWorker =
        TestListenableWorkerBuilder<PrayerTimeRefreshWorker>(context)
            .setWorkerFactory(workerFactory)
            .build()

    @Test
    fun `enqueue registers unique periodic refresh work`() {
        PrayerRefreshWork.enqueue(context)

        assertPeriodicWorkEnqueued()
    }

    @Test
    fun `repeat enqueue keeps existing work instead of duplicating`() {
        PrayerRefreshWork.enqueue(context)
        val firstId =
            workManager()
                .getWorkInfosForUniqueWork(PrayerRefreshWork.UNIQUE_WORK_NAME)
                .get()[0]
                .id

        PrayerRefreshWork.enqueue(context)

        val infos =
            workManager()
                .getWorkInfosForUniqueWork(PrayerRefreshWork.UNIQUE_WORK_NAME)
                .get()
        assertEquals(1, infos.size)
        assertEquals(firstId, infos[0].id)
    }

    @Test
    fun `registered worker refreshes prayer data in background`() =
        runBlocking {
            fakeRepo.workerCityConfig!!.value = CityConfig("Hameln", "DE", "Europe/Berlin", 52.1, 9.35)
            fakeRepo.fetchResult = FakePrayerTimesRepository.defaultSuccessResult()

            PrayerRefreshWork.enqueue(context)
            assertWorkerRegistered()

            val result = buildRefreshWorker().doWork()
            assertEquals(ListenableWorker.Result.success(), result)
            assertTrue(fakeRepo.fetchInvoked)
        }

    @Test
    fun `registered worker retries gracefully when refresh fails`() =
        runBlocking {
            fakeRepo.workerCityConfig!!.value = CityConfig("Hameln", "DE", "Europe/Berlin", 52.1, 9.35)
            fakeRepo.fetchResult = PrayerTimesResult.Error(FetchError.NETWORK)

            PrayerRefreshWork.enqueue(context)
            assertWorkerRegistered()

            val result = buildRefreshWorker().doWork()
            assertEquals(ListenableWorker.Result.retry(), result)
            assertTrue(fakeRepo.fetchInvoked)
        }

    @Test
    fun `registered worker skips fetch when no city is configured`() =
        runBlocking {
            fakeRepo.workerCityConfig!!.value = null

            PrayerRefreshWork.enqueue(context)
            assertWorkerRegistered()

            val result = buildRefreshWorker().doWork()
            assertEquals(ListenableWorker.Result.success(), result)
            assertFalse(fakeRepo.fetchInvoked)
        }

    private fun assertPeriodicWorkEnqueued() {
        val infos =
            workManager()
                .getWorkInfosForUniqueWork(PrayerRefreshWork.UNIQUE_WORK_NAME)
                .get()
        assertEquals(1, infos.size)
        val state = infos[0].state
        assertTrue(
            "Expected active periodic work, got $state",
            state == WorkInfo.State.ENQUEUED ||
                state == WorkInfo.State.BLOCKED ||
                state == WorkInfo.State.RUNNING,
        )
        assertTrue(
            infos[0].tags.any { it.contains(PrayerTimeRefreshWorker::class.java.simpleName) },
        )
    }

    private fun assertWorkerRegistered() {
        val infos =
            workManager()
                .getWorkInfosForUniqueWork(PrayerRefreshWork.UNIQUE_WORK_NAME)
                .get()
        assertEquals(1, infos.size)
        assertTrue(
            infos[0].tags.any { it.contains(PrayerTimeRefreshWorker::class.java.simpleName) },
        )
    }
}
