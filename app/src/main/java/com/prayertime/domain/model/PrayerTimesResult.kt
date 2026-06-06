package com.prayertime.domain.model

sealed class PrayerTimesResult {
    data class Success(
        val times: List<PrayerTime>,
        val nextPrayer: Prayer,
        val countdown: Long,
    ) : PrayerTimesResult()

    data class Error(val type: FetchError) : PrayerTimesResult()
}
