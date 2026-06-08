package com.prayertime.widget

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.prayertime.R
import com.prayertime.data.local.AppPreferencesDataSource
import com.prayertime.domain.calculator.HijriCalculator
import com.prayertime.domain.model.HijriDate
import com.prayertime.domain.model.Prayer
import com.prayertime.domain.model.PrayerTime
import com.prayertime.ui.HijriDateFormatter
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.TimeZone

/**
 * Fast smoke tests for WidgetRemoteViewsBuilder.
 *
 * AppPreferencesDataSource is mocked — no DataStore flows are opened, so Robolectric's
 * main-looper never blocks. Each test is independent and completes in well under 1 second.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetRemoteViewsBuilderTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    // Mock preferences — WidgetRemoteViewsBuilder only calls readAppLanguageTagSync().
    // Returning null means "use system locale", which is correct for these tests.
    private val preferences: AppPreferencesDataSource =
        mockk {
            every { readAppLanguageTagSync() } returns null
            every { readAppThemeSync() } returns com.prayertime.ui.theme.AppTheme.LIGHT
        }

    private val builder = WidgetRemoteViewsBuilder(context, preferences)
    private val berlin = TimeZone.getTimeZone("Europe/Berlin")

    // ── NO_CITY ──────────────────────────────────────────────────────────────

    @Test
    fun build_noCityLarge_inflatesWithoutCrash() {
        applyWidget(builder.build(WidgetSnapshot(WidgetSnapshot.State.NO_CITY), WidgetSize.LARGE))
    }

    @Test
    fun build_noCityMedium_showsSetupMessage() {
        val widget = applyWidget(builder.build(WidgetSnapshot(WidgetSnapshot.State.NO_CITY), WidgetSize.MEDIUM))
        assertEquals(context.getString(R.string.widget_no_city), widget.text(R.id.widget_empty))
    }

    // ── ERROR ─────────────────────────────────────────────────────────────────

    @Test
    fun build_errorLarge_inflatesWithoutCrash() {
        applyWidget(builder.build(errorSnapshot(), WidgetSize.LARGE))
    }

    @Test
    fun build_errorMedium_showsErrorMessageAndHijriDate() {
        val hijri = HijriCalculator.gregorianToHijri(2024, 6, 4)
        val widget = applyWidget(builder.build(errorSnapshot(hijri), WidgetSize.MEDIUM))
        assertEquals(context.getString(R.string.widget_error), widget.text(R.id.widget_empty))
        assertEquals(HijriDateFormatter.format(hijri, context.resources), widget.text(R.id.widget_hijri))
    }

    // ── STALE ─────────────────────────────────────────────────────────────────

    @Test
    fun build_staleLarge_inflatesWithoutCrash() {
        applyWidget(builder.build(staleSnapshot(), WidgetSize.LARGE))
    }

    @Test
    fun build_staleMedium_showsStaleBannerAndPrayerColumns() {
        val hijri = HijriCalculator.gregorianToHijri(2024, 6, 4)
        val widget = applyWidget(builder.build(staleSnapshot(hijri), WidgetSize.MEDIUM))
        assertEquals(context.getString(R.string.widget_stale), widget.text(R.id.widget_empty))
        assertEquals(HijriDateFormatter.format(hijri, context.resources), widget.text(R.id.widget_hijri))
        assertEquals(context.getString(R.string.fajr), widget.text(R.id.widget_prayer_0))
        assertEquals("04:00", widget.text(R.id.widget_time_0))
    }

    // ── READY ─────────────────────────────────────────────────────────────────

    @Test
    fun build_readyLarge_showsCityHijriPrayerNamesAndTimes() {
        val hijri = HijriCalculator.gregorianToHijri(2024, 6, 4)
        val times = sampleTimes()
        val widget = applyWidget(builder.build(readySnapshot(hijri, times), WidgetSize.LARGE))
        assertEquals("Hameln, DE", widget.text(R.id.widget_city))
        assertEquals(HijriDateFormatter.format(hijri, context.resources), widget.text(R.id.widget_hijri))
        assertEquals(context.getString(R.string.widget_m_fajr), widget.text(R.id.widget_prayer_0))
        assertEquals("04:00", widget.text(R.id.widget_time_0))
        assertEquals(context.getString(R.string.widget_m_dhuhr), widget.text(R.id.widget_prayer_2))
        assertEquals("12:30", widget.text(R.id.widget_time_2))
        assertTrue(
            "L-widget next-prayer column shows countdown",
            widget.text(R.id.widget_countdown_0).contains("h"),
        )
    }

    @Test
    fun build_readyMedium_showsPrayerNamesAndTimesWithoutTruncation() {
        val widget =
            applyWidget(
                builder.build(readySnapshot(HijriCalculator.gregorianToHijri(2024, 6, 4), sampleTimes()), WidgetSize.MEDIUM),
            )
        assertEquals("04:00", widget.text(R.id.widget_time_0))
        assertEquals("12:30", widget.text(R.id.widget_time_2))
        // Medium widget shows no countdown column
        assertEquals("", widget.text(R.id.widget_countdown_0))
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun errorSnapshot(hijri: HijriDate = HijriCalculator.gregorianToHijri(2024, 6, 4)) =
        WidgetSnapshot(state = WidgetSnapshot.State.ERROR, cityLabel = "Hameln, DE", hijriDate = hijri)

    private fun staleSnapshot(hijri: HijriDate = HijriCalculator.gregorianToHijri(2024, 6, 4)) =
        WidgetSnapshot(
            state = WidgetSnapshot.State.STALE,
            cityLabel = "Hameln, DE",
            times = sampleTimes(),
            nextPrayer = Prayer.FAJR,
            countdownMillis = 3_600_000L,
            hijriDate = hijri,
        )

    private fun readySnapshot(
        hijri: HijriDate,
        times: List<PrayerTime>,
    ) = WidgetSnapshot(
        state = WidgetSnapshot.State.READY,
        cityLabel = "Hameln, DE",
        timezone = berlin.id,
        times = times,
        nextPrayer = Prayer.FAJR,
        countdownMillis = 3_600_000L,
        hijriDate = hijri,
    )

    private fun sampleTimes(): List<PrayerTime> {
        val base = System.currentTimeMillis()
        val h = 3_600_000L
        return listOf(
            PrayerTime(Prayer.FAJR, "04:00", base + 2 * h),
            PrayerTime(Prayer.SHURUQ, "05:30", base + 3 * h),
            PrayerTime(Prayer.DHUHR, "12:30", base + 10 * h),
            PrayerTime(Prayer.ASR, "16:00", base + 13 * h),
            PrayerTime(Prayer.MAGHRIB, "19:00", base + 16 * h),
            PrayerTime(Prayer.ISHA, "20:30", base + 18 * h),
        )
    }

    private fun applyWidget(views: RemoteViews): AppliedWidget = AppliedWidget(views.apply(context, FrameLayout(context)))

    private class AppliedWidget(private val root: View) {
        fun text(viewId: Int): String = (root.findViewById<View>(viewId) as? TextView)?.text?.toString().orEmpty()
    }
}
