package com.prayertime.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import com.prayertime.PendingIntentRequestCodes
import com.prayertime.domain.model.Prayer
import com.prayertime.domain.model.PrayerTime

object PrayerAlarmScheduler {
    /**
     * @param useReliableAlarms When true, uses [AlarmManager.setAlarmClock] (Doze-safe). Requires
     * [AlarmManager.canScheduleExactAlarms] on API 31+ ([USE_EXACT_ALARM] or user-granted
     * [SCHEDULE_EXACT_ALARM]); otherwise falls back to inexact [AlarmManager.setAndAllowWhileIdle].
     */
    fun schedulePrayerAlarms(
        context: Context,
        times: List<PrayerTime>,
        useReliableAlarms: Boolean,
    ) {
        cancelAllPrayerAlarms(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val now = System.currentTimeMillis()
        val upcoming =
            times.filter { time ->
                time.prayer in Prayer.adhanAlarmPrayers && time.timestamp > now
            }
        val showIntent = if (useReliableAlarms) showPendingIntent(context) else null

        for (time in upcoming) {
            val pendingIntent = pendingIntentFor(context, time.prayer)
            scheduleOne(
                alarmManager = alarmManager,
                triggerAtMs = time.timestamp,
                pendingIntent = pendingIntent,
                showIntent = showIntent,
                useReliableAlarms = useReliableAlarms,
            )
        }
    }

    fun cancelAllPrayerAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (prayer in Prayer.adhanAlarmPrayers) {
            val pending =
                adhanAlarmPendingIntent(
                    context,
                    prayer,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
                ) ?: continue
            alarmManager.cancel(pending)
            pending.cancel()
        }
    }

    fun canUseExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        return alarmManager?.canScheduleExactAlarms() == true
    }

    private fun scheduleOne(
        alarmManager: AlarmManager,
        triggerAtMs: Long,
        pendingIntent: PendingIntent,
        showIntent: PendingIntent?,
        useReliableAlarms: Boolean,
    ) {
        if (useReliableAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && showIntent != null) {
            try {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAtMs, showIntent),
                    pendingIntent,
                )
                return
            } catch (_: SecurityException) {
                // Fall through to inexact fallback
            }
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

    private fun showPendingIntent(context: Context): PendingIntent = ShowIntentFactory.create(context, PendingIntentRequestCodes.ADHAN_SHOW)

    private fun pendingIntentFor(
        context: Context,
        prayer: Prayer,
    ): PendingIntent =
        checkNotNull(
            adhanAlarmPendingIntent(
                context,
                prayer,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )

    /** Canonical adhan alarm [PendingIntent]; schedule and cancel must share this builder. */
    private fun adhanAlarmPendingIntent(
        context: Context,
        prayer: Prayer,
        flags: Int,
    ): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            requestCode(prayer),
            intentFor(context, prayer),
            flags,
        )

    private fun intentFor(
        context: Context,
        prayer: Prayer,
    ): Intent =
        Intent().apply {
            component = ComponentName(context, AdhanAlarmReceiver::class.java)
            setPackage(context.packageName)
            putExtra(AdhanAlarmReceiver.EXTRA_PRAYER, prayer.name)
        }

    private fun requestCode(prayer: Prayer): Int = PendingIntentRequestCodes.adhanPrayer(prayer)
}
