package com.prayertime.widget

import android.content.Context
import com.prayertime.PendingIntentRequestCodes

internal object WidgetCountdownRefreshScheduler {
    fun scheduleNextTick(context: Context) {
        WidgetAlarmScheduling.scheduleAt(
            context = context,
            triggerAtMs = nextTickTimestamp(),
            requestCode = PendingIntentRequestCodes.WIDGET_COUNTDOWN_ALARM,
            showRequestCode = PendingIntentRequestCodes.WIDGET_COUNTDOWN_SHOW,
            action = WidgetPrayerBoundaryReceiver.ACTION_WIDGET_COUNTDOWN_TICK,
            cancelExisting = { WidgetAlarmScheduling.cancelCountdownTick(it) },
        )
    }

    fun cancel(context: Context) {
        WidgetAlarmScheduling.cancelCountdownTick(context)
    }

    internal fun nextTickTimestamp(now: Long = System.currentTimeMillis()): Long = ((now / 60_000L) + 1L) * 60_000L
}
