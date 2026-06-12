package com.prayertime.notification

import android.content.Context
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
        /**
         * @param detachedPlayback When true (alarm path), hand audio to [AdhanPlaybackService] so
         *   playback is not cut off by the ~30s [android.content.BroadcastReceiver.goAsync] limit.
         */
        suspend fun deliver(
            prayer: Prayer,
            respectMute: Boolean = true,
            detachedPlayback: Boolean = false,
        ) {
            if (respectMute && preferences.isPrayerMuted(prayer.name)) return
            val playWhenSilent = preferences.readAdhanPlayWhenSilentOnce()
            val soundPref = preferences.adhanSound.first()
            val effectiveMode = AdhanAlertPolicy.effectiveMode(context, playWhenSilent)
            adhanNotificationHelper.showPrayerNotification(prayer, effectiveMode)
            if (!AdhanAlertPolicy.shouldPlayAdhanAudio(context, playWhenSilent)) return
            if (detachedPlayback) {
                AdhanPlaybackService.start(context, prayer, soundPref)
            } else {
                AdhanAudioPlayer.playToCompletion(context, soundPref)
            }
        }
    }
