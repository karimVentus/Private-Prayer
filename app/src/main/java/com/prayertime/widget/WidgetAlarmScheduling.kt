package com.prayertime.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.prayertime.PendingIntentRequestCodes
import com.prayertime.alarm.ExplicitAlarmIntents
import com.prayertime.alarm.PrayerAlarmScheduler
import com.prayertime.alarm.ShowIntentFactory

internal object WidgetAlarmScheduling {
    fun scheduleAt(
        context: Context,
        triggerAtMs: Long,
        requestCode: Int,
        showRequestCode: Int,
        cancelExisting: (Context) -> Unit,
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        cancelExisting(context)
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                requestCode,
                widgetAlarmIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val useReliableAlarms = PrayerAlarmScheduler.canUseExactAlarms(context)
        val showIntent = if (useReliableAlarms) ShowIntentFactory.create(context, showRequestCode) else null
        if (useReliableAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && showIntent != null) {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAtMs, showIntent),
                pendingIntent,
            )
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMs,
                pendingIntent,
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerAtMs,
                pendingIntent,
            )
        }
    }

    fun cancelBoundary(context: Context) {
        cancel(context, PendingIntentRequestCodes.WIDGET_BOUNDARY_ALARM)
    }

    fun cancelCountdownTick(context: Context) {
        cancel(context, PendingIntentRequestCodes.WIDGET_COUNTDOWN_ALARM)
    }

    private fun cancel(
        context: Context,
        requestCode: Int,
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pending =
            PendingIntent.getBroadcast(
                context,
                requestCode,
                widgetAlarmIntent(context),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
        if (pending != null) {
            alarmManager.cancel(pending)
            pending.cancel()
        }
    }

    private fun widgetAlarmIntent(context: Context): Intent =
        ExplicitAlarmIntents.broadcast(context, WidgetPrayerBoundaryReceiver::class.java)
}
