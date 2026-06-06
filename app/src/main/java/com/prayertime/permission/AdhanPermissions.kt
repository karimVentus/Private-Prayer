package com.prayertime.permission

import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object AdhanPermissions {
    const val REQUEST_POST_NOTIFICATIONS = 1001

    fun needsPostNotificationsPermission(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun hasPostNotificationsPermission(context: Context): Boolean {
        if (!needsPostNotificationsPermission()) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** Matches the system “allow notifications” toggle (Settings → Apps → Prayer Times). */
    fun areNotificationsAllowed(context: Context): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun openNotificationSettings(activity: Activity) {
        val intent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
            }
        safeStartActivity(activity, intent)
    }

    fun requestPostNotifications(activity: Activity) {
        if (!needsPostNotificationsPermission()) return
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_POST_NOTIFICATIONS,
        )
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        return alarmManager?.canScheduleExactAlarms() == true
    }

    fun openExactAlarmSettings(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val intent =
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
        safeStartActivity(activity, intent)
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
    }

    fun openBatteryOptimizationSettings(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val requestExemption =
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
        if (requestExemption.resolveActivity(activity.packageManager) != null) {
            safeStartActivity(activity, requestExemption)
            return
        }
        safeStartActivity(activity, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }

    private fun safeStartActivity(
        activity: Activity,
        intent: Intent,
    ) {
        if (intent.resolveActivity(activity.packageManager) == null) return
        try {
            activity.startActivity(intent)
        } catch (_: Exception) {
            // Emulator or OEM may lack handler; optional settings must not crash the app.
        }
    }
}
