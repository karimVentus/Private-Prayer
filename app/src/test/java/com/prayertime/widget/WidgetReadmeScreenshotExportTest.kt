package com.prayertime.widget

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.test.core.app.ApplicationProvider
import com.prayertime.R
import com.prayertime.data.local.AppPreferencesDataSource
import com.prayertime.domain.model.HijriDate
import com.prayertime.domain.model.IslamicEvent
import com.prayertime.domain.model.Prayer
import com.prayertime.domain.model.PrayerTime
import com.prayertime.domain.model.UpcomingEvent
import com.prayertime.ui.theme.AppTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/**
 * Regenerates README widget PNGs under [docs/screenshots].
 *
 * Run: PRAYERTIME_EXPORT_SCREENSHOTS=1 ./scripts/export-readme-widget-screenshots.sh
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetReadmeScreenshotExportTest {
    @Test
    fun exportReadmeWidgetScreenshots() {
        assumeTrue(System.getenv("PRAYERTIME_EXPORT_SCREENSHOTS") == "1")

        val outDir = screenshotDir()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val baseMs = System.currentTimeMillis()
        val snapshot = readmeSnapshot(baseMs)

        exportVariant(
            context = context,
            snapshot = snapshot,
            languageTag = null,
            mediumName = "widget-medium-en-light.png",
            largeName = "",
            compositeName = "widgets-en-light.png",
            outDir = outDir,
        )
        exportVariant(
            context = context,
            snapshot = snapshot,
            languageTag = "ar",
            mediumName = "widget-medium-ar-light.png",
            largeName = "",
            compositeName = "widgets-ar-light.png",
            outDir = outDir,
        )
        exportVariant(
            context = context,
            snapshot = snapshot.copy(appTheme = AppTheme.DARK),
            languageTag = "ar",
            mediumName = "widget-medium-ar-dark.png",
            largeName = "",
            compositeName = "widgets-ar-dark.png",
            outDir = outDir,
        )
        rebuildCompositesFromExistingLarge(outDir)
    }

    private fun rebuildCompositesFromExistingLarge(outDir: File) {
        listOf(
            Triple("widget-medium-en-light.png", "widget-large-en-light.png", "widgets-en-light.png"),
            Triple("widget-medium-ar-light.png", "widget-large-ar-light.png", "widgets-ar-light.png"),
            Triple("widget-medium-ar-dark.png", "widget-large-ar-dark.png", "widgets-ar-dark.png"),
        ).forEach { (medium, large, composite) ->
            val mediumBitmap = android.graphics.BitmapFactory.decodeFile(outDir.resolve(medium).absolutePath)
            val largeBitmap = android.graphics.BitmapFactory.decodeFile(outDir.resolve(large).absolutePath)
            writePng(outDir.resolve(composite), stackVertical(mediumBitmap, largeBitmap, 24))
        }
    }

    private fun exportVariant(
        context: Context,
        snapshot: WidgetSnapshot,
        languageTag: String?,
        mediumName: String,
        largeName: String,
        compositeName: String,
        outDir: File,
    ) {
        val preferences = AppPreferencesDataSource(context)
        runBlocking {
            preferences.setAppLanguageTag(languageTag)
        }
        drainMainLooper()
        val builder = WidgetRemoteViewsBuilder(context, preferences)
        val medium = renderToBitmap(builder.build(snapshot, WidgetSize.MEDIUM), mediumWidthPx(context))
        writePng(outDir.resolve(mediumName), medium)
        if (largeName.isNotEmpty()) {
            val large = renderToBitmap(builder.build(snapshot, WidgetSize.LARGE), largeWidthPx(context))
            writePng(outDir.resolve(largeName), large)
            writePng(outDir.resolve(compositeName), stackVertical(medium, large, gapPx(context, 12)))
        }
    }

    private fun readmeSnapshot(baseMs: Long): WidgetSnapshot {
        val hour = 3_600_000L
        val times =
            listOf(
                PrayerTime(Prayer.FAJR, "02:30", baseMs + 12 * hour),
                PrayerTime(Prayer.SHURUQ, "04:45", baseMs + 14 * hour),
                PrayerTime(Prayer.DHUHR, "13:05", baseMs + 23 * hour),
                PrayerTime(Prayer.ASR, "17:29", baseMs + 3 * hour),
                PrayerTime(Prayer.MAGHRIB, "21:26", baseMs + 7 * hour),
                PrayerTime(Prayer.ISHA, "22:56", baseMs + 9 * hour),
            )
        return WidgetSnapshot(
            state = WidgetSnapshot.State.READY,
            appTheme = AppTheme.LIGHT,
            cityLabel = "Berlin, DE",
            timezone = "Europe/Berlin",
            times = times,
            nextPrayer = Prayer.ASR,
            countdownMillis = 3 * hour,
            hijriDate = HijriDate(year = 1447, month = 12, day = 21),
            upcomingEvent =
                UpcomingEvent(
                    event = IslamicEvent.ISLAMIC_NEW_YEAR,
                    hijriDate = HijriDate(year = 1448, month = 1, day = 1),
                    daysUntil = 10,
                ),
        )
    }

    private fun mediumWidthPx(context: Context): Int = dp(context, 360)

    private fun largeWidthPx(context: Context): Int = dp(context, 360)

    private fun gapPx(
        context: Context,
        dp: Int,
    ): Int = dp(context, dp)

    private fun dp(
        context: Context,
        value: Int,
    ): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics,
        ).toInt()

    private fun renderToBitmap(
        views: RemoteViews,
        widthPx: Int,
    ): Bitmap {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        activity.setTheme(R.style.Theme_PrayerTime)
        val root = views.apply(activity, FrameLayout(activity))
        val widthSpec = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        root.measure(widthSpec, heightSpec)
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
        drainMainLooper()
        val bitmap = Bitmap.createBitmap(root.measuredWidth, root.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        root.draw(canvas)
        return bitmap
    }

    private fun stackVertical(
        top: Bitmap,
        bottom: Bitmap,
        gap: Int,
    ): Bitmap {
        val width = maxOf(top.width, bottom.width)
        val height = top.height + gap + bottom.height
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(top, (width - top.width) / 2f, 0f, null)
        canvas.drawBitmap(bottom, (width - bottom.width) / 2f, (top.height + gap).toFloat(), null)
        return out
    }

    private fun writePng(
        file: File,
        bitmap: Bitmap,
    ) {
        FileOutputStream(file).use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                "Failed to write ${file.name}"
            }
        }
    }

    private fun screenshotDir(): File {
        var root = File(checkNotNull(System.getProperty("user.dir")))
        if (root.name == "app") {
            root = root.parentFile ?: root
        }
        return root.resolve("docs/screenshots").also { it.mkdirs() }
    }

    private fun drainMainLooper() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }
}
