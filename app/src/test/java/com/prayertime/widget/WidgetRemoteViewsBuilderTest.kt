package com.prayertime.widget

import android.content.Context
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.prayertime.R
import com.prayertime.data.local.AppPreferencesDataSource
import com.prayertime.domain.calculator.HijriCalculator
import com.prayertime.domain.calculator.PrayerTimeCalculator
import com.prayertime.domain.model.HijriDate
import com.prayertime.domain.model.Prayer
import com.prayertime.domain.model.PrayerTime
import com.prayertime.locale.AppLocale
import com.prayertime.ui.HijriDateFormatter
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetRemoteViewsBuilderTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preferences = AppPreferencesDataSource(context)
    private val builder = WidgetRemoteViewsBuilder(context, preferences)
    private val berlin = TimeZone.getTimeZone("Europe/Berlin")

    @After
    fun drainMainLooper() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun build_allStatesAndSizes_produceInflatableRemoteViews() {
        val baseMs = System.currentTimeMillis()
        val hijri = HijriCalculator.gregorianToHijri(2024, 6, 4)
        val times = sampleTimes(baseMs)
        val snapshots =
            listOf(
                WidgetSnapshot(WidgetSnapshot.State.NO_CITY),
                WidgetSnapshot(WidgetSnapshot.State.ERROR, cityLabel = "Hameln, DE", hijriDate = hijri),
                WidgetSnapshot(
                    state = WidgetSnapshot.State.STALE,
                    cityLabel = "Hameln, DE",
                    times = times,
                    nextPrayer = Prayer.FAJR,
                    countdownMillis = 3_600_000L,
                    hijriDate = hijri,
                ),
                WidgetSnapshot(
                    state = WidgetSnapshot.State.READY,
                    cityLabel = "Hameln, DE",
                    times = times,
                    nextPrayer = Prayer.FAJR,
                    countdownMillis = 3_600_000L,
                    hijriDate = hijri,
                ),
            )
        for (snapshot in snapshots) {
            for (size in WidgetSize.entries) {
                applyWidget(builder.build(snapshot, size))
            }
        }
    }

    @Test
    fun build_noCityMedium_showsSetupMessage() {
        val widget = applyWidget(builder.build(WidgetSnapshot(WidgetSnapshot.State.NO_CITY), WidgetSize.MEDIUM))
        assertEquals(context.getString(R.string.widget_no_city), widget.text(R.id.widget_empty))
    }

    @Test
    fun build_errorMedium_showsErrorMessageAndHijriDate() {
        val hijri = HijriCalculator.gregorianToHijri(2024, 6, 4)
        val snapshot =
            WidgetSnapshot(
                state = WidgetSnapshot.State.ERROR,
                cityLabel = "Hameln, DE",
                hijriDate = hijri,
            )
        val widget = applyWidget(builder.build(snapshot, WidgetSize.MEDIUM))
        assertEquals(context.getString(R.string.widget_error), widget.text(R.id.widget_empty))
        assertEquals(HijriDateFormatter.format(hijri, context.resources), widget.text(R.id.widget_hijri))
    }

    @Test
    fun build_readyLarge_showsCityHijriPrayerNamesAndTimes() {
        val baseMs = System.currentTimeMillis()
        val hijri = HijriCalculator.gregorianToHijri(2024, 6, 4)
        val times = sampleTimes(baseMs)
        val snapshot = readySnapshot(hijri, times)
        val widget = applyWidget(builder.build(snapshot, WidgetSize.LARGE))
        assertEquals("Hameln, DE", widget.text(R.id.widget_city))
        assertEquals(HijriDateFormatter.format(hijri, context.resources), widget.text(R.id.widget_hijri))
        assertEquals(context.getString(R.string.fajr), widget.text(R.id.widget_prayer_0))
        assertEquals("04:00", widget.text(R.id.widget_time_0))
        assertEquals(context.getString(R.string.dhuhr), widget.text(R.id.widget_prayer_2))
        assertEquals("12:30", widget.text(R.id.widget_time_2))
        val countdown = widget.text(R.id.widget_countdown_0)
        assertTrue(countdown.isNotEmpty())
        assertTrue(countdown.contains(context.getString(R.string.countdown_hours)))
    }

    @Test
    fun build_readyMedium_showsPrayerNamesAndTimesWithoutTruncation() {
        val baseMs = System.currentTimeMillis()
        val snapshot = readySnapshot(HijriCalculator.gregorianToHijri(2024, 6, 4), sampleTimes(baseMs))
        val widget = applyWidget(builder.build(snapshot, WidgetSize.MEDIUM))
        assertEquals("04:00", widget.text(R.id.widget_time_0))
        assertEquals("12:30", widget.text(R.id.widget_time_2))
        assertEquals("", widget.text(R.id.widget_countdown_0))
    }

    @Test
    fun build_readyLarge_countdownIgnoresStaleSnapshotValue() {
        val baseMs = System.currentTimeMillis()
        val times = sampleTimes(baseMs)
        val snapshot =
            readySnapshot(HijriCalculator.gregorianToHijri(2024, 6, 4), times)
                .copy(countdownMillis = 27 * 60_000L)
        val widget = applyWidget(builder.build(snapshot, WidgetSize.LARGE))
        val countdown = widget.text(R.id.widget_countdown_0)
        val freshMillis =
            PrayerTimeCalculator.millisUntilNextOccurrence(
                times.first().timestamp,
                System.currentTimeMillis(),
                berlin,
            )
        val expected =
            CountdownFormatter.format(
                freshMillis,
                context.getString(R.string.countdown_hours),
                context.getString(R.string.countdown_minutes),
            )
        assertEquals(expected, countdown)
        assertFalse(countdown.contains("27"))
    }

    @Test
    fun build_readyMedium_usesPersistedArabicWhenAppCompatLocalesEmpty() =
        runBlocking {
            preferences.setAppLanguageTag("ar")
            drainMainLooper()
            AppLocale.apply(null)
            val baseMs = System.currentTimeMillis()
            val snapshot =
                readySnapshot(
                    HijriCalculator.gregorianToHijri(2024, 6, 4),
                    sampleTimes(baseMs),
                )
            val widget = applyWidget(builder.build(snapshot, WidgetSize.MEDIUM))
            val localized = context.withAppWidgetLocale("ar")
            assertEquals(localized.getString(R.string.widget_m_fajr), widget.text(R.id.widget_prayer_0))
        }

    @Test
    fun build_staleMedium_showsStaleBannerAndPrayerColumns() {
        val baseMs = System.currentTimeMillis()
        val hijri = HijriCalculator.gregorianToHijri(2024, 6, 4)
        val snapshot =
            WidgetSnapshot(
                state = WidgetSnapshot.State.STALE,
                cityLabel = "Hameln, DE",
                times = sampleTimes(baseMs),
                nextPrayer = Prayer.FAJR,
                countdownMillis = 3_600_000L,
                hijriDate = hijri,
            )
        val widget = applyWidget(builder.build(snapshot, WidgetSize.MEDIUM))
        assertEquals(context.getString(R.string.widget_stale), widget.text(R.id.widget_empty))
        assertEquals(HijriDateFormatter.format(hijri, context.resources), widget.text(R.id.widget_hijri))
        assertEquals(context.getString(R.string.fajr), widget.text(R.id.widget_prayer_0))
        assertEquals("04:00", widget.text(R.id.widget_time_0))
    }

    private fun readySnapshot(
        hijri: HijriDate,
        times: List<PrayerTime>,
    ): WidgetSnapshot =
        WidgetSnapshot(
            state = WidgetSnapshot.State.READY,
            cityLabel = "Hameln, DE",
            timezone = berlin.id,
            times = times,
            nextPrayer = Prayer.FAJR,
            countdownMillis = 3_600_000L,
            hijriDate = hijri,
        )

    private fun sampleTimes(baseMs: Long): List<PrayerTime> {
        val hour = 3_600_000L
        return listOf(
            PrayerTime(Prayer.FAJR, "04:00", baseMs + 2 * hour),
            PrayerTime(Prayer.SHURUQ, "05:30", baseMs + 3 * hour),
            PrayerTime(Prayer.DHUHR, "12:30", baseMs + 10 * hour),
            PrayerTime(Prayer.ASR, "16:00", baseMs + 13 * hour),
            PrayerTime(Prayer.MAGHRIB, "19:00", baseMs + 16 * hour),
            PrayerTime(Prayer.ISHA, "20:30", baseMs + 18 * hour),
        )
    }

    private fun applyWidget(views: RemoteViews): AppliedWidget {
        val root = views.apply(context, FrameLayout(context))
        drainMainLooper()
        return AppliedWidget(root)
    }

    private class AppliedWidget(private val root: View) {
        fun text(viewId: Int): String = (root.findViewById<View>(viewId) as? TextView)?.text?.toString().orEmpty()
    }
}
