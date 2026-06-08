package com.prayertime.alarm

import android.content.Context
import com.prayertime.data.local.AppPreferencesDataSource
import com.prayertime.data.repository.PrayerTimesRepository
import com.prayertime.domain.model.PrayerTimesResult
import com.prayertime.permission.AdhanPermissions
import com.prayertime.widget.WidgetUpdater
import kotlinx.coroutines.flow.first

/** Reschedules prayer alarms when adhan is enabled and permissions allow posting. */
object AdhanAlarmRescheduler {
    suspend fun rescheduleIfEnabled(
        context: Context,
        repository: PrayerTimesRepository,
        preferences: AppPreferencesDataSource,
        widgetUpdater: WidgetUpdater,
    ) {
        if (!preferences.adhanNotificationsEnabled.first()) {
            PrayerAlarmScheduler.cancelAllPrayerAlarms(context)
            widgetUpdater.requestImmediateUpdate()
            return
        }
        val config =
            repository.cityConfig.first() ?: run {
                PrayerAlarmScheduler.cancelAllPrayerAlarms(context)
                widgetUpdater.requestImmediateUpdate()
                return
            }
        when (val resolved = BootPrayerTimesResolver.resolve(repository, config)) {
            is PrayerTimesResult.Success ->
                if (AdhanPermissions.areNotificationsAllowed(context)) {
                    PrayerAlarmScheduler.schedulePrayerAlarms(
                        context,
                        resolved.times,
                        useReliableAlarms = true,
                    )
                } else {
                    PrayerAlarmScheduler.cancelAllPrayerAlarms(context)
                }
            is PrayerTimesResult.Error -> PrayerAlarmScheduler.cancelAllPrayerAlarms(context)
        }
        widgetUpdater.requestImmediateUpdate()
    }
}
