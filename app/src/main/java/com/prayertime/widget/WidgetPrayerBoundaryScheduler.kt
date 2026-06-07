package com.prayertime.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.prayertime.PendingIntentRequestCodes
import com.prayertime.domain.calculator.PrayerTimeCalculator
import com.prayertime.domain.model.PrayerTime
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetPrayerBoundaryScheduler
    @Inject
    constructor(
        private val loader: WidgetSnapshotLoader,
    ) {
        suspend fun scheduleNextBoundaryUpdate(context: Context) {
            val snapshot = loader.load()
            if (snapshot.state != WidgetSnapshot.State.READY || !hasInstalledWidgets(context)) {
                cancel(context)
                return
            }
            val boundaryAt = nextBoundaryTimestamp(snapshot.times, snapshot.timezone)
            if (boundaryAt != null) {
                scheduleBoundaryAt(context, boundaryAt)
                WidgetCountdownRefreshScheduler.scheduleNextTick(context)
            } else {
                cancel(context)
            }
        }

        companion object {
            fun cancel(context: Context) {
                WidgetAlarmScheduling.cancelBoundary(context)
                WidgetCountdownRefreshScheduler.cancel(context)
            }

            internal fun nextBoundaryTimestamp(
                times: List<PrayerTime>,
                timezone: String,
                now: Long = System.currentTimeMillis(),
            ): Long? {
                if (timezone.isEmpty()) return null
                return PrayerTimeCalculator.nextBoundaryTimestamp(
                    times,
                    TimeZone.getTimeZone(timezone),
                    now,
                )
            }

            internal fun scheduleBoundaryAt(
                context: Context,
                triggerAtMs: Long,
            ) {
                WidgetAlarmScheduling.scheduleAt(
                    context = context,
                    triggerAtMs = triggerAtMs,
                    requestCode = PendingIntentRequestCodes.WIDGET_BOUNDARY_ALARM,
                    showRequestCode = PendingIntentRequestCodes.WIDGET_BOUNDARY_SHOW,
                    action = WidgetPrayerBoundaryReceiver.ACTION_WIDGET_BOUNDARY,
                    cancelExisting = WidgetAlarmScheduling::cancelBoundary,
                )
            }

            private fun hasInstalledWidgets(context: Context): Boolean {
                val manager = AppWidgetManager.getInstance(context)
                return listOf(
                    ComponentName(context, PrayerTimeWidgetProvider::class.java),
                    ComponentName(context, PrayerTimeWidgetProviderLarge::class.java),
                ).any { manager.getAppWidgetIds(it).isNotEmpty() }
            }
        }
    }
