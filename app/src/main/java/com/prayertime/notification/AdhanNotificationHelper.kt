package com.prayertime.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.prayertime.R
import com.prayertime.domain.model.Prayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdhanNotificationHelper
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        companion object {
            const val CHANNEL_ID = "adhan"
            private const val NOTIFICATION_ID_ENABLED = 3990
            private const val NOTIFICATION_ID_BASE = 4000
        }

        init {
            ensureChannel()
        }

        fun ensureChannel() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.adhan_channel_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = context.getString(R.string.adhan_channel_desc)
                    enableVibration(true)
                }
            manager.createNotificationChannel(channel)
        }

        /** One-time proof that posting works; Android Settings then lists a notification for this app. */
        fun showAdhanEnabledConfirmation() {
            ensureChannel()
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

            val notification =
                NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_adhan)
                    .setContentTitle(context.getString(R.string.adhan_enabled_confirmation_title))
                    .setContentText(context.getString(R.string.adhan_enabled_confirmation_body))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(true)
                    .setSilent(true)
                    .build()

            try {
                manager.notify(NOTIFICATION_ID_ENABLED, notification)
            } catch (_: SecurityException) {
            } catch (_: RuntimeException) {
                // Invalid/missing small icon on legacy API — must not crash Settings toggle.
            }
        }

        fun showPrayerNotification(prayer: Prayer) {
            ensureChannel()
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

            val title = context.getString(R.string.adhan_notification_title)
            val body = context.getString(R.string.adhan_notification_body, prayerLabel(prayer))

            val notification =
                NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_adhan)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .build()

            try {
                manager.notify(NOTIFICATION_ID_BASE + prayer.ordinal, notification)
            } catch (_: SecurityException) {
                // POST_NOTIFICATIONS not granted on API 33+
            } catch (_: RuntimeException) {
                // Legacy devices — bad icon or disabled notifications.
            }
        }

        private fun prayerLabel(prayer: Prayer): String {
            val resId =
                when (prayer) {
                    Prayer.FAJR -> R.string.fajr
                    Prayer.SHURUQ -> R.string.shuruq
                    Prayer.DHUHR -> R.string.dhuhr
                    Prayer.ASR -> R.string.asr
                    Prayer.MAGHRIB -> R.string.maghrib
                    Prayer.ISHA -> R.string.isha
                }
            return context.getString(resId)
        }
    }
