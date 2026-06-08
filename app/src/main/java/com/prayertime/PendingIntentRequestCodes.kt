package com.prayertime

import com.prayertime.domain.model.Prayer

/**
 * App-wide [android.app.PendingIntent] request codes.
 *
 * PendingIntent identity is `(requestCode, component, intent action)` — extras are ignored.
 * Duplicate codes with different components are fine; reuse within the same component causes
 * silent collisions. [com.prayertime.alarm.PrayerAlarmScheduler] schedule/cancel share one
 * intent builder per prayer so lookup stays symmetric.
 *
 * | Range   | Code(s)              | Owner |
 * |---------|----------------------|-------|
 * | 5000–05 | 5000 + [Prayer.ordinal] | [com.prayertime.alarm.PrayerAlarmScheduler] |
 * | 5999    | ADHAN_SHOW           | PrayerAlarmScheduler alarm-clock show intent |
 * | 7001    | WIDGET_LAUNCH        | [com.prayertime.widget.WidgetRemoteViewsBuilder] tap → MainActivity |
 * | 7100    | WIDGET_BOUNDARY_ALARM | [com.prayertime.widget.WidgetPrayerBoundaryScheduler] |
 * | 7101    | WIDGET_BOUNDARY_SHOW | WidgetPrayerBoundaryScheduler alarm-clock show intent |
 * | 7102    | WIDGET_COUNTDOWN_ALARM | WidgetPrayerBoundaryScheduler minute countdown refresh |
 * | 7103    | WIDGET_COUNTDOWN_SHOW | WidgetPrayerBoundaryScheduler countdown alarm show intent |
 *
 * Next free block: 7200.
 */
object PendingIntentRequestCodes {
    const val ADHAN_PRAYER_BASE = 5000
    const val ADHAN_SHOW = 5999
    const val WIDGET_LAUNCH = 7001
    const val WIDGET_BOUNDARY_ALARM = 7100
    const val WIDGET_BOUNDARY_SHOW = 7101
    const val WIDGET_COUNTDOWN_ALARM = 7102
    const val WIDGET_COUNTDOWN_SHOW = 7103

    fun adhanPrayer(prayer: Prayer): Int = ADHAN_PRAYER_BASE + prayer.ordinal
}
