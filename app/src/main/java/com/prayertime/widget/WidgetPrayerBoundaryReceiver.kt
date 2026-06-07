package com.prayertime.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.prayertime.di.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

class WidgetPrayerBoundaryReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != ACTION_WIDGET_BOUNDARY && intent.action != ACTION_WIDGET_COUNTDOWN_TICK) return
        val pendingResult = goAsync()
        val entryPoint =
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                WidgetEntryPoint::class.java,
            )
        val updater = entryPoint.widgetUpdater()
        val scope = entryPoint.widgetCoroutineScope()
        scope.launch {
            try {
                updater.requestImmediateUpdate()
            } finally {
                pendingResult?.finish()
            }
        }
    }

    companion object {
        const val ACTION_WIDGET_BOUNDARY = "com.prayertime.action.WIDGET_PRAYER_BOUNDARY"
        const val ACTION_WIDGET_COUNTDOWN_TICK = "com.prayertime.action.WIDGET_COUNTDOWN_TICK"
    }
}
