package com.prayertime.notification

import android.content.Context
import android.media.AudioManager

/** How adhan should alert, derived from the device ringer mode. */
enum class AdhanAlertMode {
    /** Ringer on — play adhan audio and allow notification sound. */
    AUDIBLE,

    /** Vibrate only — no adhan audio; notification may vibrate. */
    VIBRATE,

    /** Silent — visual notification only. */
    SILENT,
}

internal object AdhanAlertPolicy {
    fun mode(context: Context): AdhanAlertMode {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return AdhanAlertMode.AUDIBLE
        return when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> AdhanAlertMode.SILENT
            AudioManager.RINGER_MODE_VIBRATE -> AdhanAlertMode.VIBRATE
            else -> AdhanAlertMode.AUDIBLE
        }
    }

    /** Applies [playWhenSilent] user override on top of device ringer mode. */
    fun effectiveMode(
        context: Context,
        playWhenSilent: Boolean,
    ): AdhanAlertMode {
        val deviceMode = mode(context)
        return if (playWhenSilent && deviceMode != AdhanAlertMode.AUDIBLE) {
            AdhanAlertMode.AUDIBLE
        } else {
            deviceMode
        }
    }

    fun shouldPlayAdhanAudio(
        context: Context,
        playWhenSilent: Boolean = false,
    ): Boolean = effectiveMode(context, playWhenSilent) == AdhanAlertMode.AUDIBLE
}
