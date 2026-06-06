package com.prayertime.alarm

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

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {
    @Inject
    lateinit var repository: PrayerTimesRepository

    @Inject
    lateinit var preferences: AppPreferencesDataSource

    @Inject
    lateinit var widgetUpdater: WidgetUpdater

    @Inject
    @com.prayertime.di.WidgetScope
    lateinit var bootScope: CoroutineScope

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }
        val pendingResult = goAsync()
        bootScope.launch {
            try {
                rescheduleAfterBoot(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun rescheduleAfterBoot(context: Context) {
        AdhanAlarmRescheduler.rescheduleIfEnabled(
            context,
            repository,
            preferences,
            widgetUpdater,
        )
    }
}
