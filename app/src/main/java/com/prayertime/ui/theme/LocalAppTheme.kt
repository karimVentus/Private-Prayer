package com.prayertime.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppTheme = staticCompositionLocalOf { AppTheme.LIGHT }

val LocalCalendarPalette = staticCompositionLocalOf { ThemePalettes.calendar(AppTheme.LIGHT) }

@Composable
fun ProvideAppTheme(
    theme: AppTheme,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAppTheme provides theme,
        LocalCalendarPalette provides ThemePalettes.calendar(theme),
        content = content,
    )
}
