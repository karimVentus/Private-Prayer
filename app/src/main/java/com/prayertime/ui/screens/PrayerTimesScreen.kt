package com.prayertime.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.prayertime.R
import com.prayertime.domain.model.HijriDate
import com.prayertime.domain.model.Prayer
import com.prayertime.domain.model.PrayerTime
import com.prayertime.domain.model.PrayerTimesResult
import com.prayertime.domain.model.UpcomingEvent
import com.prayertime.ui.HijriDateFormatter
import com.prayertime.ui.LivePrayerCountdown
import com.prayertime.ui.components.AppTextButton
import com.prayertime.ui.prayer.PrayerTimesActions
import com.prayertime.ui.theme.AppSpacing
import com.prayertime.widget.CountdownFormatter
import kotlinx.coroutines.flow.StateFlow

@Composable
fun PrayerTimesScreen(
    city: String,
    todayHijriDate: HijriDate?,
    upcomingEvent: UpcomingEvent?,
    result: PrayerTimesResult.Success,
    liveCountdownFlow: StateFlow<LivePrayerCountdown?>,
    offlineOnly: Boolean,
    actions: PrayerTimesActions,
) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = AppSpacing.screenHorizontal),
    ) {
        item(key = "prayer_header") {
            BismillahHeader()
            PrayerTimesHeader(
                city = city,
                todayHijriDate = todayHijriDate,
                upcomingEvent = upcomingEvent,
                offlineOnly = offlineOnly,
                liveCountdownFlow = liveCountdownFlow,
                actions = actions,
            )
            Spacer(modifier = Modifier.height(AppSpacing.sectionGap))
        }
        items(
            items = result.times,
            key = { it.prayer },
        ) { time ->
            val isMuted = actions.mutedPrayers.contains(time.prayer.name)
            PrayerTimeRow(
                time = time,
                isMuted = isMuted,
                onToggleMute = { actions.onToggleMute(time.prayer) },
            )
        }
    }
}

@Composable
private fun PrayerTimesHeader(
    city: String,
    todayHijriDate: HijriDate?,
    upcomingEvent: UpcomingEvent?,
    offlineOnly: Boolean,
    liveCountdownFlow: StateFlow<LivePrayerCountdown?>,
    actions: PrayerTimesActions,
) {
    val context = LocalContext.current
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .zIndex(1f),
    ) {
        Text(
            text = city,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
        if (todayHijriDate != null) {
            Text(
                text = HijriDateFormatter.format(todayHijriDate, context.resources),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (upcomingEvent != null) {
            Text(
                text = HijriDateFormatter.formatBanner(upcomingEvent, context.resources),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Start,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        HeaderActionRow(actions = actions)
        if (offlineOnly) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.privacy_mode_badge),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        CountdownHeader(liveCountdownFlow = liveCountdownFlow)
    }
}

/** Subscribes to the 1s ticker here so the rest of [PrayerTimesScreen] does not recompose. */
@Composable
private fun CountdownHeader(liveCountdownFlow: StateFlow<LivePrayerCountdown?>) {
    val countdown by liveCountdownFlow.collectAsState()
    val current = countdown ?: return
    val hoursUnit = stringResource(R.string.countdown_hours)
    val minutesUnit = stringResource(R.string.countdown_minutes)
    val prayerLabel = stringResource(prayerNameRes(current.nextPrayer))
    val durationText =
        CountdownFormatter.format(
            millis = current.countdownMillis,
            hoursUnit = hoursUnit,
            minutesUnit = minutesUnit,
        )
    Text(
        text = stringResource(R.string.next_label, prayerLabel, durationText),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeaderActionRow(actions: PrayerTimesActions) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
        verticalArrangement = Arrangement.Center,
    ) {
        HeaderActionButton(
            label = stringResource(R.string.change),
            onClick = actions.onChangeCity,
        )
        HeaderActionButton(
            label = stringResource(R.string.calendar),
            onClick = actions.onCalendar,
        )
        HeaderActionButton(
            label = stringResource(R.string.language),
            onClick = actions.onLanguage,
        )
        HeaderActionButton(
            label = stringResource(R.string.settings),
            onClick = actions.onAbout,
        )
    }
}

@Composable
private fun HeaderActionButton(
    label: String,
    onClick: () -> Unit,
) {
    AppTextButton(
        onClick = onClick,
    ) {
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun PrayerTimeRow(
    time: PrayerTime,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = AppSpacing.listItemVertical),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.cardPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(prayerNameRes(time.prayer)),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = time.displayTime,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            PrayerNotificationSlot(
                prayer = time.prayer,
                isMuted = isMuted,
                onToggleMute = onToggleMute,
            )
        }
    }
}

@Composable
private fun PrayerNotificationSlot(
    prayer: Prayer,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
) {
    val prayerLabel = stringResource(prayerNameRes(prayer))
    IconButton(onClick = onToggleMute) {
        Icon(
            imageVector = if (isMuted) Icons.Outlined.NotificationsOff else Icons.Outlined.Notifications,
            contentDescription =
                stringResource(
                    if (isMuted) R.string.unmute_prayer_notification else R.string.mute_prayer_notification,
                    prayerLabel,
                ),
            tint =
                if (isMuted) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                },
        )
    }
}

private fun prayerNameRes(prayer: Prayer): Int {
    return when (prayer) {
        Prayer.FAJR -> R.string.fajr
        Prayer.SHURUQ -> R.string.shuruq
        Prayer.DHUHR -> R.string.dhuhr
        Prayer.ASR -> R.string.asr
        Prayer.MAGHRIB -> R.string.maghrib
        Prayer.ISHA -> R.string.isha
    }
}
