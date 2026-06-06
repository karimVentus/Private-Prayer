package com.prayertime.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun PrayerTimeTheme(
    theme: AppTheme = AppTheme.LIGHT,
    content: @Composable () -> Unit,
) {
    ProvideAppTheme(theme = theme) {
        MaterialTheme(
            colorScheme = ThemePalettes.materialScheme(theme),
            content = content,
        )
    }
}
