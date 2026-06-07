package com.prayertime.ui.prayer

import androidx.annotation.StringRes
import com.prayertime.domain.model.HijriDate
import com.prayertime.domain.model.Prayer
import com.prayertime.domain.model.PrayerTimesResult
import com.prayertime.domain.model.UpcomingEvent

sealed class PrayerTimesUiState {
    data object Loading : PrayerTimesUiState()

    data object NoCity : PrayerTimesUiState()

    data class FetchError(
        @StringRes val messageResId: Int,
    ) : PrayerTimesUiState()

    data class Success(
        val city: String,
        val timezone: String,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val result: PrayerTimesResult.Success,
        val todayHijriDate: HijriDate? = null,
        val upcomingEvent: UpcomingEvent? = null,
    ) : PrayerTimesUiState()
}

data class PrayerTimesActions(
    val onChangeCity: () -> Unit,
    val onCalendar: () -> Unit,
    val onQibla: () -> Unit,
    val onAbout: () -> Unit,
    val onLanguage: () -> Unit,
    val onToggleMute: (Prayer) -> Unit,
    val mutedPrayers: Set<String> = emptySet(),
)
