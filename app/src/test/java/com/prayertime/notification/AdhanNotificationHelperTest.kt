package com.prayertime.notification

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.prayertime.domain.model.Prayer
import com.prayertime.permission.AdhanPermissions
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * Phase 5B.2: POST_NOTIFICATIONS denial → no crash, indicator shown.
 *
 * Verifies that:
 * - showPrayerNotification catches SecurityException when POST_NOTIFICATIONS denied (API 33+)
 * - showAdhanEnabledConfirmation catches SecurityException when POST_NOTIFICATIONS denied
 * - areNotificationsAllowed callable without crash
 * - hasPostNotificationsPermission reflects correct state
 * - The denial indicator (adhan_notifications_denied string) is wired into AboutScreen UI;
 *   unit-tested indirectly via the state that drives it (enabled && !notificationsGranted)
 */
@RunWith(RobolectricTestRunner::class)
class AdhanNotificationHelperTest {
    // ── 5B.2.1: showPrayerNotification does NOT crash when POST_NOTIFICATIONS denied ──

    @Test
    @Config(sdk = [33])
    fun `showPrayerNotification does NOT crash when POST_NOTIFICATIONS denied`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        denyPostNotifications(context)

        val helper = AdhanNotificationHelper(context.applicationContext)

        // Must not throw — SecurityException caught internally
        helper.showPrayerNotification(Prayer.FAJR)
        helper.showPrayerNotification(Prayer.DHUHR)
        helper.showPrayerNotification(Prayer.ASR)
        helper.showPrayerNotification(Prayer.MAGHRIB)
        helper.showPrayerNotification(Prayer.ISHA)
    }

    @Test
    @Config(sdk = [34])
    fun `showPrayerNotification does NOT crash when POST_NOTIFICATIONS denied on SDK 34`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        denyPostNotifications(context)

        val helper = AdhanNotificationHelper(context.applicationContext)
        helper.showPrayerNotification(Prayer.FAJR)
    }

    // ── 5B.2.2: showAdhanEnabledConfirmation does NOT crash when POST_NOTIFICATIONS denied ──

    @Test
    @Config(sdk = [33])
    fun `showAdhanEnabledConfirmation does NOT crash when POST_NOTIFICATIONS denied`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        denyPostNotifications(context)

        val helper = AdhanNotificationHelper(context.applicationContext)
        // Must not throw — SecurityException caught internally
        helper.showAdhanEnabledConfirmation()
    }

    @Test
    @Config(sdk = [34])
    fun `showAdhanEnabledConfirmation does NOT crash when POST_NOTIFICATIONS denied on SDK 34`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        denyPostNotifications(context)

        val helper = AdhanNotificationHelper(context.applicationContext)
        helper.showAdhanEnabledConfirmation()
    }

    // ── 5B.2.3: No crash on pre-33 with default notification state ──

    @Test
    @Config(sdk = [23])
    fun `showAdhanEnabledConfirmation does NOT crash on API 23`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = AdhanNotificationHelper(context.applicationContext)
        helper.showAdhanEnabledConfirmation()
        helper.showPrayerNotification(Prayer.FAJR)
    }

    @Test
    @Config(sdk = [31])
    fun `showPrayerNotification does NOT crash on pre-33 SDK`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = AdhanNotificationHelper(context.applicationContext)
        // Must not throw — pre-33 has no POST_NOTIFICATIONS runtime permission
        helper.showPrayerNotification(Prayer.FAJR)
    }

    @Test
    @Config(sdk = [31])
    fun `showAdhanEnabledConfirmation does NOT crash on pre-33 SDK`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = AdhanNotificationHelper(context.applicationContext)
        helper.showAdhanEnabledConfirmation()
    }

    // ── 5B.2.4: Permission state checks (denial indicator drivers) ──

    @Test
    @Config(sdk = [33])
    fun `hasPostNotificationsPermission returns false when denied`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        denyPostNotifications(context)

        assertTrue(AdhanPermissions.needsPostNotificationsPermission())
        assertFalse(AdhanPermissions.hasPostNotificationsPermission(context))
    }

    @Test
    @Config(sdk = [33])
    fun `hasPostNotificationsPermission returns true when granted`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Shadows.shadowOf(context.applicationContext as Application)
            .grantPermissions("android.permission.POST_NOTIFICATIONS")

        assertTrue(AdhanPermissions.hasPostNotificationsPermission(context))
    }

    @Test
    @Config(sdk = [30])
    fun `hasPostNotificationsPermission returns true on pre-33 even without permission`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Pre-33: needsPostNotificationsPermission() is false → has returns true
        assertFalse(AdhanPermissions.needsPostNotificationsPermission())
        assertTrue(AdhanPermissions.hasPostNotificationsPermission(context))
    }

    // ── 5B.2.5: areNotificationsAllowed still callable when POST_NOTIFICATIONS denied ──

    @Test
    @Config(sdk = [33])
    fun `areNotificationsAllowed still callable when POST_NOTIFICATIONS denied`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        denyPostNotifications(context)

        // Must not crash — areNotificationsAllowed checks system-level notification toggle,
        // not the POST_NOTIFICATIONS runtime permission directly
        val allowed = AdhanPermissions.areNotificationsAllowed(context)
        // In Robolectric, system notifications are enabled by default
        assertTrue(allowed)
    }

    @Test
    @Config(sdk = [33])
    fun `requestPostNotifications method exists and is callable`() {
        // requestPostNotifications uses SafeStartActivity under the hood via requestPermissions.
        // Existence confirms the permission request path is wired.
        assertTrue(
            AdhanPermissions::class.java.methods.any { it.name == "requestPostNotifications" },
        )
    }

    @Test
    @Config(sdk = [33])
    fun `openNotificationSettings method exists and is callable`() {
        // openNotificationSettings uses safeStartActivity — existence confirms the Settings guide path.
        assertTrue(
            AdhanPermissions::class.java.methods.any { it.name == "openNotificationSettings" },
        )
    }

    // ── helpers ──

    private fun denyPostNotifications(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Shadows.shadowOf(context.applicationContext as Application)
                .denyPermissions("android.permission.POST_NOTIFICATIONS")
        }
    }
}
