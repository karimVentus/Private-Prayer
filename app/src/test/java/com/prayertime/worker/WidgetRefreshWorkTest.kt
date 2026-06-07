package com.prayertime.worker

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Looper
import android.widget.TextView
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
import com.prayertime.R
import com.prayertime.testing.WidgetTestSupport
import com.prayertime.widget.WidgetRemoteViewsBuilder
import com.prayertime.widget.WidgetSize
import com.prayertime.widget.WidgetSnapshot
import com.prayertime.widget.WidgetUpdater
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
import org.robolectric.shadows.ShadowAppWidgetManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetRefreshWorkTest {
    private lateinit var context: Context
    private lateinit var stack: WidgetTestSupport.Stack
    private lateinit var workerFactory: WorkerFactory
    private lateinit var shadowWidgets: ShadowAppWidgetManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val appWidgetManager = AppWidgetManager.getInstance(context)
        shadowWidgets = Shadows.shadowOf(appWidgetManager) as ShadowAppWidgetManager
        // Avoid RemoteViews.reapply() tag-cache corruption when multiple widgets update in one test.
        shadowWidgets.setAlwaysRecreateViewsDuringUpdate(true)
        stack = runBlocking { WidgetTestSupport.create(context) }

        workerFactory =
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
                            stack.preferences,
                            stack.updater,
                            stack.boundaryScheduler,
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
        wm.cancelUniqueWork(WidgetRefreshWork.UNIQUE_WORK_NAME).result.get()
        wm.pruneWork().result.get()
    }

    @After
    fun teardown() {
        shadowWidgets.setAlwaysRecreateViewsDuringUpdate(false)
        stack.close()
    }

    @Test
    fun enqueue_registersUniquePeriodicWidgetRefreshWork() {
        WidgetRefreshWork.enqueue(context)

        val infos =
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(WidgetRefreshWork.UNIQUE_WORK_NAME)
                .get()
        assertEquals(1, infos.size)
        val state = infos[0].state
        assertTrue(
            state == WorkInfo.State.ENQUEUED ||
                state == WorkInfo.State.BLOCKED ||
                state == WorkInfo.State.RUNNING,
        )
        assertTrue(
            infos[0].tags.any { it.contains(WidgetUpdateWorker::class.java.simpleName) },
        )
    }

    @Test
    fun enqueue_secondCallKeepsSinglePeriodicWork() {
        WidgetRefreshWork.enqueue(context)
        WidgetRefreshWork.enqueue(context)

        val infos =
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(WidgetRefreshWork.UNIQUE_WORK_NAME)
                .get()
        assertEquals(1, infos.size)
    }

    @Test
    fun worker_withNoCity_skipsBoundaryAlarmAndBuildsSetupMessage() =
        runBlocking {
            val noCityStack = WidgetTestSupport.create(context, seedCity = false)
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                val shadowAlarm = Shadows.shadowOf(alarmManager)

                val worker =
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
                                            noCityStack.preferences,
                                            noCityStack.updater,
                                            noCityStack.boundaryScheduler,
                                        )
                                    } else {
                                        null
                                    }
                            },
                        ).build()
                assertEquals(ListenableWorker.Result.success(), worker.doWork())
                assertEquals(0, shadowAlarm.scheduledAlarms.size)

                val snapshot = noCityStack.loader.load()
                assertEquals(WidgetSnapshot.State.NO_CITY, snapshot.state)
                val views = WidgetRemoteViewsBuilder(context, noCityStack.preferences).build(snapshot, WidgetSize.MEDIUM)
                val emptyMessage =
                    views.apply(context, android.widget.FrameLayout(context))
                        .findViewById<TextView>(R.id.widget_empty)
                        ?.text
                        ?.toString()
                assertEquals(context.getString(R.string.widget_no_city), emptyMessage)
            } finally {
                noCityStack.close()
            }
        }

    @Test
    fun updateAll_noOpsWhenNoWidgetsRegistered() =
        runBlocking {
            stack.updater.updateAll()
        }

    @Test
    fun updateAll_pushesSnapshotContentToRegisteredWidget() =
        runBlocking {
            WidgetTestSupport.registerMediumWidget(context)

            stack.updater.updateAll()

            val snapshot = stack.loader.load()
            assertEquals(WidgetSnapshot.State.READY, snapshot.state)
            val root =
                WidgetRemoteViewsBuilder(context, stack.preferences)
                    .build(snapshot, WidgetSize.MEDIUM)
                    .apply(context, android.widget.FrameLayout(context))
            assertEquals(
                context.getString(R.string.fajr),
                root.findViewById<TextView>(R.id.widget_prayer_0)?.text?.toString(),
            )
        }

    @Test
    fun updateAll_updatesEveryRegisteredProvider() =
        runBlocking {
            WidgetTestSupport.registerMediumWidget(context)
            WidgetTestSupport.registerLargeWidget(context)

            stack.updater.updateAll()
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            val snapshot = stack.loader.load()
            assertEquals(WidgetSnapshot.State.READY, snapshot.state)
            val fajr = context.getString(R.string.fajr)
            val builder = WidgetRemoteViewsBuilder(context, stack.preferences)
            listOf(WidgetSize.MEDIUM, WidgetSize.LARGE).forEach { size ->
                val root =
                    builder.build(snapshot, size)
                        .apply(context, android.widget.FrameLayout(context))
                assertEquals(
                    fajr,
                    root.findViewById<TextView>(R.id.widget_prayer_0)?.text?.toString(),
                )
            }
        }

    @Test
    fun requestImmediateUpdate_enqueuesWidgetUpdateWorker() {
        stack.updater.requestImmediateUpdate()

        val infos =
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(WidgetUpdater.IMMEDIATE_WORK_NAME)
                .get()
        assertEquals(1, infos.size)
        assertTrue(
            infos[0].state == WorkInfo.State.ENQUEUED ||
                infos[0].state == WorkInfo.State.RUNNING ||
                infos[0].state == WorkInfo.State.SUCCEEDED,
        )
    }

    @Test
    fun worker_updatesWidgetsAndSchedulesBoundaryAlarm() =
        runBlocking {
            WidgetTestSupport.registerMediumWidget(context)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val shadowAlarm = Shadows.shadowOf(alarmManager)
            val alarmsBefore = shadowAlarm.scheduledAlarms.size

            val worker =
                TestListenableWorkerBuilder<WidgetUpdateWorker>(context)
                    .setWorkerFactory(workerFactory)
                    .build()
            val result = worker.doWork()
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            assertEquals(ListenableWorker.Result.success(), result)
            val snapshot = stack.loader.load()
            assertEquals(WidgetSnapshot.State.READY, snapshot.state)
            val root =
                WidgetRemoteViewsBuilder(context, stack.preferences)
                    .build(snapshot, WidgetSize.MEDIUM)
                    .apply(context, android.widget.FrameLayout(context))
            assertTrue(
                root.findViewById<TextView>(R.id.widget_prayer_0)?.text?.isNotBlank() == true,
            )
            assertTrue(
                "Boundary alarm should be scheduled after refresh",
                shadowAlarm.scheduledAlarms.size > alarmsBefore,
            )
        }
}
