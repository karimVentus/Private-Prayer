package com.prayertime.notification

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.PowerManager
import com.prayertime.data.local.AppPreferencesDataSource
import com.prayertime.domain.model.Prayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

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

        /**
         * Plays adhan to completion. Uses [MediaPlayer.setWakeMode] so playback survives Doze /
         * screen-off (e.g. airplane mode overnight). Caller must keep the broadcast [goAsync] scope
         * alive until this suspend function returns.
         */
        private suspend fun playAdhanSound(soundPref: String) {
            suspendCancellableCoroutine { cont ->
                var player: MediaPlayer? = null

                fun finishPlayback() {
                    player?.release()
                    player = null
                    if (cont.isActive) cont.resume(Unit)
                }

                cont.invokeOnCancellation { finishPlayback() }

                try {
                    player =
                        MediaPlayer().apply {
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_ALARM)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build(),
                            )
                            setWakeMode(context.applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                            setOnCompletionListener { finishPlayback() }
                            setOnErrorListener { mp, _, _ ->
                                mp.release()
                                player = null
                                if (cont.isActive) cont.resume(Unit)
                                true
                            }
                        }
                    if (!setAdhanDataSource(checkNotNull(player), soundPref)) {
                        finishPlayback()
                        return@suspendCancellableCoroutine
                    }
                    checkNotNull(player).prepare()
                    checkNotNull(player).start()
                } catch (_: Exception) {
                    finishPlayback()
                }
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
