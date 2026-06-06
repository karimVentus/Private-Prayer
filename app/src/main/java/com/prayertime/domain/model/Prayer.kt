package com.prayertime.domain.model

enum class Prayer {
    FAJR,

    /** Shuruq (sunrise). Excluded from adhan alarms. */
    SHURUQ,
    DHUHR,
    ASR,
    MAGHRIB,
    ISHA,
}
