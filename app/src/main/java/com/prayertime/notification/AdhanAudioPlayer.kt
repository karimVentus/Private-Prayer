package com.prayertime.notification

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

/** Plays adhan audio to completion (shared by in-app delivery and [AdhanPlaybackService]). */
internal object AdhanAudioPlayer {
    suspend fun playToCompletion(
        context: Context,
        soundPref: String,
    ) {
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
                if (!setAdhanDataSource(context, checkNotNull(player), soundPref)) {
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
        context: Context,
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
                setRawDataSource(context, player, AdhanSoundResolver.DEFAULT_KEY)
            }
        }
        return setRawDataSource(context, player, soundPref)
    }

    private fun setRawDataSource(
        context: Context,
        player: MediaPlayer,
        key: String,
    ): Boolean {
        val resId = AdhanSoundResolver.rawResFor(key)
        return try {
            val uri = Uri.parse("android.resource://${context.packageName}/$resId")
            player.setDataSource(context, uri)
            true
        } catch (_: Exception) {
            false
        }
    }
}
