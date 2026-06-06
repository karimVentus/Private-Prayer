package com.prayertime.domain.model

data class HijriDate(
    val year: Int,
    // 1=Muharram .. 12=Dhul Hijjah
    val month: Int,
    val day: Int,
)

/** Islamic month names — [monthNumber] matches [HijriDate.month]. */
enum class IslamicMonth(val monthNumber: Int) {
    MUHARRAM(1),
    SAFAR(2),
    RABI_AL_AWWAL(3),
    RABI_AL_THANI(4),
    JUMADA_AL_AWWAL(5),
    JUMADA_AL_THANI(6),
    RAJAB(7),
    SHABAN(8),
    RAMADAN(9),
    SHAWWAL(10),
    DHUL_QADAH(11),
    DHUL_HIJJAH(12),
    ;

    companion object {
        fun fromMonthNumber(month: Int): IslamicMonth = entries.first { it.monthNumber == month }
    }
}

/** Islamic events tracked for countdown display. */
enum class IslamicEvent(val month: Int, val day: Int) {
    ISLAMIC_NEW_YEAR(1, 1),
    ASHURA(1, 10),
    MAWLID_AL_NABI(3, 12),
    ISRA_AND_MIRAJ(7, 27),
    MID_SHABAN(8, 15),
    RAMADAN_START(9, 1),
    LAYLAT_AL_QADR(9, 27),
    EID_AL_FITR(10, 1),
    DAY_OF_ARAFAH(12, 9),
    EID_AL_ADHA(12, 10),
}

/** Result of [HijriCalculator.nextUpcomingEvent]. */
data class UpcomingEvent(
    val event: IslamicEvent,
    val hijriDate: HijriDate,
    val daysUntil: Int,
)
