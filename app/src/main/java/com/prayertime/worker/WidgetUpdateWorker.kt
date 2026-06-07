package com.prayertime.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prayertime.data.local.AppPreferencesDataSource
import com.prayertime.locale.AppLocale
import com.prayertime.widget.WidgetPrayerBoundaryScheduler
import com.prayertime.widget.WidgetUpdater
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class WidgetUpdateWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted params: WorkerParameters,
        private val preferences: AppPreferencesDataSource,
        private val widgetUpdater: WidgetUpdater,
        private val boundaryScheduler: WidgetPrayerBoundaryScheduler,
    ) : CoroutineWorker(appContext, params) {
        override suspend fun doWork(): Result {
            val tag = preferences.readAppLanguageTagOnce()
            AppLocale.apply(AppLocale.normalizeStoredTag(tag))
            widgetUpdater.updateAll()
            boundaryScheduler.scheduleNextBoundaryUpdate(applicationContext)
            return Result.success()
        }
    }
