package com.prayertime.notification

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.prayertime.data.local.AppPreferencesDataSource
import com.prayertime.domain.model.Prayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Shows the adhan notification and plays audio (shared by alarm receiver and in-app unmute). */
@Singleton
class AdhanAlertDeliverer
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val preferences: AppPreferencesDataSource,
        private val adhanNotificationHelper: AdhanNotificationHelper,
    ) {
        suspend fun deliver(
            prayer: Prayer,
            respectMute: Boolean = true,
        ) {
            if (respectMute && preferences.isPrayerMuted(prayer.name)) return
            val playWhenSilent = preferences.readAdhanPlayWhenSilentOnce()
            val soundPref = preferences.adhanSound.first()
            val effectiveMode = AdhanAlertPolicy.effectiveMode(context, playWhenSilent)
            adhanNotificationHelper.showPrayerNotification(prayer, effectiveMode)
            if (AdhanAlertPolicy.shouldPlayAdhanAudio(context, playWhenSilent)) {
                playAdhanSound(soundPref)
            }
        }

        private fun playAdhanSound(soundPref: String) {
            val resId = AdhanSoundResolver.rawResFor(soundPref)
            try {
                val player = MediaPlayer()
                player.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                player.setOnCompletionListener { it.release() }
                val fd = context.resources.openRawResourceFd(resId) ?: return
                fd.use {
                    player.setDataSource(it.fileDescriptor, it.startOffset, it.length)
                }
                player.prepare()
                player.start()
            } catch (_: Exception) {
                // Notification still shown if audio fails
            }
        }
    }
