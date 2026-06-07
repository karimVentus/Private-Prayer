package com.prayertime.widget

import com.prayertime.data.local.AppPreferencesDataSource
import com.prayertime.data.repository.PrayerTimesRepository
import com.prayertime.domain.repository.LocationRepository
import com.prayertime.domain.calculator.HijriCalculator
import com.prayertime.domain.model.CityConfig
import com.prayertime.domain.model.HijriDate
import com.prayertime.domain.model.PrayerTimesResult
import com.prayertime.domain.model.UpcomingEvent
import com.prayertime.ui.theme.AppTheme
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetSnapshotLoader
    @Inject
    constructor(
        private val repository: PrayerTimesRepository,
        private val preferences: AppPreferencesDataSource,
        private val locations: LocationRepository,
    ) {
        suspend fun load(): WidgetSnapshot {
            val appTheme = AppTheme.fromStorage(preferences.readAppThemeOnce())
            val config = repository.cityConfig.first() ?: return WidgetSnapshot(WidgetSnapshot.State.NO_CITY, appTheme = appTheme)
            locations.awaitReady()
            val languageTag = preferences.readAppLanguageTagOnce()
            val cityLabel =
                locations.formatCityHeader(
                    cityName = config.cityName,
                    countryCode = config.countryCode,
                    languageTag = languageTag,
                )
            val hijriContext = loadHijriContext(config.timezone)
            return when (val result = repository.fetchTodayTimes(config)) {
                is PrayerTimesResult.Success ->
                    successSnapshot(cityLabel, config.timezone, result, hijriContext, appTheme)
                is PrayerTimesResult.Error ->
                    errorSnapshot(config, cityLabel, hijriContext, appTheme)
            }
        }

        private data class HijriContext(
            val hijriDate: HijriDate?,
            val upcomingEvent: UpcomingEvent?,
        )

        private fun loadHijriContext(timezone: String): HijriContext {
            val tz = TimeZone.getTimeZone(timezone)
            val cal = Calendar.getInstance(tz)
            val todayHijri =
                try {
                    HijriCalculator.gregorianToHijri(
                        cal[Calendar.YEAR],
                        cal[Calendar.MONTH] + 1,
                        cal[Calendar.DAY_OF_MONTH],
                    )
                } catch (_: Exception) {
                    null
                }
            val upcomingEvent =
                try {
                    if (todayHijri != null) HijriCalculator.nextUpcomingEvent(todayHijri) else null
                } catch (_: Exception) {
                    null
                }
            return HijriContext(todayHijri, upcomingEvent)
        }

        private fun successSnapshot(
            cityLabel: String,
            timezone: String,
            result: PrayerTimesResult.Success,
            hijri: HijriContext,
            appTheme: AppTheme,
        ): WidgetSnapshot =
            WidgetSnapshot(
                state = WidgetSnapshot.State.READY,
                appTheme = appTheme,
                cityLabel = cityLabel,
                timezone = timezone,
                times = result.times,
                nextPrayer = result.nextPrayer,
                countdownMillis = result.countdown,
                hijriDate = hijri.hijriDate,
                upcomingEvent = hijri.upcomingEvent,
            )

        private suspend fun errorSnapshot(
            config: CityConfig,
            cityLabel: String,
            hijri: HijriContext,
            appTheme: AppTheme,
        ): WidgetSnapshot {
            val cached = repository.getCachedTodayTimes(config)
            return if (cached is PrayerTimesResult.Success) {
                WidgetSnapshot(
                    state = WidgetSnapshot.State.STALE,
                    appTheme = appTheme,
                    cityLabel = cityLabel,
                    timezone = config.timezone,
                    times = cached.times,
                    nextPrayer = cached.nextPrayer,
                    countdownMillis = cached.countdown,
                    hijriDate = hijri.hijriDate,
                    upcomingEvent = hijri.upcomingEvent,
                )
            } else {
                WidgetSnapshot(
                    state = WidgetSnapshot.State.ERROR,
                    appTheme = appTheme,
                    cityLabel = cityLabel,
                    hijriDate = hijri.hijriDate,
                    upcomingEvent = hijri.upcomingEvent,
                )
            }
        }
    }
