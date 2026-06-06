package com.prayertime.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.prayertime.PendingIntentRequestCodes
import com.prayertime.alarm.PrayerAlarmScheduler
import com.prayertime.alarm.ShowIntentFactory
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
            if (snapshot.state != WidgetSnapshot.State.READY) {
                cancel(context)
                return
            }
            val triggerAtMs =
                nextBoundaryTimestamp(snapshot.times, snapshot.timezone) ?: run {
                    cancel(context)
                    return
                }
            scheduleAt(context, triggerAtMs)
        }

        companion object {
            fun cancel(context: Context) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val pending =
                    PendingIntent.getBroadcast(
                        context,
                        PendingIntentRequestCodes.WIDGET_BOUNDARY_ALARM,
                        intentFor(context),
                        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
                    )
                if (pending != null) {
                    alarmManager.cancel(pending)
                    pending.cancel()
                }
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

            internal fun scheduleAt(
                context: Context,
                triggerAtMs: Long,
            ) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                cancel(context)
                val pendingIntent =
                    PendingIntent.getBroadcast(
                        context,
                        PendingIntentRequestCodes.WIDGET_BOUNDARY_ALARM,
                        intentFor(context),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                val useReliableAlarms = PrayerAlarmScheduler.canUseExactAlarms(context)
                val showIntent = if (useReliableAlarms) showPendingIntent(context) else null
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

            private fun intentFor(context: Context): Intent =
                Intent(context, WidgetPrayerBoundaryReceiver::class.java).apply {
                    action = WidgetPrayerBoundaryReceiver.ACTION_WIDGET_BOUNDARY
                }

            private fun showPendingIntent(context: Context): PendingIntent =
                ShowIntentFactory.create(context, PendingIntentRequestCodes.WIDGET_BOUNDARY_SHOW)
        }
    }
