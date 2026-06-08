package com.prayertime.domain.model

enum class Prayer {
    FAJR,

    /** Shuruq (sunrise). Display-only slot; excluded from adhan alarms. */
    SHURUQ,
    DHUHR,
    ASR,
    MAGHRIB,
    ISHA,
    ;

    companion object {
        /** Fard prayer slots that [com.prayertime.alarm.PrayerAlarmScheduler] may schedule. */
        val adhanAlarmPrayers: Set<Prayer> = entries.filter { it != SHURUQ }.toSet()
    }
}
