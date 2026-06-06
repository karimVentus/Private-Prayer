package com.prayertime.alarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.prayertime.data.local.AppPreferencesDataSource
import com.prayertime.data.repository.PrayerTimesRepository
import com.prayertime.widget.WidgetUpdater
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Reschedules adhan alarms when the user grants or revokes exact-alarm permission. */
@AndroidEntryPoint
class ExactAlarmPermissionReceiver : BroadcastReceiver() {
    @Inject
    lateinit var repository: PrayerTimesRepository

    @Inject
    lateinit var preferences: AppPreferencesDataSource

    @Inject
    lateinit var widgetUpdater: WidgetUpdater

    @Inject
    @com.prayertime.di.WidgetScope
    lateinit var scope: CoroutineScope

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) {
            return
        }
        val pendingResult = goAsync()
        scope.launch {
            try {
                AdhanAlarmRescheduler.rescheduleIfEnabled(
                    context,
                    repository,
                    preferences,
                    widgetUpdater,
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
