package com.prayertime.notification

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.prayertime.data.local.AppPreferencesDataSource
import com.prayertime.domain.model.Prayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.File
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
            try {
                val player = MediaPlayer()
                player.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                player.setOnCompletionListener { it.release() }
                setAdhanDataSource(player, soundPref) ?: return
                player.prepare()
                player.start()
            } catch (_: Exception) {
                // Notification still shown if audio fails
            }
        }

        private fun setAdhanDataSource(
            player: MediaPlayer,
            soundPref: String,
        ): Boolean {
            if (AdhanSoundResolver.isCustom(soundPref)) {
                val filePath = AdhanSoundResolver.filePathForCustom(context, soundPref)
                val file = File(filePath)
                return if (file.exists()) {
                    player.setDataSource(filePath)
                    true
                } else {
                    setRawDataSource(player, AdhanSoundResolver.DEFAULT_KEY)
                }
            }
            return setRawDataSource(player, soundPref)
        }

        private fun setRawDataSource(
            player: MediaPlayer,
            key: String,
        ): Boolean {
            val resId = AdhanSoundResolver.rawResFor(key)
            val fd = context.resources.openRawResourceFd(resId) ?: return false
            fd.use { player.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            return true
        }
    }
