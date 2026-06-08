package com.prayertime.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Status bar and horizontal cutout padding for full-screen routes without a [Scaffold] bottom bar.
 * Tab content inside [Scaffold] bottom navigation must not use this — the bar already handles nav insets.
 */
@Composable
fun Modifier.screenTopSafeInsets(): Modifier =
    windowInsetsPadding(
        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
    )
