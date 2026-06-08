package com.prayertime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prayertime.R
import com.prayertime.domain.calculator.HijriCalculator
import com.prayertime.domain.model.HijriDate
import com.prayertime.domain.model.IslamicEvent
import com.prayertime.locale.toEasternArabicDigits
import com.prayertime.ui.HijriDateFormatter
import com.prayertime.ui.components.AppTextButton
import com.prayertime.ui.theme.AppSpacing
import com.prayertime.ui.theme.CalendarPalette
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@Composable
fun HijriCalendarScreen(
    timezone: String,
    onClose: () -> Unit,
    showBackLink: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val palette = calendarPalette()
    val tz = TimeZone.getTimeZone(timezone)
    val cal = remember { Calendar.getInstance(tz) }
    val todayHijri =
        remember(cal) {
            HijriCalculator.gregorianToHijri(cal[Calendar.YEAR], cal[Calendar.MONTH] + 1, cal[Calendar.DAY_OF_MONTH])
        }
    val locale = Locale.getDefault()
    val isArabic = locale.language == "ar"

    var selectedTab by remember { mutableIntStateOf(0) }
    val resources = LocalContext.current.resources
    val tabTitles =
        listOf(
            resources.getString(R.string.calendar_tab_monthly),
            resources.getString(R.string.calendar_tab_annual),
        )

    Column(modifier = modifier.fillMaxSize().background(palette.background)) {
        BismillahHeader()
        CalendarTabRow(palette, selectedTab, tabTitles) { selectedTab = it }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> MonthlyCalendarTab(palette, todayHijri, isArabic, tz, locale, resources)
                1 ->
                    AnnualEventsView(
                        hijriYear = todayHijri.year,
                        resources = resources,
                    )
            }
        }

        if (showBackLink) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = resources.getString(R.string.back_to_prayer_times),
                    color = palette.textSecondary,
                    fontSize = 11.sp,
                    modifier =
                        Modifier
                            .defaultMinSize(minHeight = AppSpacing.touchTargetMin)
                            .clickable(onClick = onClose)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun CalendarTabRow(
    palette: CalendarPalette,
    selectedTab: Int,
    titles: List<String>,
    onTabSelected: (Int) -> Unit,
) {
    TabRow(
        selectedTabIndex = selectedTab,
        containerColor = palette.headerBg,
        contentColor = palette.fridayGold,
        divider = {},
    ) {
        titles.forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        title,
                        color = if (selectedTab == index) palette.fridayGold else palette.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

@Composable
private fun MonthlyCalendarTab(
    palette: CalendarPalette,
    todayHijri: HijriDate,
    isArabic: Boolean,
    tz: TimeZone,
    locale: Locale,
    resources: android.content.res.Resources,
) {
    var viewYear by remember { mutableIntStateOf(todayHijri.year) }
    var viewMonth by remember { mutableIntStateOf(todayHijri.month) }
    var selectedCell by remember { mutableStateOf<CalendarCell?>(null) }

    val daysInMonth = HijriCalculator.daysInMonth(viewYear, viewMonth)
    val monthName = resources.getString(HijriDateFormatter.monthNameRes(viewMonth))
    val gregCal = remember(tz) { Calendar.getInstance(tz) }
    val gRange = computeGregorianRange(viewYear, viewMonth, daysInMonth, isArabic, tz, locale, gregCal)

    val firstG = HijriCalculator.hijriToGregorian(HijriDate(viewYear, viewMonth, 1))
    gregCal.time = firstG
    val startOffset = gregCal[Calendar.DAY_OF_WEEK] % 7

    val gregFmt = SimpleDateFormat(if (isArabic) "d MMM" else "MMM d", locale).apply { timeZone = tz }
    val cells =
        buildCalendarCells(
            viewYear,
            viewMonth,
            daysInMonth,
            todayHijri,
            startOffset,
            gregFmt,
            gregCal,
            resources,
        )

    Column(modifier = Modifier.fillMaxSize()) {
        CalendarHeader(
            palette,
            monthName,
            gRange,
            resources,
            onPrev = {
                if (viewMonth == 1) {
                    viewMonth = 12
                    viewYear--
                } else {
                    viewMonth--
                }
            },
            onNext = {
                if (viewMonth == 12) {
                    viewMonth = 1
                    viewYear++
                } else {
                    viewMonth++
                }
            },
        )

        val todayLabel = HijriDateFormatter.format(todayHijri, resources)
        val todayG =
            SimpleDateFormat(if (isArabic) "d MMMM" else "MMMM d", locale)
                .apply { timeZone = tz }.format(Calendar.getInstance(tz).time)
        Text(
            "$todayLabel  —  $todayG",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            color = palette.textSecondary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
        )

        DayHeaders(palette, resources)
        BoxWithConstraints(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
        ) {
            val rowCount = paddedCalendarRowCount(cells)
            val fitHeight = maxHeight / rowCount
            val rowHeight = fitHeight.coerceIn(44.dp, 72.dp)
            val totalGridHeight = rowHeight * rowCount
            val gridModifier =
                if (totalGridHeight <= maxHeight) {
                    Modifier
                        .fillMaxWidth()
                        .height(totalGridHeight)
                } else {
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                CalendarMonthGrid(
                    palette = palette,
                    cells = cells,
                    isArabic = isArabic,
                    resources = resources,
                    rowHeight = if (totalGridHeight <= maxHeight) rowHeight else 44.dp,
                    modifier = gridModifier,
                    onEventClick = { selectedCell = it },
                )
            }
        }
    }

    selectedCell?.let { cell ->
        EventDetailDialog(palette, cell, resources, todayHijri) { selectedCell = null }
    }
}

private fun buildCalendarCells(
    viewYear: Int,
    viewMonth: Int,
    daysInMonth: Int,
    todayHijri: HijriDate,
    startOffset: Int,
    gregFmt: SimpleDateFormat,
    gregCal: Calendar,
    resources: android.content.res.Resources,
): List<CalendarCell?> {
    val isRamadan = viewMonth == 9
    val cells = mutableListOf<CalendarCell?>()
    repeat(startOffset) { cells.add(null) }
    for (d in 1..daysInMonth) {
        val hd = HijriDate(viewYear, viewMonth, d)
        val gd = HijriCalculator.hijriToGregorian(hd)
        val events = IslamicEvent.entries.filter { it.month == viewMonth && it.day == d }.toMutableList()
        val extraLabels = mutableListOf<String>()
        if (isRamadan && d == 1) extraLabels.add(resources.getString(R.string.calendar_ramadan_first))
        if (isRamadan && d == daysInMonth) extraLabels.add(resources.getString(R.string.calendar_ramadan_last))
        val isToday = hd.year == todayHijri.year && hd.month == todayHijri.month && hd.day == todayHijri.day
        gregCal.time = gd
        val isFriday = gregCal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
        cells.add(CalendarCell(hd, gregFmt.format(gd), events, extraLabels, isToday, isFriday))
    }
    return cells
}

private fun computeGregorianRange(
    viewYear: Int,
    viewMonth: Int,
    daysInMonth: Int,
    isArabic: Boolean,
    tz: TimeZone,
    locale: Locale,
    gregCal: Calendar,
): String {
    val firstG = HijriCalculator.hijriToGregorian(HijriDate(viewYear, viewMonth, 1))
    val lastG = HijriCalculator.hijriToGregorian(HijriDate(viewYear, viewMonth, daysInMonth))
    val gFmt = SimpleDateFormat(if (isArabic) "MMM" else "MMMM", locale).apply { timeZone = tz }
    return if (isArabic) {
        gregCal.time = firstG
        val y1 = gregCal.get(Calendar.YEAR)
        gregCal.time = lastG
        val y2 = gregCal.get(Calendar.YEAR)
        if (y1 == y2) {
            "${gFmt.format(firstG)} / ${gFmt.format(lastG)} $y1"
        } else {
            "${gFmt.format(firstG)} $y1 / ${gFmt.format(lastG)} $y2"
        }
    } else {
        "${gFmt.format(firstG)} – ${gFmt.format(lastG)}"
    }
}

@Composable
private fun CalendarHeader(
    palette: CalendarPalette,
    monthName: String,
    gRange: String,
    resources: android.content.res.Resources,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(palette.headerBg).padding(horizontal = AppSpacing.screenHorizontal, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppTextButton(
            onClick = onPrev,
            modifier =
                Modifier.semantics {
                    contentDescription = resources.getString(R.string.calendar_nav_prev_desc)
                },
        ) {
            Text(stringResource(R.string.calendar_nav_prev), color = palette.textSecondary, fontSize = 20.sp)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(monthName, color = palette.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(gRange, color = palette.textSecondary, fontSize = 11.sp)
        }
        AppTextButton(
            onClick = onNext,
            modifier =
                Modifier.semantics {
                    contentDescription = resources.getString(R.string.calendar_nav_next_desc)
                },
        ) {
            Text(stringResource(R.string.calendar_nav_next), color = palette.textSecondary, fontSize = 20.sp)
        }
    }
}

@Composable
private fun DayHeaders(
    palette: CalendarPalette,
    resources: android.content.res.Resources,
) {
    val dayNameResIds =
        listOf(
            R.string.calendar_day_sat,
            R.string.calendar_day_sun,
            R.string.calendar_day_mon,
            R.string.calendar_day_tue,
            R.string.calendar_day_wed,
            R.string.calendar_day_thu,
            R.string.calendar_day_fri,
        )
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(modifier = Modifier.fillMaxWidth().background(palette.headerAccent).padding(vertical = 6.dp)) {
            for ((i, resId) in dayNameResIds.withIndex()) {
                Text(
                    resources.getString(resId),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = if (i == 6) palette.fridayGold else palette.headerDayText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

private fun paddedCalendarRowCount(cells: List<CalendarCell?>): Int {
    var size = cells.size
    while (size % 7 != 0) {
        size++
    }
    return size / 7
}

@Composable
private fun CalendarMonthGrid(
    palette: CalendarPalette,
    cells: List<CalendarCell?>,
    isArabic: Boolean,
    resources: android.content.res.Resources,
    rowHeight: Dp,
    modifier: Modifier = Modifier,
    onEventClick: (CalendarCell) -> Unit,
) {
    val paddedCells =
        buildList {
            addAll(cells)
            while (size % 7 != 0) {
                add(null)
            }
        }
    val rows = paddedCells.chunked(7)
    Column(modifier = modifier.fillMaxWidth()) {
        for (row in rows) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(rowHeight),
            ) {
                for (cell in row) {
                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        if (cell != null) CalendarCellView(palette, cell, isArabic, resources, onEventClick)
                    }
                }
            }
        }
    }
}

private data class CalendarCell(
    val hijriDate: HijriDate,
    val gregorianLabel: String,
    val events: List<IslamicEvent>,
    val extraLabels: List<String>,
    val isToday: Boolean,
    val isFriday: Boolean,
)

@Composable
private fun CalendarCellView(
    palette: CalendarPalette,
    cell: CalendarCell,
    isArabic: Boolean,
    resources: android.content.res.Resources,
    onEventClick: (CalendarCell) -> Unit,
) {
    val dayNum = if (isArabic) cell.hijriDate.day.toEasternArabicDigits() else cell.hijriDate.day.toString()
    val labelTexts = cell.extraLabels.toMutableList()
    if (cell.events.isNotEmpty()) {
        labelTexts.add(resources.getString(HijriDateFormatter.eventNameCellRes(cell.events.first())))
    }
    val hasEvent = labelTexts.isNotEmpty()
    val displayLabel = labelTexts.firstOrNull()
    val clickModifier = if (hasEvent) Modifier.clickable { onEventClick(cell) } else Modifier

    Box(
        modifier =
            Modifier.fillMaxSize().padding(1.dp).clip(RoundedCornerShape(4.dp)).then(clickModifier)
                .then(
                    when {
                        cell.isToday -> Modifier.background(palette.headerAccent.copy(alpha = 0.5f))
                        cell.isFriday -> Modifier.background(palette.eventPill.copy(alpha = 0.15f))
                        else -> Modifier
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                dayNum,
                fontSize = if (isArabic) 16.sp else 15.sp,
                fontWeight = FontWeight.Medium,
                color =
                    when {
                        cell.isFriday -> palette.fridayGold
                        else -> palette.textPrimary
                    },
            )
            Text(
                cell.gregorianLabel,
                fontSize = 9.sp,
                color = if (cell.isFriday) palette.fridayGoldDim else palette.textSecondary,
                maxLines = 1,
            )
            if (hasEvent && displayLabel != null) {
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier =
                        Modifier.background(palette.eventPill, RoundedCornerShape(4.dp))
                            .padding(horizontal = 2.dp, vertical = 1.dp)
                            .fillMaxWidth(0.95f),
                ) {
                    Text(
                        displayLabel,
                        fontSize = 7.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun EventDetailDialog(
    palette: CalendarPalette,
    cell: CalendarCell,
    resources: android.content.res.Resources,
    todayHijri: HijriDate,
    onDismiss: () -> Unit,
) {
    val labelTexts = cell.extraLabels.toMutableList()
    cell.events.forEach { event -> labelTexts.add(resources.getString(HijriDateFormatter.eventNameRes(event))) }
    val hijriLabel = HijriDateFormatter.format(cell.hijriDate, resources)
    val daysDiff = daysBetween(todayHijri, cell.hijriDate)
    val daysLabel =
        when {
            daysDiff == 0 -> resources.getString(R.string.calendar_today)
            daysDiff > 0 -> resources.getString(R.string.calendar_days_from_now, daysDiff)
            else -> resources.getString(R.string.calendar_days_ago, -daysDiff)
        }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(hijriLabel, color = palette.textPrimary) },
        text = {
            Column {
                Text(cell.gregorianLabel, color = palette.textSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Text(daysLabel, color = palette.fridayGold, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                labelTexts.forEach { label -> Text("• $label", color = palette.textPrimary, fontSize = 14.sp) }
            }
        },
        confirmButton = {
            Text(
                text = resources.getString(R.string.calendar_ok),
                color = palette.fridayGold,
                modifier =
                    Modifier
                        .defaultMinSize(minHeight = AppSpacing.touchTargetMin)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        },
        containerColor = palette.background,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textPrimary,
    )
}

private fun daysBetween(
    from: HijriDate,
    to: HijriDate,
): Int {
    val diffMillis = HijriCalculator.hijriToGregorian(to).time - HijriCalculator.hijriToGregorian(from).time
    return (diffMillis / 86_400_000L).toInt()
}
