package com.prayertime.alarm

import com.prayertime.data.repository.PrayerTimesRepository
import com.prayertime.domain.calculator.LocalPrayerTimeCalculator
import com.prayertime.domain.calculator.PrayerTimeCalculator
import com.prayertime.domain.model.CityConfig
import com.prayertime.domain.model.FetchError
import com.prayertime.domain.model.PrayerTimesResult
import java.util.TimeZone

/** Resolves today's prayer times for boot-time alarm scheduling (network → cache → local). */
internal object BootPrayerTimesResolver {
    suspend fun resolve(
        repository: PrayerTimesRepository,
        config: CityConfig,
    ): PrayerTimesResult =
        when (val fetched = repository.fetchTodayTimes(config)) {
            is PrayerTimesResult.Success -> fetched
            is PrayerTimesResult.Error ->
                repository.getCachedTodayTimes(config)
                    ?: localFallback(config)
        }

    fun localFallback(config: CityConfig): PrayerTimesResult {
        val lat = config.latitude
        val lon = config.longitude
        if (lat == null || lon == null) {
            return PrayerTimesResult.Error(FetchError.UNKNOWN)
        }
        val times =
            LocalPrayerTimeCalculator.calculate(
                latitude = lat,
                longitude = lon,
                timezone = config.timezone,
            )
        return PrayerTimeCalculator.buildResult(
            times,
            timezone = TimeZone.getTimeZone(config.timezone),
        )
    }
}
