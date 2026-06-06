package com.prayertime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prayertime.R
import com.prayertime.domain.calculator.HijriCalculator
import com.prayertime.domain.model.HijriDate
import com.prayertime.domain.model.IslamicEvent
import com.prayertime.locale.toEasternArabicDigits
import com.prayertime.ui.HijriDateFormatter
import com.prayertime.ui.theme.CalendarPalette
import java.text.SimpleDateFormat
import java.util.Locale

private fun eventRowBg(
    palette: CalendarPalette,
    index: Int,
): Color = palette.headerBg.copy(alpha = if (index % 2 == 0) 0.75f else 0.55f)

private fun eventSubLabel(
    event: IslamicEvent,
    resources: android.content.res.Resources,
): String? =
    when (event) {
        IslamicEvent.LAYLAT_AL_QADR -> resources.getString(R.string.annual_sub_laylat)
        IslamicEvent.EID_AL_FITR -> resources.getString(R.string.annual_sub_eid_fitr)
        IslamicEvent.EID_AL_ADHA -> resources.getString(R.string.annual_sub_eid_adha)
        else -> null
    }

private val starEvents = setOf(IslamicEvent.EID_AL_FITR, IslamicEvent.EID_AL_ADHA)
private val goldTextEvents = starEvents + setOf(IslamicEvent.MAWLID_AL_NABI, IslamicEvent.DAY_OF_ARAFAH)

private data class EventRow(val event: IslamicEvent, val hijriLabel: String, val gregLabel: String)

@Composable
fun AnnualEventsView(
    hijriYear: Int,
    resources: android.content.res.Resources,
    modifier: Modifier = Modifier,
) {
    val palette = calendarPalette()
    val locale = Locale.getDefault()
    val isArabic = locale.language == "ar"
    val firstDay = HijriDate(hijriYear, 1, 1)
    val lastDay = HijriDate(hijriYear, 12, if (HijriCalculator.isLeapYear(hijriYear)) 30 else 29)
    val gregRange = "${SimpleDateFormat(
        "d MMMM yyyy",
        locale,
    ).format(HijriCalculator.hijriToGregorian(firstDay))}  —  ${
        SimpleDateFormat("d MMMM yyyy", locale).format(HijriCalculator.hijriToGregorian(lastDay))}"

    val rows =
        remember(hijriYear, isArabic) {
            val fmt = SimpleDateFormat(if (isArabic) "d MMMM" else "MMM d", locale)
            IslamicEvent.entries.map { event ->
                val hd = HijriDate(hijriYear, event.month, event.day)
                val hijriMonthName = resources.getString(HijriDateFormatter.monthNameRes(event.month))
                val hijriDay = if (isArabic) event.day.toEasternArabicDigits() else event.day.toString()
                EventRow(event, "$hijriDay $hijriMonthName $hijriYear", fmt.format(HijriCalculator.hijriToGregorian(hd)))
            }
        }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        LazyColumn(
            modifier = modifier.fillMaxSize().background(palette.background),
        ) {
            item(key = "annual_header") {
                AnnualEventsHeader(palette, hijriYear, gregRange, resources)
            }
            item(key = "annual_columns") {
                AnnualEventsColumnHeaders(palette, resources)
            }
            itemsIndexed(rows, key = { _, row -> row.event.name }) { index, row ->
                EventRowItem(palette, row, index, resources)
            }
            item(key = "annual_footer") {
                AnnualEventsFooter(palette, resources)
            }
        }
    }
}

@Composable
private fun AnnualEventsHeader(
    palette: CalendarPalette,
    hijriYear: Int,
    gregRange: String,
    resources: android.content.res.Resources,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(palette.headerBg)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            resources.getString(R.string.annual_events_title),
            color = palette.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "$hijriYear ${resources.getString(R.string.annual_ah_suffix)} — $gregRange",
            color = palette.textSecondary,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun AnnualEventsColumnHeaders(
    palette: CalendarPalette,
    resources: android.content.res.Resources,
) {
    Row(modifier = Modifier.fillMaxWidth().background(palette.headerAccent).padding(horizontal = 12.dp, vertical = 5.dp)) {
        Text(
            resources.getString(R.string.annual_col_hijri),
            modifier = Modifier.weight(1.3f),
            color = palette.textSecondary,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            resources.getString(R.string.annual_col_event),
            modifier = Modifier.weight(1.5f),
            color = palette.textSecondary,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            resources.getString(R.string.annual_col_greg),
            modifier = Modifier.weight(1.2f),
            color = palette.textSecondary,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EventRowItem(
    palette: CalendarPalette,
    row: EventRow,
    index: Int,
    resources: android.content.res.Resources,
) {
    val accent = islamicEventAccent(row.event)
    val star = row.event in starEvents
    val isGold = row.event in goldTextEvents
    val sub = eventSubLabel(row.event, resources)

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 10.dp, vertical = 1.5.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(eventRowBg(palette, index))
                .padding(end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .width(3.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(accent),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            row.hijriLabel,
            modifier = Modifier.weight(1.3f),
            color = palette.textPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Column(modifier = Modifier.weight(1.5f), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (star) Text("☾ ", color = palette.fridayGold, fontSize = 12.sp)
                Text(
                    resources.getString(HijriDateFormatter.eventNameRes(row.event)),
                    color = if (isGold) palette.fridayGold else palette.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (sub != null) Text(sub, fontSize = 7.sp, color = palette.textSecondary)
        }
        Text(row.gregLabel, modifier = Modifier.weight(1.2f), color = palette.textSecondary, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun AnnualEventsFooter(
    palette: CalendarPalette,
    resources: android.content.res.Resources,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        LegendItem(palette, Color(0xFF1D9E75), resources.getString(R.string.annual_legend_events))
        Spacer(Modifier.width(10.dp))
        LegendItem(palette, Color(0xFF7F77DD), resources.getString(R.string.annual_legend_ramadan))
        Spacer(Modifier.width(10.dp))
        LegendItem(palette, Color(0xFFFAC775), resources.getString(R.string.annual_legend_hajj))
        Spacer(Modifier.width(10.dp))
        LegendItem(palette, Color(0xFFEEEDFE), resources.getString(R.string.annual_legend_eids))
    }
    Text(
        resources.getString(R.string.annual_disclaimer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        color = palette.headerAccent,
        fontSize = 8.sp,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun LegendItem(
    palette: CalendarPalette,
    color: Color,
    label: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(7.dp).height(7.dp).clip(RoundedCornerShape(1.5.dp)).background(color))
        Spacer(Modifier.width(3.dp))
        Text(label, color = palette.textSecondary, fontSize = 8.sp)
    }
}
