package com.prayertime.domain.model

data class PrayerTime(
    val prayer: Prayer,
    val displayTime: String,
    val timestamp: Long,
)
