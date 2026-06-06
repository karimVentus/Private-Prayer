package com.prayertime.ui

import android.content.res.Resources
import androidx.annotation.StringRes
import com.prayertime.R
import com.prayertime.domain.model.HijriDate
import com.prayertime.domain.model.IslamicEvent
import com.prayertime.domain.model.IslamicMonth
import com.prayertime.domain.model.UpcomingEvent

object HijriDateFormatter {
    /**
     * Formats [date] using string resources for month names.
     * e.g. "14 Ramadan 1445" (EN) or "١٤ رمضان ١٤٤٥" (AR).
     */
    fun format(
        date: HijriDate,
        resources: Resources,
    ): String {
        val monthRes = monthNameRes(date.month)
        val monthName = resources.getString(monthRes)
        return resources.getString(R.string.hijri_date_format, date.day, monthName, date.year)
    }

    @StringRes
    fun monthNameRes(month: Int): Int =
        when (IslamicMonth.fromMonthNumber(month)) {
            IslamicMonth.MUHARRAM -> R.string.hijri_month_1
            IslamicMonth.SAFAR -> R.string.hijri_month_2
            IslamicMonth.RABI_AL_AWWAL -> R.string.hijri_month_3
            IslamicMonth.RABI_AL_THANI -> R.string.hijri_month_4
            IslamicMonth.JUMADA_AL_AWWAL -> R.string.hijri_month_5
            IslamicMonth.JUMADA_AL_THANI -> R.string.hijri_month_6
            IslamicMonth.RAJAB -> R.string.hijri_month_7
            IslamicMonth.SHABAN -> R.string.hijri_month_8
            IslamicMonth.RAMADAN -> R.string.hijri_month_9
            IslamicMonth.SHAWWAL -> R.string.hijri_month_10
            IslamicMonth.DHUL_QADAH -> R.string.hijri_month_11
            IslamicMonth.DHUL_HIJJAH -> R.string.hijri_month_12
        }

    /** Formats [event] countdown using string resources. e.g. "Eid al-Fitr in 5 days". */
    fun formatBanner(
        event: UpcomingEvent,
        resources: Resources,
    ): String {
        val eventName = resources.getString(eventNameRes(event.event))
        return resources.getString(R.string.event_banner_format, eventName, event.daysUntil)
    }

    @StringRes
    fun eventNameCellRes(event: IslamicEvent): Int =
        when (event) {
            IslamicEvent.ISLAMIC_NEW_YEAR -> R.string.event_islamic_new_year_cell
            IslamicEvent.ASHURA -> R.string.event_ashura_cell
            IslamicEvent.MAWLID_AL_NABI -> R.string.event_mawlid_al_nabi_cell
            IslamicEvent.ISRA_AND_MIRAJ -> R.string.event_isra_and_miraj_cell
            IslamicEvent.MID_SHABAN -> R.string.event_mid_shaban_cell
            IslamicEvent.RAMADAN_START -> R.string.event_ramadan_start_cell
            IslamicEvent.LAYLAT_AL_QADR -> R.string.event_laylat_al_qadr_cell
            IslamicEvent.EID_AL_FITR -> R.string.event_eid_al_fitr_cell
            IslamicEvent.DAY_OF_ARAFAH -> R.string.event_day_of_arafah_cell
            IslamicEvent.EID_AL_ADHA -> R.string.event_eid_al_adha_cell
        }

    @StringRes
    fun eventNameRes(event: IslamicEvent): Int =
        when (event) {
            IslamicEvent.ISLAMIC_NEW_YEAR -> R.string.event_islamic_new_year
            IslamicEvent.ASHURA -> R.string.event_ashura
            IslamicEvent.MAWLID_AL_NABI -> R.string.event_mawlid_al_nabi
            IslamicEvent.ISRA_AND_MIRAJ -> R.string.event_isra_and_miraj
            IslamicEvent.MID_SHABAN -> R.string.event_mid_shaban
            IslamicEvent.RAMADAN_START -> R.string.event_ramadan_start
            IslamicEvent.LAYLAT_AL_QADR -> R.string.event_laylat_al_qadr
            IslamicEvent.EID_AL_FITR -> R.string.event_eid_al_fitr
            IslamicEvent.DAY_OF_ARAFAH -> R.string.event_day_of_arafah
            IslamicEvent.EID_AL_ADHA -> R.string.event_eid_al_adha
        }
}
