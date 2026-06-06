package com.prayertime.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prayertime.BuildConfig
import com.prayertime.R
import com.prayertime.notification.AdhanSoundResolver
import com.prayertime.ui.components.AppTextButton
import com.prayertime.ui.theme.AppSpacing
import com.prayertime.ui.theme.AppTheme

data class PrivacyModeUiState(
    val offlineOnly: Boolean,
    val networkModeAvailable: Boolean,
    val onOfflineOnlyChanged: (Boolean) -> Unit,
)

data class AdhanNotificationsUiState(
    val enabled: Boolean,
    val notificationsGranted: Boolean,
    val exactAlarmsGranted: Boolean,
    val batteryOptimizationExempt: Boolean,
    val adhanSound: String,
    val onEnabledChanged: (Boolean) -> Unit,
    val onRequestNotifications: () -> Unit,
    val onRequestExactAlarms: () -> Unit,
    val onRequestBatteryOptimization: () -> Unit,
    val onAdhanSoundChanged: (String) -> Unit,
)

data class ThemeUiState(
    val selected: AppTheme,
    val onThemeChanged: (AppTheme) -> Unit,
)

@Composable
fun AboutScreen(
    theme: ThemeUiState,
    privacy: PrivacyModeUiState,
    adhan: AdhanNotificationsUiState,
    onRefreshTimes: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(AppSpacing.screenHorizontal)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
    ) {
        BismillahHeader()
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppTextButton(onClick = onBack) {
                Text(stringResource(R.string.back))
            }
        }

        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(AppSpacing.sectionGap))

        if (privacy.networkModeAvailable) {
            PrivacyModeCard(privacy.offlineOnly, privacy.onOfflineOnlyChanged)
        } else {
            OfflineOnlyBuildCard()
        }
        Spacer(modifier = Modifier.height(AppSpacing.sectionGap))

        ThemePickerCard(theme.selected, theme.onThemeChanged)
        Spacer(modifier = Modifier.height(AppSpacing.sectionGap))

        AdhanNotificationsCard(adhan = adhan)
        Spacer(modifier = Modifier.height(AppSpacing.sectionGap))

        RefreshTimesCard(onRefreshTimes = onRefreshTimes)
        Spacer(modifier = Modifier.height(AppSpacing.sectionGap))

        PrivacyPolicyCard(privacy.networkModeAvailable)
        Spacer(modifier = Modifier.height(AppSpacing.sectionGap))

        CalculationMethodCard()
        Spacer(modifier = Modifier.height(AppSpacing.sectionGap))

        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ThemePickerCard(
    selected: AppTheme,
    onSelected: (AppTheme) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(AppSpacing.cardPadding)) {
            Text(
                text = stringResource(R.string.theme_picker),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            val options =
                listOf(
                    AppTheme.LIGHT to R.string.theme_light,
                    AppTheme.GREEN to R.string.theme_green,
                    AppTheme.DARK to R.string.theme_dark,
                )
            options.forEach { (theme, labelRes) ->
                val isSelected = theme == selected
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(theme) }
                            .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(labelRes),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color =
                            if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        modifier = Modifier.weight(1f),
                    )
                    if (isSelected) {
                        Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun RefreshTimesCard(onRefreshTimes: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.refresh_times_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.refresh_times_desc),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            AppTextButton(onClick = onRefreshTimes) {
                Text(stringResource(R.string.refresh_times_action))
            }
        }
    }
}

@Composable
private fun AdhanNotificationsCard(adhan: AdhanNotificationsUiState) {
    val enabled = adhan.enabled
    val notificationsGranted = adhan.notificationsGranted
    val exactAlarmsGranted = adhan.exactAlarmsGranted
    val showBatteryNotice = enabled && notificationsGranted && exactAlarmsGranted && !adhan.batteryOptimizationExempt
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.adhan_toggle),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.adhan_toggle_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = adhan.onEnabledChanged,
                )
            }
            if (enabled && !notificationsGranted) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.adhan_notifications_denied),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.clickable(onClick = adhan.onRequestNotifications),
                )
            }
            if (enabled && notificationsGranted && !exactAlarmsGranted) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.adhan_exact_alarm_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.clickable(onClick = adhan.onRequestExactAlarms),
                )
            }
            if (showBatteryNotice) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.adhan_battery_optimization_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable(onClick = adhan.onRequestBatteryOptimization),
                )
            }
            if (enabled) {
                Spacer(modifier = Modifier.height(12.dp))
                AdhanSoundPicker(
                    selected = adhan.adhanSound,
                    onSelected = adhan.onAdhanSoundChanged,
                )
            }
        }
    }
}

@Composable
private fun AdhanSoundPicker(
    selected: String,
    onSelected: (String) -> Unit,
) {
    val context = LocalContext.current
    val options = AdhanSoundResolver.options
    var playingKey by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }

    // Stop playback and release player
    fun stopPlayback() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        playingKey = null
    }

    // Clean up when leaving the screen
    DisposableEffect(Unit) {
        onDispose { stopPlayback() }
    }

    Text(
        text = stringResource(R.string.adhan_sound_picker),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(modifier = Modifier.height(4.dp))

    options.forEach { option ->
        val key = option.storageKey
        val labelRes = option.labelRes
        val isSelected = key == selected
        val isPlaying = key == playingKey

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { onSelected(key) }
                    .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Play / pause button
            TextButton(
                onClick = {
                    if (isPlaying) {
                        stopPlayback()
                    } else {
                        stopPlayback()
                        val resId = option.rawRes
                        val mp = android.media.MediaPlayer.create(context, resId)
                        if (mp != null) {
                            mp.setOnCompletionListener {
                                it.release()
                                if (mediaPlayer === it) {
                                    mediaPlayer = null
                                    playingKey = null
                                }
                            }
                            mp.start()
                            mediaPlayer = mp
                            playingKey = key
                        }
                    }
                },
                modifier = Modifier.defaultMinSize(minWidth = 40.dp, minHeight = 40.dp),
            ) {
                Text(if (isPlaying) "⏹" else "▶", fontSize = 14.sp)
            }

            // Sound name
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                modifier = Modifier.padding(start = 4.dp).weight(1f),
            )

            // Checkmark for selected
            if (isSelected) {
                Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun OfflineOnlyBuildCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.privacy_mode),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.offline_apk_fixed_desc),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.offline_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PrivacyModeCard(
    offlineOnly: Boolean,
    onOfflineOnlyChanged: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.privacy_mode),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.offline_only_toggle),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = offlineOnly,
                    onCheckedChange = onOfflineOnlyChanged,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text =
                    if (offlineOnly) {
                        stringResource(R.string.offline_desc)
                    } else {
                        stringResource(R.string.network_desc)
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PrivacyPolicyCard(networkModeAvailable: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.privacy_section),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.privacy_what_sent),
            )
            Text(
                text = stringResource(R.string.privacy_offline_item),
            )
            if (networkModeAvailable) {
                Text(
                    text = stringResource(R.string.privacy_network_item),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.privacy_never_used),
            )
            Text(
                text = stringResource(R.string.privacy_no_analytics),
            )
            Text(
                text = stringResource(R.string.privacy_no_ads),
            )
            Text(
                text = stringResource(R.string.privacy_no_crash),
            )
            Text(
                text = stringResource(R.string.privacy_no_gps),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.privacy_full_policy),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CalculationMethodCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.calc_method),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.calc_method_desc),
            )
            Text(
                text = stringResource(R.string.calc_method_high_lat),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
