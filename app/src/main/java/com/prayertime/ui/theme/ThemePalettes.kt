package com.prayertime.ui.theme

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.prayertime.R

data class CalendarPalette(
    val background: Color,
    val headerBg: Color,
    val headerAccent: Color,
    /** Weekday names on [headerAccent] bar (non-Friday). */
    val headerDayText: Color,
    val textSecondary: Color,
    val fridayGold: Color,
    val fridayGoldDim: Color,
    val textPrimary: Color,
    val eventPill: Color,
)

data class WidgetPalette(
    @ColorRes val background: Int,
    @ColorRes val textPrimary: Int,
    @ColorRes val textSecondary: Int,
    @ColorRes val textAccent: Int,
    @DrawableRes val backgroundDrawable: Int,
    @DrawableRes val rowHighlightDrawable: Int,
)

object ThemePalettes {
    fun materialScheme(theme: AppTheme): ColorScheme =
        when (theme) {
            AppTheme.LIGHT -> lightScheme
            AppTheme.GREEN -> greenScheme
            AppTheme.DARK -> darkScheme
        }

    fun calendar(theme: AppTheme): CalendarPalette =
        when (theme) {
            AppTheme.LIGHT -> lightCalendar
            AppTheme.GREEN -> greenCalendar
            AppTheme.DARK -> darkCalendar
        }

    fun widget(theme: AppTheme): WidgetPalette =
        when (theme) {
            AppTheme.LIGHT -> lightWidget
            AppTheme.GREEN -> greenWidget
            AppTheme.DARK -> darkWidget
        }

    private val lightScheme =
        lightColorScheme(
            primary = Color(0xFF0F6E56),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFB8EAD6),
            onPrimaryContainer = Color(0xFF04342C),
            secondary = Color(0xFF1D9E75),
            onSecondary = Color(0xFFFFFFFF),
            background = Color(0xFFFAFDFB),
            surface = Color(0xFFFAFDFB),
            onSurface = Color(0xFF04342C),
            surfaceVariant = Color(0xFFE8F5F0),
            onSurfaceVariant = Color(0xFF0F6E56),
            outline = Color(0xFF6B9E8F),
            error = Color(0xFFBA1A1A),
            onError = Color(0xFFFFFFFF),
        )

    private val greenScheme =
        darkColorScheme(
            primary = Color(0xFF9FE1CB),
            onPrimary = Color(0xFF04342C),
            primaryContainer = Color(0xFF0F6E56),
            onPrimaryContainer = Color(0xFFE1F5EE),
            secondary = Color(0xFFFAC775),
            onSecondary = Color(0xFF04342C),
            background = Color(0xFF04342C),
            surface = Color(0xFF04342C),
            onSurface = Color(0xFFE1F5EE),
            surfaceVariant = Color(0xFF085041),
            onSurfaceVariant = Color(0xFF9FE1CB),
            outline = Color(0xFF1D9E75),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
        )

    private val darkScheme =
        darkColorScheme(
            primary = Color(0xFF81C784),
            onPrimary = Color(0xFF1E1E2E),
            primaryContainer = Color(0xFF2E3B55),
            onPrimaryContainer = Color(0xFFFFFFFF),
            secondary = Color(0xFF81C784),
            onSecondary = Color(0xFF1E1E2E),
            background = Color(0xFF1E1E2E),
            surface = Color(0xFF1E1E2E),
            onSurface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFF2E3B55),
            onSurfaceVariant = Color(0xFFB0BEC5),
            outline = Color(0xFF546E7A),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
        )

    private val lightCalendar =
        CalendarPalette(
            background = Color(0xFFFAFDFB),
            headerBg = Color(0xFFE8F5F0),
            headerAccent = Color(0xFF0F6E56),
            headerDayText = Color(0xFFE1F5EE),
            textSecondary = Color(0xFF0F6E56),
            fridayGold = Color(0xFFEF9F27),
            fridayGoldDim = Color(0xFFC8860A),
            textPrimary = Color(0xFF04342C),
            eventPill = Color(0xFF1D9E75),
        )

    private val greenCalendar =
        CalendarPalette(
            background = Color(0xFF04342C),
            headerBg = Color(0xFF085041),
            headerAccent = Color(0xFF0F6E56),
            headerDayText = Color(0xFF9FE1CB),
            textSecondary = Color(0xFF9FE1CB),
            fridayGold = Color(0xFFFAC775),
            fridayGoldDim = Color(0xFFEF9F27),
            textPrimary = Color(0xFFE1F5EE),
            eventPill = Color(0xFF1D9E75),
        )

    private val darkCalendar =
        CalendarPalette(
            background = Color(0xFF1E1E2E),
            headerBg = Color(0xFF252538),
            headerAccent = Color(0xFF2E3B55),
            headerDayText = Color(0xFFB0BEC5),
            textSecondary = Color(0xFFB0BEC5),
            fridayGold = Color(0xFF81C784),
            fridayGoldDim = Color(0xFF66BB6A),
            textPrimary = Color(0xFFFFFFFF),
            eventPill = Color(0xFF81C784),
        )

    private val lightWidget =
        WidgetPalette(
            background = R.color.widget_light_background,
            textPrimary = R.color.widget_light_text_primary,
            textSecondary = R.color.widget_light_text_secondary,
            textAccent = R.color.widget_light_text_accent,
            backgroundDrawable = R.drawable.widget_background_light,
            rowHighlightDrawable = R.drawable.widget_col_highlight_light,
        )

    private val greenWidget =
        WidgetPalette(
            background = R.color.widget_green_background,
            textPrimary = R.color.widget_green_text_primary,
            textSecondary = R.color.widget_green_text_secondary,
            textAccent = R.color.widget_green_text_accent,
            backgroundDrawable = R.drawable.widget_background_green,
            rowHighlightDrawable = R.drawable.widget_col_highlight_green,
        )

    private val darkWidget =
        WidgetPalette(
            background = R.color.widget_dark_background,
            textPrimary = R.color.widget_dark_text_primary,
            textSecondary = R.color.widget_dark_text_secondary,
            textAccent = R.color.widget_dark_text_accent,
            backgroundDrawable = R.drawable.widget_background_dark,
            rowHighlightDrawable = R.drawable.widget_col_highlight_dark,
        )
}
