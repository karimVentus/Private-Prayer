package com.prayertime.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.prayertime.PendingIntentRequestCodes
import com.prayertime.domain.model.Prayer
import com.prayertime.domain.model.PrayerTime
import com.prayertime.notification.AdhanSoundResolver

object PrayerAlarmScheduler {
    private val schedulablePrayers = Prayer.entries.toSet()

    /**
     * @param useReliableAlarms When true, uses [AlarmManager.setAlarmClock] (Doze-safe). Requires
     * [AlarmManager.canScheduleExactAlarms] on API 31+ ([USE_EXACT_ALARM] or user-granted
     * [SCHEDULE_EXACT_ALARM]); otherwise falls back to inexact [AlarmManager.setAndAllowWhileIdle].
     * @param adhanSound The DataStore preference key for the selected adhan sound (e.g. "adhan").
     */
    fun schedulePrayerAlarms(
        context: Context,
        times: List<PrayerTime>,
        useReliableAlarms: Boolean,
        adhanSound: String = "adhan",
    ) {
        cancelAllPrayerAlarms(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val now = System.currentTimeMillis()
        val upcoming =
            times.filter { time ->
                time.prayer in schedulablePrayers && time.timestamp > now
            }
        val showIntent = if (useReliableAlarms) showPendingIntent(context) else null

        for (time in upcoming) {
            val pendingIntent = pendingIntentFor(context, time.prayer, adhanSound)
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
        for (prayer in schedulablePrayers) {
            // Extras do not affect PendingIntent identity; default key suffices for cancel lookup.
            val intent = intentFor(context, prayer, AdhanSoundResolver.DEFAULT_KEY)
            val pending =
                PendingIntent.getBroadcast(
                    context,
                    requestCode(prayer),
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
                )
            if (pending != null) {
                alarmManager.cancel(pending)
                pending.cancel()
            }
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
        adhanSound: String,
    ): PendingIntent {
        val intent = intentFor(context, prayer, adhanSound)
        return PendingIntent.getBroadcast(
            context,
            requestCode(prayer),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun intentFor(
        context: Context,
        prayer: Prayer,
        adhanSound: String,
    ): Intent =
        Intent(context, AdhanAlarmReceiver::class.java).apply {
            action = AdhanAlarmReceiver.ACTION_PRAYER_ALARM
            putExtra(AdhanAlarmReceiver.EXTRA_PRAYER, prayer.name)
            putExtra(AdhanAlarmReceiver.EXTRA_ADHAN_SOUND, adhanSound)
        }

    private fun requestCode(prayer: Prayer): Int = PendingIntentRequestCodes.adhanPrayer(prayer)
}
