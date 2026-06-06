package com.prayertime.ui

import com.prayertime.domain.model.Prayer

/** Live next-prayer snapshot emitted by [com.prayertime.ui.prayer.PrayerTimesViewModel] once per second. */
data class LivePrayerCountdown(
    val nextPrayer: Prayer,
    val countdownMillis: Long,
)
