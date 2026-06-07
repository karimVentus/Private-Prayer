package com.prayertime.notification

import android.content.Context
import android.media.AudioManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AdhanAlertPolicyTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun setRingerMode(mode: Int) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.ringerMode = mode
    }

    @Test
    fun `mode is AUDIBLE when ringer is normal`() {
        setRingerMode(AudioManager.RINGER_MODE_NORMAL)
        assertEquals(AdhanAlertMode.AUDIBLE, AdhanAlertPolicy.mode(context))
        assertTrue(AdhanAlertPolicy.shouldPlayAdhanAudio(context))
    }

    @Test
    fun `mode is VIBRATE when ringer is vibrate`() {
        setRingerMode(AudioManager.RINGER_MODE_VIBRATE)
        assertEquals(AdhanAlertMode.VIBRATE, AdhanAlertPolicy.mode(context))
        assertFalse(AdhanAlertPolicy.shouldPlayAdhanAudio(context))
    }

    @Test
    fun `mode is SILENT when ringer is silent`() {
        setRingerMode(AudioManager.RINGER_MODE_SILENT)
        assertEquals(AdhanAlertMode.SILENT, AdhanAlertPolicy.mode(context))
        assertFalse(AdhanAlertPolicy.shouldPlayAdhanAudio(context))
    }

    @Test
    fun `playWhenSilent override treats silent ringer as AUDIBLE`() {
        setRingerMode(AudioManager.RINGER_MODE_SILENT)
        assertEquals(AdhanAlertMode.AUDIBLE, AdhanAlertPolicy.effectiveMode(context, playWhenSilent = true))
        assertTrue(AdhanAlertPolicy.shouldPlayAdhanAudio(context, playWhenSilent = true))
    }

    @Test
    fun `playWhenSilent override treats vibrate ringer as AUDIBLE`() {
        setRingerMode(AudioManager.RINGER_MODE_VIBRATE)
        assertEquals(AdhanAlertMode.AUDIBLE, AdhanAlertPolicy.effectiveMode(context, playWhenSilent = true))
        assertTrue(AdhanAlertPolicy.shouldPlayAdhanAudio(context, playWhenSilent = true))
    }

    @Test
    fun `playWhenSilent false keeps silent ringer silent`() {
        setRingerMode(AudioManager.RINGER_MODE_SILENT)
        assertEquals(AdhanAlertMode.SILENT, AdhanAlertPolicy.effectiveMode(context, playWhenSilent = false))
        assertFalse(AdhanAlertPolicy.shouldPlayAdhanAudio(context, playWhenSilent = false))
    }
}
