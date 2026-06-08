package com.prayertime.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prayertime.alarm.PrayerAlarmScheduler
import com.prayertime.data.local.AppPreferencesDataSource
import com.prayertime.data.repository.PrayerTimesRepository
import com.prayertime.domain.model.PrayerTimesResult
import com.prayertime.permission.AdhanPermissions
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class PrayerTimeRefreshWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted params: WorkerParameters,
        private val repository: PrayerTimesRepository,
        private val preferences: AppPreferencesDataSource,
    ) : CoroutineWorker(appContext, params) {
        override suspend fun doWork(): Result {
            val config = repository.cityConfig.first() ?: return Result.success()
            return when (val result = repository.fetchTodayTimes(config)) {
                is PrayerTimesResult.Error -> Result.retry()
                is PrayerTimesResult.Success -> {
                    val adhanEnabled = preferences.adhanNotificationsEnabled.first()
                    if (adhanEnabled && AdhanPermissions.areNotificationsAllowed(applicationContext)) {
                        PrayerAlarmScheduler.schedulePrayerAlarms(
                            applicationContext,
                            result.times,
                            useReliableAlarms = true,
                        )
                    }
                    Result.success()
                }
            }
        }
    }
