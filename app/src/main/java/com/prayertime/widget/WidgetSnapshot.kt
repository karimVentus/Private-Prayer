package com.prayertime.widget

import com.prayertime.domain.model.HijriDate
import com.prayertime.domain.model.Prayer
import com.prayertime.domain.model.PrayerTime
import com.prayertime.domain.model.UpcomingEvent
import com.prayertime.ui.theme.AppTheme

data class WidgetSnapshot(
    val state: State,
    val appTheme: AppTheme = AppTheme.LIGHT,
    val cityLabel: String = "",
    val timezone: String = "",
    val times: List<PrayerTime> = emptyList(),
    val nextPrayer: Prayer? = null,
    val countdownMillis: Long = 0L,
    val hijriDate: HijriDate? = null,
    val upcomingEvent: UpcomingEvent? = null,
) {
    enum class State {
        NO_CITY,
        READY,

        /** Cached times shown after fetch failed; banner indicates data may be outdated. */
        STALE,
        ERROR,
    }
}
