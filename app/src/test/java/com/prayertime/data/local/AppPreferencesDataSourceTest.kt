package com.prayertime.data.local

import androidx.test.core.app.ApplicationProvider
import com.prayertime.ui.theme.AppTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppPreferencesDataSourceTest {
    private fun prefs() = AppPreferencesDataSource(ApplicationProvider.getApplicationContext())

    // ── Adhan enabled ──

    @Test
    fun `adhan enabled toggles true then false`() =
        runTest {
            val p = prefs()
            // Set to known state first — DataStore persists across tests in Robolectric.
            p.setAdhanNotificationsEnabled(false)
            assertFalse(p.adhanNotificationsEnabled.first())
            p.setAdhanNotificationsEnabled(true)
            assertTrue(p.adhanNotificationsEnabled.first())
            p.setAdhanNotificationsEnabled(false)
            assertFalse(p.adhanNotificationsEnabled.first())
        }

    // ── Language tag ──

    @Test
    fun `language tag persists and reads back`() =
        runTest {
            val p = prefs()
            p.setAppLanguageTag("ar")
            assertEquals("ar", p.appLanguageTag.first())
            assertEquals("ar", p.readAppLanguageTagOnce())
        }

    @Test
    fun `language tag null removes the key`() =
        runTest {
            val p = prefs()
            p.setAppLanguageTag("ar")
            p.setAppLanguageTag(null)
            assertNull(p.appLanguageTag.first())
        }

    // ── Adhan sound ──

    @Test
    fun `adhan sound persists and reads back`() =
        runTest {
            val p = prefs()
            // Set to known value first — DataStore persists across tests.
            p.setAdhanSound("adhan")
            assertEquals("adhan", p.adhanSound.first())
            p.setAdhanSound("almadina")
            assertEquals("almadina", p.adhanSound.first())
            p.setAdhanSound("alhram")
            assertEquals("alhram", p.adhanSound.first())
        }

    // ── App theme ──

    @Test
    fun `app theme persists and reads back`() =
        runTest {
            val p = prefs()
            // Set to a known value first — DataStore may have leftover state from prior tests.
            p.setAppTheme("green")
            assertEquals("green", p.appTheme.first())
            assertEquals("green", p.readAppThemeOnce())
            assertEquals(AppTheme.GREEN, p.readAppThemeSync())
            p.setAppTheme("dark")
            assertEquals("dark", p.appTheme.first())
            assertEquals(AppTheme.DARK, p.readAppThemeSync())
        }

    @Test
    fun `warmAppThemeCache syncs DataStore theme to SharedPreferences`() =
        runTest {
            val p = prefs()
            p.setAppTheme("green")
            p.warmAppThemeCache()
            assertEquals(AppTheme.GREEN, p.readAppThemeSync())
        }

    // ── Round trip: all preferences survive multiple writes ──

    @Test
    fun `all preferences coexist and survive interleaved writes`() =
        runTest {
            val p = prefs()

            p.setAdhanNotificationsEnabled(true)
            p.setAppLanguageTag("ar")
            p.setAdhanSound("alhram")
            p.setAppTheme("dark")

            assertTrue(p.adhanNotificationsEnabled.first())
            assertEquals("ar", p.appLanguageTag.first())
            assertEquals("alhram", p.adhanSound.first())
            assertEquals("dark", p.appTheme.first())

            // Toggle back
            p.setAdhanNotificationsEnabled(false)
            p.setAppLanguageTag(null)
            p.setAdhanSound("adhan")
            p.setAppTheme("light")

            assertFalse(p.adhanNotificationsEnabled.first())
            assertNull(p.appLanguageTag.first())
            assertEquals("adhan", p.adhanSound.first())
            assertEquals("light", p.appTheme.first())
        }
}
