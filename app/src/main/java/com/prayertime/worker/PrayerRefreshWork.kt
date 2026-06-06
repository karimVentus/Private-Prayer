package com.prayertime.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Enqueues a daily background refresh of prayer times and adhan alarm reschedule.
 *
 * [REFRESH_INTERVAL_HOURS] is the nominal repeat interval only. WorkManager periodic work is not
 * wall-clock exact: the platform may defer runs (repeat interval plus flex), so the gap between
 * executions can exceed 24 hours. That is acceptable here because day rollover is also handled
 * when the app is open via [com.prayertime.ui.prayer.PrayerTimesViewModel.refreshIfPrayerDayStale]
 * and the live countdown ticker.
 */
object PrayerRefreshWork {
    internal const val UNIQUE_WORK_NAME = "prayer_time_daily_refresh"

    /** Nominal repeat interval; actual spacing is WorkManager-controlled and may be longer. */
    internal const val REFRESH_INTERVAL_HOURS = 24L

    fun enqueue(context: Context) {
        val request =
            PeriodicWorkRequestBuilder<PrayerTimeRefreshWorker>(REFRESH_INTERVAL_HOURS, TimeUnit.HOURS)
                .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
