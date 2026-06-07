package com.prayertime.notification

import androidx.test.core.app.ApplicationProvider
import com.prayertime.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AdhanSoundResolverTest {

    @Test
    fun `rawResFor adhan returns R raw adhan`() {
        assertEquals(R.raw.adhan, AdhanSoundResolver.rawResFor("adhan"))
    }

    @Test
    fun `rawResFor alhram returns R raw alhram`() {
        assertEquals(R.raw.alhram, AdhanSoundResolver.rawResFor("alhram"))
    }

    @Test
    fun `rawResFor nonexistent falls back to R raw adhan`() {
        assertEquals(R.raw.adhan, AdhanSoundResolver.rawResFor("nonexistent"))
    }

    @Test
    fun `rawResFor empty string falls back to R raw adhan`() {
        assertEquals(R.raw.adhan, AdhanSoundResolver.rawResFor(""))
    }

    @Test
    fun `DEFAULT_KEY equals adhan`() {
        assertEquals("adhan", AdhanSoundResolver.DEFAULT_KEY)
    }
}
