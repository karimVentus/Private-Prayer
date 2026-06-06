package com.prayertime.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Enqueues periodic widget refresh. [REFRESH_INTERVAL_MINUTES] is the nominal interval; WorkManager
 * may defer runs. Prayer-boundary alarms provide sharper updates at each prayer time.
 */
object WidgetRefreshWork {
    internal const val UNIQUE_WORK_NAME = "prayer_time_widget_refresh"

    internal const val REFRESH_INTERVAL_MINUTES = 30L

    fun enqueue(context: Context) {
        val request =
            PeriodicWorkRequestBuilder<WidgetUpdateWorker>(REFRESH_INTERVAL_MINUTES, TimeUnit.MINUTES)
                .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
