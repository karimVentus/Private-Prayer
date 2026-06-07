package com.prayertime.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.prayertime.data.local.AppPreferencesDataSource
import com.prayertime.domain.model.Prayer
import com.prayertime.notification.AdhanAlertPolicy
import com.prayertime.notification.AdhanNotificationHelper
import com.prayertime.notification.AdhanSoundResolver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AdhanAlarmReceiver : BroadcastReceiver() {
    @Inject
    lateinit var adhanNotificationHelper: AdhanNotificationHelper

    @Inject
    lateinit var preferences: AppPreferencesDataSource

    @Inject
    @com.prayertime.di.WidgetScope
    lateinit var alarmScope: CoroutineScope

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val prayerName =
            intent.takeIf { it.action == ACTION_PRAYER_ALARM }
                ?.getStringExtra(EXTRA_PRAYER)
        val prayer = prayerName?.let { runCatching { Prayer.valueOf(it) }.getOrNull() }
        if (prayer == null) {
            return
        }
        val soundPref = intent.getStringExtra(EXTRA_ADHAN_SOUND) ?: DEFAULT_ADHAN_SOUND
        val pendingResult = goAsync()
        alarmScope.launch {
            try {
                if (preferences.isPrayerMuted(prayer.name)) return@launch
                val playWhenSilent = preferences.readAdhanPlayWhenSilentOnce()
                val effectiveMode = AdhanAlertPolicy.effectiveMode(context, playWhenSilent)
                adhanNotificationHelper.showPrayerNotification(prayer, effectiveMode)
                if (AdhanAlertPolicy.shouldPlayAdhanAudio(context, playWhenSilent)) {
                    playAdhanSound(context, soundPref)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun playAdhanSound(
        context: Context,
        soundPref: String,
    ) {
        val resId = AdhanSoundResolver.rawResFor(soundPref)
        try {
            val player = MediaPlayer.create(context, resId) ?: return
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            player.setOnCompletionListener { it.release() }
            player.start()
        } catch (_: Exception) {
            // Notification still shown if audio fails
        }
    }

    companion object {
        const val ACTION_PRAYER_ALARM = "com.prayertime.ACTION_PRAYER_ALARM"
        const val EXTRA_PRAYER = "extra_prayer"
        const val EXTRA_ADHAN_SOUND = "extra_adhan_sound"
        const val DEFAULT_ADHAN_SOUND = AdhanSoundResolver.DEFAULT_KEY
    }
}
