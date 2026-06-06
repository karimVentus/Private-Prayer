package com.prayertime.domain.calculator

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.HighLatitudeRule
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import com.prayertime.domain.model.Prayer
import com.prayertime.domain.model.PrayerTime
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

object LocalPrayerTimeCalculator {
    private const val HIGH_LATITUDE_THRESHOLD = 48.0

    fun calculate(
        latitude: Double,
        longitude: Double,
        timezone: String,
        date: Date = Date(),
    ): List<PrayerTime> {
        val coordinates = Coordinates(latitude, longitude)
        val dateComponents = dateComponentsFor(date, timezone)
        // Umm al-Qura (Makkah) + Shafi/ Maliki / Hanbali Asr (not Hanafi).
        val params = CalculationMethod.UMM_AL_QURA.parameters
        params.madhab = Madhab.SHAFI
        if (abs(latitude) >= HIGH_LATITUDE_THRESHOLD) {
            // Required for e.g. Germany (~52°N); otherwise Fajr/Isha are far too early/late in summer.
            params.highLatitudeRule = HighLatitudeRule.TWILIGHT_ANGLE
        }

        val times = PrayerTimes(coordinates, dateComponents, params)
        val tz = TimeZone.getTimeZone(timezone)

        return listOf(
            PrayerTime(Prayer.FAJR, formatTime(times.fajr, tz), times.fajr.time),
            // Shuruq slot = sunrise, matching common prayer apps — not sunrise + 20 min.
            PrayerTime(Prayer.SHURUQ, formatTime(times.sunrise, tz), times.sunrise.time),
            PrayerTime(Prayer.DHUHR, formatTime(times.dhuhr, tz), times.dhuhr.time),
            PrayerTime(Prayer.ASR, formatTime(times.asr, tz), times.asr.time),
            PrayerTime(Prayer.MAGHRIB, formatTime(times.maghrib, tz), times.maghrib.time),
            PrayerTime(Prayer.ISHA, formatTime(times.isha, tz), times.isha.time),
        )
    }

    private fun dateComponentsFor(
        date: Date,
        timezone: String,
    ): DateComponents {
        val cal =
            Calendar.getInstance(TimeZone.getTimeZone(timezone)).apply {
                time = date
            }
        return DateComponents(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
        )
    }

    private fun formatTime(
        date: Date,
        tz: TimeZone,
    ): String {
        val fmt = SimpleDateFormat("HH:mm", Locale.US).apply { this.timeZone = tz }
        return fmt.format(date)
    }
}
