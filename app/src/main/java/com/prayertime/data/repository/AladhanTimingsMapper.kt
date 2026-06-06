package com.prayertime.data.repository

import com.prayertime.domain.model.Prayer
import com.prayertime.domain.model.PrayerTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Parses Aladhan API JSON into [PrayerTime] list.
 * All [SimpleDateFormat] instances are created per-call (no shared state) → thread-safe.
 */
internal object AladhanTimingsMapper {
    fun buildPrayerTimes(
        timingsMap: Map<String, String>,
        apiDateStr: String?,
        timezone: String,
    ): List<PrayerTime> {
        val tz = TimeZone.getTimeZone(timezone)
        val dateLabel = prepareDateLabel(apiDateStr)
        val baseDate = parseDate(dateLabel, tz) ?: return emptyList()

        val fajr = mapPrayer(timingsMap, "Fajr", Prayer.FAJR, baseDate, tz)
        // Aladhan "Sunrise" → Shuruq slot
        val shuruq = mapPrayer(timingsMap, "Sunrise", Prayer.SHURUQ, baseDate, tz)
        val dhuhr = mapPrayer(timingsMap, "Dhuhr", Prayer.DHUHR, baseDate, tz)
        val asr = mapPrayer(timingsMap, "Asr", Prayer.ASR, baseDate, tz)
        val maghrib = mapPrayer(timingsMap, "Maghrib", Prayer.MAGHRIB, baseDate, tz)
        val isha = mapPrayer(timingsMap, "Isha", Prayer.ISHA, baseDate, tz)

        return listOfNotNull(fajr, shuruq, dhuhr, asr, maghrib, isha)
    }

    private fun mapPrayer(
        timingsMap: Map<String, String>,
        key: String,
        prayer: Prayer,
        baseDate: Date,
        tz: TimeZone,
    ): PrayerTime? {
        val raw = timingsMap[key] ?: return null
        val display = normalizeTime(raw)
        val timestamp = toTimestamp(baseDate, display, tz)
        return timestamp?.let { PrayerTime(prayer, display, it) }
    }

    private fun normalizeTime(raw: String): String {
        val trimmed = raw.trim()
        val space = trimmed.indexOf(' ')
        return if (space > 0) trimmed.substring(0, space) else trimmed
    }

    private fun toTimestamp(
        date: Date,
        timeStr: String,
        tz: TimeZone,
    ): Long? {
        val (hour, minute) = parseHourMinute(timeStr) ?: return null
        return runCatching {
            newDateTimeFmt(tz).parse(
                "${dateIso(date, tz)} ${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}",
            )?.time
        }.getOrNull()
    }

    private fun parseHourMinute(timeStr: String): Pair<Int, Int>? {
        val parts = timeStr.split(":")
        if (parts.size < 2) return null
        val hour = parts[0].toIntOrNull()
        val minute = parts[1].take(2).toIntOrNull()
        return when {
            hour == null || minute == null -> null
            hour !in 0..23 || minute !in 0..59 -> null
            else -> hour to minute
        }
    }

    // -- Thread-safe helpers: each call creates a new SimpleDateFormat --

    /** Fresh "yyyy-MM-dd" formatter for [tz]. */
    private fun newDateFmt(tz: TimeZone) = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = tz }

    /** Fresh "yyyy-MM-dd HH:mm" formatter for [tz]. */
    private fun newDateTimeFmt(tz: TimeZone) = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { timeZone = tz }

    /** Formats [date] as ISO-8601 date string in [tz]. */
    private fun dateIso(
        date: Date,
        tz: TimeZone,
    ): String = newDateFmt(tz).format(date)

    private fun prepareDateLabel(apiDateStr: String?): String {
        if (apiDateStr == null) {
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }
        return try {
            val parsed = SimpleDateFormat("dd-MM-yyyy", Locale.US).parse(apiDateStr)
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(parsed!!)
        } catch (_: Exception) {
            apiDateStr
        }
    }

    private fun parseDate(
        dateStr: String,
        tz: TimeZone,
    ): Date? {
        if (dateStr.isBlank()) return null
        return try {
            newDateFmt(tz).parse(dateStr)
        } catch (_: Exception) {
            null
        }
    }
}
