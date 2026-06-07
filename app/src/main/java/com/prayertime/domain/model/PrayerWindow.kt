package com.prayertime.domain.model

/** True when [now] is between this prayer's time and the next slot (or ~90 min after last). */
fun isInPrayerWindow(
    prayer: Prayer,
    times: List<PrayerTime>,
    now: Long = System.currentTimeMillis(),
): Boolean {
    val index = times.indexOfFirst { it.prayer == prayer }
    if (index < 0) return false
    val start = times[index].timestamp
    val end =
        times.getOrNull(index + 1)?.timestamp
            ?: (start + NINETY_MINUTES_MS)
    return now in start until end
}

private const val NINETY_MINUTES_MS = 90 * 60 * 1000L
