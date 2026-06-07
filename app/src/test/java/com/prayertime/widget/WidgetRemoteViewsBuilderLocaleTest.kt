package com.prayertime.widget

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.prayertime.R
import com.prayertime.data.local.AppPreferencesDataSource
import com.prayertime.domain.calculator.HijriCalculator
import com.prayertime.domain.model.Prayer
import com.prayertime.domain.model.PrayerTime
import com.prayertime.locale.AppLocale
import com.prayertime.ui.theme.AppTheme
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.TimeZone

/**
 * Tests WidgetRemoteViewsBuilder Arabic locale path — no DataStore interaction.
 *
 * The mock returns "ar" directly so we never open a DataStore flow, which would
 * deadlock Robolectric's main-looper.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetRemoteViewsBuilderLocaleTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val berlin = TimeZone.getTimeZone("Europe/Berlin")

    private val preferences: AppPreferencesDataSource =
        mockk {
            every { readAppLanguageTagSync() } returns "ar"
            every { readAppThemeSync() } returns AppTheme.LIGHT
        }
    private val builder = WidgetRemoteViewsBuilder(context, preferences)

    @Test
    fun build_readyMedium_usesArabicLocaleForPrayerLabels() {
        AppLocale.apply(null)

        val base = System.currentTimeMillis()
        val h = 3_600_000L
        val times =
            listOf(
                PrayerTime(Prayer.FAJR, "04:00", base + 2 * h),
                PrayerTime(Prayer.SHURUQ, "05:30", base + 3 * h),
                PrayerTime(Prayer.DHUHR, "12:30", base + 10 * h),
                PrayerTime(Prayer.ASR, "16:00", base + 13 * h),
                PrayerTime(Prayer.MAGHRIB, "19:00", base + 16 * h),
                PrayerTime(Prayer.ISHA, "20:30", base + 18 * h),
            )
        val snapshot =
            WidgetSnapshot(
                state = WidgetSnapshot.State.READY,
                cityLabel = "Hameln, DE",
                timezone = berlin.id,
                times = times,
                nextPrayer = Prayer.FAJR,
                countdownMillis = 3_600_000L,
                hijriDate = HijriCalculator.gregorianToHijri(2024, 6, 4),
            )

        val root = builder.build(snapshot, WidgetSize.MEDIUM).apply(context, FrameLayout(context))
        val localized = context.withAppWidgetLocale("ar")
        val fajrLabel =
            (root.findViewById<View>(R.id.widget_prayer_0) as? TextView)
                ?.text?.toString().orEmpty()
        assertEquals(localized.getString(R.string.widget_m_fajr), fajrLabel)
    }
}
