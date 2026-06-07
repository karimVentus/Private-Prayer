package com.prayertime.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.prayertime.data.local.AppPreferencesDataSource
import com.prayertime.widget.WidgetPrayerBoundaryScheduler
import com.prayertime.widget.WidgetUpdater
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetUpdateWorkerTest {
    private lateinit var context: Context
    private lateinit var preferences: AppPreferencesDataSource
    private val widgetUpdater = mockk<WidgetUpdater>(relaxed = true)
    private val boundaryScheduler = mockk<WidgetPrayerBoundaryScheduler>(relaxed = true)

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        preferences = AppPreferencesDataSource(context)
        coEvery { widgetUpdater.updateAll() } returns Unit
        coEvery { boundaryScheduler.scheduleNextBoundaryUpdate(any()) } returns Unit
    }

    @Test
    fun doWork_updatesWidgetsThenSchedulesBoundaryAlarm() =
        runBlocking {
            buildWorker().doWork()

            coVerifyOrder {
                widgetUpdater.updateAll()
                boundaryScheduler.scheduleNextBoundaryUpdate(context)
            }
        }

    @Test
    fun doWork_returnsSuccess() =
        runBlocking {
            val result = buildWorker().doWork()
            assertEquals(ListenableWorker.Result.success(), result)
        }

    private fun buildWorker(): WidgetUpdateWorker =
        TestListenableWorkerBuilder<WidgetUpdateWorker>(context)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ): ListenableWorker? =
                        if (workerClassName == WidgetUpdateWorker::class.java.name) {
                            WidgetUpdateWorker(
                                appContext,
                                workerParameters,
                                preferences,
                                widgetUpdater,
                                boundaryScheduler,
                            )
                        } else {
                            null
                        }
                },
            ).build()
}
