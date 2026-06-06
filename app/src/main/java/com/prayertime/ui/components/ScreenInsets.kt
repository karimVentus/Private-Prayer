package com.prayertime.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Status bar, nav bar, and display-cutout padding — Phase 5E.4. */
@Composable
fun Modifier.screenSafeInsets(): Modifier = windowInsetsPadding(WindowInsets.safeDrawing)
