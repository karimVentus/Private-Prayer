package com.prayertime.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prayertime.ui.theme.AppSpacing

/** Minimum 48 dp touch target — Phase 5E.3. */
@Composable
fun AppTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier =
            modifier.defaultMinSize(
                minWidth = AppSpacing.touchTargetMin,
                minHeight = AppSpacing.touchTargetMin,
            ),
        content = content,
    )
}
