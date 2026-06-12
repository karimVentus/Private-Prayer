package com.prayertime.notification

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.prayertime.domain.model.Prayer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground playback for alarm-triggered adhan. Broadcast receivers are capped at ~30s via
 * [android.content.BroadcastReceiver.goAsync]; this service runs until audio finishes.
 */
@AndroidEntryPoint
class AdhanPlaybackService : Service() {
    @Inject
    lateinit var adhanNotificationHelper: AdhanNotificationHelper

    @Inject
    @com.prayertime.di.WidgetScope
    lateinit var serviceScope: CoroutineScope

    private var playbackJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val prayerName = intent?.getStringExtra(EXTRA_PRAYER)
        val prayer = prayerName?.let { runCatching { Prayer.valueOf(it) }.getOrNull() }
        val soundPref = intent?.getStringExtra(EXTRA_SOUND) ?: AdhanSoundResolver.DEFAULT_KEY
        if (prayer == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val notificationId = adhanNotificationHelper.notificationId(prayer)
        val notification =
            adhanNotificationHelper.buildPrayerNotification(
                prayer,
                AdhanAlertPolicy.mode(this),
            )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(notificationId, notification)
        }

        playbackJob?.cancel()
        playbackJob =
            serviceScope.launch {
                try {
                    AdhanAudioPlayer.playToCompletion(applicationContext, soundPref)
                } finally {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    } else {
                        @Suppress("DEPRECATION")
                        stopForeground(true)
                    }
                    stopSelf(startId)
                }
            }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        playbackJob?.cancel()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_PRAYER = "extra_prayer"
        private const val EXTRA_SOUND = "extra_sound"

        fun start(
            context: Context,
            prayer: Prayer,
            soundPref: String,
        ) {
            val intent =
                Intent(context, AdhanPlaybackService::class.java).apply {
                    setPackage(context.packageName)
                    putExtra(EXTRA_PRAYER, prayer.name)
                    putExtra(EXTRA_SOUND, soundPref)
                }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
