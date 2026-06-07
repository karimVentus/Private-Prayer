package com.prayertime.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.ColorRes
import com.prayertime.PendingIntentRequestCodes
import com.prayertime.R
import com.prayertime.data.local.AppPreferencesDataSource
import com.prayertime.domain.calculator.PrayerTimeCalculator
import com.prayertime.domain.model.Prayer
import com.prayertime.domain.model.PrayerTime
import com.prayertime.ui.HijriDateFormatter
import com.prayertime.ui.MainActivity
import com.prayertime.ui.theme.AppTheme
import com.prayertime.ui.theme.ThemePalettes
import com.prayertime.ui.theme.WidgetPalette
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

enum class WidgetSize(val layoutRes: Int) {
    MEDIUM(R.layout.widget_prayer_times_medium),
    LARGE(R.layout.widget_prayer_times_large),
}

@Singleton
class WidgetRemoteViewsBuilder
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val preferences: AppPreferencesDataSource,
    ) {
        private fun localized(): Context = context.withAppWidgetLocale(preferences.readAppLanguageTagSync())

        private data class ColIds(
            val nameColId: Int,
            val timeColId: Int,
            val prayerId: Int,
            val timeId: Int,
            val countdownId: Int,
        )

        private val colIds =
            listOf(
                ColIds(R.id.widget_col_0, R.id.widget_time_col_0, R.id.widget_prayer_0, R.id.widget_time_0, R.id.widget_countdown_0),
                ColIds(R.id.widget_col_1, R.id.widget_time_col_1, R.id.widget_prayer_1, R.id.widget_time_1, R.id.widget_countdown_1),
                ColIds(R.id.widget_col_2, R.id.widget_time_col_2, R.id.widget_prayer_2, R.id.widget_time_2, R.id.widget_countdown_2),
                ColIds(R.id.widget_col_3, R.id.widget_time_col_3, R.id.widget_prayer_3, R.id.widget_time_3, R.id.widget_countdown_3),
                ColIds(R.id.widget_col_4, R.id.widget_time_col_4, R.id.widget_prayer_4, R.id.widget_time_4, R.id.widget_countdown_4),
                ColIds(R.id.widget_col_5, R.id.widget_time_col_5, R.id.widget_prayer_5, R.id.widget_time_5, R.id.widget_countdown_5),
            )

        private val mediumHighlightIds =
            listOf(
                R.id.widget_highlight_0,
                R.id.widget_highlight_1,
                R.id.widget_highlight_2,
                R.id.widget_highlight_3,
                R.id.widget_highlight_4,
                R.id.widget_highlight_5,
            )

        fun build(
            snapshot: WidgetSnapshot,
            size: WidgetSize,
        ): RemoteViews {
            val l10n = localized()
            val widgetColors = ThemePalettes.widget(snapshot.appTheme)
            val views = RemoteViews(context.packageName, size.layoutRes)
            applyWidgetChrome(views, widgetColors)
            views.setOnClickPendingIntent(R.id.widget_root, launchPendingIntent())
            when (snapshot.state) {
                WidgetSnapshot.State.NO_CITY ->
                    bindEmpty(views, size, l10n.getString(R.string.widget_no_city), widgetColors)
                WidgetSnapshot.State.ERROR ->
                    bindError(views, snapshot, size, l10n, widgetColors)
                WidgetSnapshot.State.STALE ->
                    bindStale(views, snapshot, size, l10n, widgetColors)
                WidgetSnapshot.State.READY ->
                    bindReady(views, snapshot, size, l10n, widgetColors)
            }
            return views
        }

        /** Minimal themed shell for sync bind before snapshot load. */
        fun buildThemeChrome(
            size: WidgetSize,
            theme: AppTheme,
        ): RemoteViews {
            val colors = ThemePalettes.widget(theme)
            val views = RemoteViews(context.packageName, initialLayoutFor(size))
            views.setInt(R.id.widget_root, "setBackgroundResource", colors.backgroundDrawable)
            return views
        }

        fun initialLayoutFor(size: WidgetSize): Int =
            when (size) {
                WidgetSize.MEDIUM -> R.layout.widget_initial_medium
                WidgetSize.LARGE -> R.layout.widget_initial_large
            }

        private fun widgetColor(
            @ColorRes colorRes: Int,
        ): Int = context.getColor(colorRes)

        private fun applyWidgetChrome(
            views: RemoteViews,
            colors: WidgetPalette,
        ) {
            views.setInt(R.id.widget_root, "setBackgroundResource", colors.backgroundDrawable)
            setTextColorIfPresent(views, R.id.widget_city, widgetColor(colors.textSecondary))
            setTextColorIfPresent(views, R.id.widget_hijri, widgetColor(colors.textSecondary))
            setTextColorIfPresent(views, R.id.widget_event, widgetColor(colors.textAccent))
            setTextColorIfPresent(views, R.id.widget_empty, widgetColor(colors.textSecondary))
            setTextColorIfPresent(views, R.id.widget_clock, widgetColor(colors.textSecondary))
            colIds.forEach { ids ->
                setTextColorIfPresent(views, ids.prayerId, widgetColor(colors.textPrimary))
                setTextColorIfPresent(views, ids.timeId, widgetColor(colors.textPrimary))
                setTextColorIfPresent(views, ids.countdownId, widgetColor(colors.textPrimary))
            }
        }

        private fun setTextColorIfPresent(
            views: RemoteViews,
            viewId: Int,
            color: Int,
        ) {
            runCatching { views.setTextColor(viewId, color) }
        }

        private fun setViewVisibilityIfPresent(
            views: RemoteViews,
            viewId: Int,
            visibility: Int,
        ) {
            runCatching { views.setViewVisibility(viewId, visibility) }
        }

        private fun bindEmpty(
            views: RemoteViews,
            size: WidgetSize,
            message: String,
            widgetColors: WidgetPalette,
        ) {
            when (size) {
                WidgetSize.MEDIUM -> {
                    views.setViewVisibility(R.id.widget_hijri, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_event, android.view.View.GONE)
                    setViewVisibilityIfPresent(views, R.id.widget_header_row, android.view.View.GONE)
                    setViewVisibilityIfPresent(views, R.id.widget_header_band, android.view.View.GONE)
                    setViewVisibilityIfPresent(views, R.id.widget_names_row, android.view.View.GONE)
                    setViewVisibilityIfPresent(views, R.id.widget_times_row, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_city, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_columns, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_prayer_block, android.view.View.GONE)
                }
                WidgetSize.LARGE -> {
                    views.setViewVisibility(R.id.widget_hijri, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_event, android.view.View.GONE)
                    views.setTextViewText(R.id.widget_city, "")
                    views.setViewVisibility(R.id.widget_clock, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_columns, android.view.View.GONE)
                }
            }
            views.setViewVisibility(R.id.widget_empty, android.view.View.VISIBLE)
            views.setTextViewText(R.id.widget_empty, message)
            views.setTextColor(R.id.widget_empty, widgetColor(widgetColors.textSecondary))
        }

        private fun bindStale(
            views: RemoteViews,
            snapshot: WidgetSnapshot,
            size: WidgetSize,
            l10n: Context,
            widgetColors: WidgetPalette,
        ) {
            bindReady(views, snapshot, size, l10n, widgetColors)
            val staleLabel = l10n.getString(R.string.widget_stale)
            views.setViewVisibility(R.id.widget_empty, android.view.View.VISIBLE)
            views.setTextViewText(R.id.widget_empty, staleLabel)
            views.setTextColor(R.id.widget_empty, widgetColor(widgetColors.textAccent))
        }

        private fun bindError(
            views: RemoteViews,
            snapshot: WidgetSnapshot,
            size: WidgetSize,
            l10n: Context,
            widgetColors: WidgetPalette,
        ) {
            val message = l10n.getString(R.string.widget_error)
            if (snapshot.hijriDate != null) {
                views.setViewVisibility(R.id.widget_empty, android.view.View.GONE)
                when (size) {
                    WidgetSize.MEDIUM -> views.setViewVisibility(R.id.widget_city, android.view.View.GONE)
                    WidgetSize.LARGE -> {
                        views.setTextViewText(R.id.widget_city, snapshot.cityLabel)
                        views.setViewVisibility(R.id.widget_clock, android.view.View.GONE)
                    }
                }
                bindWidgetHijri(views, snapshot, l10n, widgetColors)
                if (size == WidgetSize.MEDIUM) {
                    setViewVisibilityIfPresent(views, R.id.widget_names_row, android.view.View.GONE)
                    setViewVisibilityIfPresent(views, R.id.widget_times_row, android.view.View.GONE)
                }
                views.setViewVisibility(R.id.widget_columns, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_empty, android.view.View.VISIBLE)
                views.setTextViewText(R.id.widget_empty, message)
                views.setTextColor(R.id.widget_empty, widgetColor(widgetColors.textSecondary))
            } else {
                bindEmpty(views, size, message, widgetColors)
            }
        }

        private fun bindReady(
            views: RemoteViews,
            snapshot: WidgetSnapshot,
            size: WidgetSize,
            l10n: Context,
            widgetColors: WidgetPalette,
        ) {
            views.setViewVisibility(R.id.widget_empty, android.view.View.GONE)

            when (size) {
                WidgetSize.MEDIUM -> {
                    views.setViewVisibility(R.id.widget_city, android.view.View.GONE)
                    bindWidgetHijri(views, snapshot, l10n, widgetColors)
                    setViewVisibilityIfPresent(views, R.id.widget_header_band, android.view.View.VISIBLE)
                    setViewVisibilityIfPresent(views, R.id.widget_names_row, android.view.View.VISIBLE)
                    setViewVisibilityIfPresent(views, R.id.widget_times_row, android.view.View.VISIBLE)
                    bindColumns(
                        views,
                        snapshot,
                        l10n,
                        widgetColors,
                        compactCountdown = true,
                        inlineCountdown = false,
                        timeOnly = true,
                        useShortPrayerLabels = true,
                        unifiedColumnHighlight = true,
                    )
                }
                WidgetSize.LARGE -> {
                    views.setTextViewText(R.id.widget_city, snapshot.cityLabel)
                    views.setTextColor(R.id.widget_city, widgetColor(widgetColors.textSecondary))
                    bindWidgetHijri(views, snapshot, l10n, widgetColors)
                    views.setViewVisibility(R.id.widget_clock, android.view.View.VISIBLE)
                    views.setViewVisibility(R.id.widget_columns, android.view.View.VISIBLE)
                    bindColumns(views, snapshot, l10n, widgetColors)
                }
            }
        }

        private fun bindColumns(
            views: RemoteViews,
            snapshot: WidgetSnapshot,
            l10n: Context,
            widgetColors: WidgetPalette,
            compactCountdown: Boolean = false,
            inlineCountdown: Boolean = false,
            timeOnly: Boolean = false,
            useShortPrayerLabels: Boolean = false,
            unifiedColumnHighlight: Boolean = false,
        ) {
            val now = System.currentTimeMillis()
            val cityTimeZone = TimeZone.getTimeZone(snapshot.timezone.ifBlank { TimeZone.getDefault().id })
            val useArabicDigits = usesArabicWidgetDigits(l10n)
            val primaryColor = widgetColor(widgetColors.textPrimary)
            val accentColor = widgetColor(widgetColors.textAccent)
            val countdownHoursLabel = l10n.getString(R.string.countdown_hours)
            val countdownMinutesLabel = l10n.getString(R.string.countdown_minutes)
            val times = snapshot.times.take(colIds.size)
            if (unifiedColumnHighlight) {
                mediumHighlightIds.forEach { highlightId ->
                    views.setViewVisibility(highlightId, View.VISIBLE)
                    views.setInt(highlightId, "setBackgroundResource", 0)
                }
            }
            colIds.forEachIndexed { index, ids ->
                val time = times.getOrNull(index)
                if (time == null) {
                    hideColumn(views, ids, unifiedColumnHighlight, index)
                    return@forEachIndexed
                }
                views.setViewVisibility(ids.nameColId, View.VISIBLE)
                views.setViewVisibility(ids.timeColId, View.VISIBLE)
                val isNext = time.prayer == snapshot.nextPrayer
                if (unifiedColumnHighlight) {
                    val highlightId = mediumHighlightIds[index]
                    if (isNext) {
                        views.setInt(highlightId, "setBackgroundResource", widgetColors.rowHighlightDrawable)
                    } else {
                        views.setInt(highlightId, "setBackgroundResource", 0)
                    }
                    views.setInt(ids.nameColId, "setBackgroundResource", 0)
                    views.setInt(ids.timeColId, "setBackgroundResource", 0)
                } else if (isNext) {
                    views.setInt(ids.nameColId, "setBackgroundResource", widgetColors.rowHighlightDrawable)
                    views.setInt(ids.timeColId, "setBackgroundResource", widgetColors.rowHighlightDrawable)
                } else {
                    views.setInt(ids.nameColId, "setBackgroundResource", 0)
                    views.setInt(ids.timeColId, "setBackgroundResource", 0)
                }
                views.setTextViewText(
                    ids.prayerId,
                    l10n.getString(
                        if (useShortPrayerLabels) {
                            mediumPrayerLabelRes(time.prayer)
                        } else {
                            prayerRes(time.prayer)
                        },
                    ),
                )
                val displayTime = localizeWidgetDigits(time.displayTime, useArabicDigits)

                val countdownMillis =
                    columnCountdownMillis(
                        time = time,
                        now = now,
                        timezone = cityTimeZone,
                    )
                val countdownText =
                    if (compactCountdown) {
                        CountdownFormatter.formatCompact(
                            countdownMillis,
                            countdownHoursLabel,
                            countdownMinutesLabel,
                        )
                    } else {
                        CountdownFormatter.format(
                            countdownMillis,
                            countdownHoursLabel,
                            countdownMinutesLabel,
                        )
                    }
                val localizedCountdown = localizeWidgetDigits(countdownText, useArabicDigits)
                if (timeOnly) {
                    views.setTextViewText(ids.timeId, displayTime)
                    views.setViewVisibility(ids.countdownId, View.GONE)
                } else if (inlineCountdown) {
                    views.setTextViewText(ids.timeId, "$displayTime $localizedCountdown")
                    views.setTextViewText(ids.countdownId, "")
                    views.setViewVisibility(ids.countdownId, View.GONE)
                } else {
                    views.setTextViewText(ids.timeId, displayTime)
                    views.setTextViewText(ids.countdownId, localizedCountdown)
                    views.setViewVisibility(ids.countdownId, View.VISIBLE)
                    views.setTextColor(ids.countdownId, if (isNext) accentColor else primaryColor)
                }

                val rowColor = if (isNext) accentColor else primaryColor
                views.setTextColor(ids.timeId, rowColor)
                views.setTextColor(ids.prayerId, rowColor)
            }
        }

        private fun columnCountdownMillis(
            time: PrayerTime,
            now: Long,
            timezone: TimeZone,
        ): Long =
            PrayerTimeCalculator.millisUntilNextOccurrence(time.timestamp, now, timezone)
                .coerceAtLeast(0L)

        private fun hideColumn(
            views: RemoteViews,
            ids: ColIds,
            unifiedColumnHighlight: Boolean = false,
            columnIndex: Int = -1,
        ) {
            views.setViewVisibility(ids.nameColId, View.GONE)
            views.setViewVisibility(ids.timeColId, View.GONE)
            if (unifiedColumnHighlight && columnIndex in mediumHighlightIds.indices) {
                views.setInt(mediumHighlightIds[columnIndex], "setBackgroundResource", 0)
            }
            views.setTextViewText(ids.prayerId, "")
            views.setTextViewText(ids.timeId, "")
            views.setTextViewText(ids.countdownId, "")
        }

        private fun launchPendingIntent(): PendingIntent {
            val intent =
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            return PendingIntent.getActivity(
                context,
                PendingIntentRequestCodes.WIDGET_LAUNCH,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun prayerRes(prayer: Prayer): Int =
            when (prayer) {
                Prayer.FAJR -> R.string.fajr
                Prayer.SHURUQ -> R.string.shuruq
                Prayer.DHUHR -> R.string.dhuhr
                Prayer.ASR -> R.string.asr
                Prayer.MAGHRIB -> R.string.maghrib
                Prayer.ISHA -> R.string.isha
            }

        private fun mediumPrayerLabelRes(prayer: Prayer): Int =
            when (prayer) {
                Prayer.FAJR -> R.string.widget_m_fajr
                Prayer.SHURUQ -> R.string.widget_m_shuruq
                Prayer.DHUHR -> R.string.widget_m_dhuhr
                Prayer.ASR -> R.string.widget_m_asr
                Prayer.MAGHRIB -> R.string.widget_m_maghrib
                Prayer.ISHA -> R.string.widget_m_isha
            }

        private fun bindWidgetHijri(
            views: RemoteViews,
            snapshot: WidgetSnapshot,
            l10n: Context,
            widgetColors: WidgetPalette,
        ) {
            val hijri = snapshot.hijriDate
            var hijriVisible = false
            if (hijri != null) {
                val text = HijriDateFormatter.format(hijri, l10n.resources)
                views.setTextViewText(R.id.widget_hijri, text)
                views.setTextColor(R.id.widget_hijri, widgetColor(widgetColors.textSecondary))
                views.setViewVisibility(R.id.widget_hijri, android.view.View.VISIBLE)
                hijriVisible = true
            } else {
                views.setViewVisibility(R.id.widget_hijri, android.view.View.GONE)
            }
            val event = snapshot.upcomingEvent
            var eventVisible = false
            if (event != null) {
                val text = HijriDateFormatter.formatBanner(event, l10n.resources)
                views.setTextViewText(R.id.widget_event, text)
                views.setTextColor(R.id.widget_event, widgetColor(widgetColors.textAccent))
                views.setViewVisibility(R.id.widget_event, android.view.View.VISIBLE)
                eventVisible = true
            } else {
                views.setViewVisibility(R.id.widget_event, android.view.View.GONE)
            }
            setViewVisibilityIfPresent(
                views,
                R.id.widget_header_row,
                if (hijriVisible || eventVisible) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                },
            )
        }
    }
