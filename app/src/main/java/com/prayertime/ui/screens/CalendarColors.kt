package com.prayertime.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.prayertime.domain.model.IslamicEvent
import com.prayertime.ui.theme.CalendarPalette
import com.prayertime.ui.theme.LocalCalendarPalette

/** Calendar / annual-events palette for the active [com.prayertime.ui.theme.AppTheme]. */
@Composable
fun calendarPalette(): CalendarPalette = LocalCalendarPalette.current

/** Shared accent colors for [IslamicEvent] chips (calendar + annual events list). */
fun islamicEventAccent(event: IslamicEvent): Color =
    when (event) {
        IslamicEvent.ISLAMIC_NEW_YEAR -> Color(0xFF1D9E75)
        IslamicEvent.ASHURA -> Color(0xFF5DCAA5)
        IslamicEvent.MAWLID_AL_NABI -> Color(0xFFFAC775)
        IslamicEvent.ISRA_AND_MIRAJ -> Color(0xFF85B7EB)
        IslamicEvent.MID_SHABAN -> Color(0xFF5DCAA5)
        IslamicEvent.RAMADAN_START -> Color(0xFFAFA9EC)
        IslamicEvent.LAYLAT_AL_QADR -> Color(0xFF7F77DD)
        IslamicEvent.EID_AL_FITR -> Color(0xFFEEEDFE)
        IslamicEvent.DAY_OF_ARAFAH -> Color(0xFFEF9F27)
        IslamicEvent.EID_AL_ADHA -> Color(0xFFFAC775)
    }
