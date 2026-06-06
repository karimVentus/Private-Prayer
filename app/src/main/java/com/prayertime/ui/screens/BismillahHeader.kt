package com.prayertime.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.prayertime.R

/** Reusable Bismillah line shown at the top of every screen. */
@Composable
fun BismillahHeader(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.bismillah_header),
        modifier = modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 11.sp,
    )
}
