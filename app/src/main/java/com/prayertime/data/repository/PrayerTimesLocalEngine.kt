package com.prayertime.data.repository

import androidx.room.withTransaction
import com.prayertime.data.LocationDataSource
import com.prayertime.data.local.AppDatabase
import com.prayertime.data.local.PrayerTimeEntity
import com.prayertime.domain.calculator.HijriCalculator
import com.prayertime.domain.calculator.LocalPrayerTimeCalculator
import com.prayertime.domain.calculator.PrayerDayLabels
import com.prayertime.domain.calculator.PrayerTimeCalculator
import com.prayertime.domain.model.CityConfig
import com.prayertime.domain.model.CityResolutionResult
import com.prayertime.domain.model.FetchError
import com.prayertime.domain.model.Prayer
import com.prayertime.domain.model.PrayerTime
import com.prayertime.domain.model.PrayerTimesResult
import com.prayertime.domain.model.SaveCityError
import java.util.TimeZone

/** Shared local geocode, Room cache, and on-device prayer calculation. No network types. */
internal class PrayerTimesLocalEngine(
    private val database: AppDatabase,
) {
    suspend fun resolveLocalCoordinates(config: CityConfig): GeocodeResult {
        if (config.hasValidCoordinates) {
            return GeocodeResult.Success(config)
        }
        LocationDataSource.awaitReady()
        return when (val resolution = LocationDataSource.resolveCityCoordinates(config.countryCode, config.cityName)) {
            is CityResolutionResult.Found -> {
                GeocodeResult.Success(
                    config.copy(
                        timezone = resolution.coords.timezone,
                        latitude = resolution.coords.latitude,
                        longitude = resolution.coords.longitude,
                    ),
                )
            }
            is CityResolutionResult.Fallback -> GeocodeResult.Error(SaveCityError.CITY_NOT_FOUND)
            is CityResolutionResult.InvalidCountry -> GeocodeResult.Error(SaveCityError.INVALID_COUNTRY)
        }
    }

    suspend fun fetchLocalTimes(
        config: CityConfig,
        cityKey: String,
        todayLabel: String,
        cache: PrayerTimesResult?,
    ): PrayerTimesResult {
        val hasValidCoords = config.hasValidCoordinates
        return if (hasValidCoords) {
            if (cache != null) return cache
            try {
                val latitude = checkNotNull(config.latitude)
                val longitude = checkNotNull(config.longitude)
                val times =
                    LocalPrayerTimeCalculator.calculate(
                        latitude,
                        longitude,
                        config.timezone,
                    )
                cacheToRoom(times, cityKey, todayLabel)
                cleanupOldEntries(cityKey, config.timezone)
                buildResult(times, config.timezone)
            } catch (_: Exception) {
                PrayerTimesResult.Error(FetchError.UNKNOWN)
            }
        } else {
            cache ?: PrayerTimesResult.Error(FetchError.MISSING_COORDINATES)
        }
    }

    /** Delete all cached prayer times. Reserved for tests / future "clear app data" — not used on city switch. */
    suspend fun clearAllPrayerTimeCache() {
        database.prayerTimeDao().deleteAll()
    }

    /** Delete cached prayer times for one city on one calendar day (city timezone). */
    suspend fun invalidateTodayCache(
        cityKey: String,
        dateLabel: String,
    ) {
        database.prayerTimeDao().deleteByCityAndDate(cityKey, dateLabel)
    }

    /** Delete cached prayer times for one city (all dates). */
    suspend fun clearCityCache(cityKey: String) {
        database.prayerTimeDao().deleteByCityKey(cityKey)
    }

    suspend fun getCachedTimes(
        cityKey: String,
        dateLabel: String,
        timezone: String,
    ): PrayerTimesResult? {
        val entities = database.prayerTimeDao().getByCityAndDate(cityKey, dateLabel)
        val times = entities.map { it.toDomain() }.dedupeByPrayer()
        return if (times.isEmpty()) null else buildResult(times, timezone)
    }

    /** Cache timings from an already-parsed API response — used to avoid a second network call during city save. */
    suspend fun cacheTimingsFromApiResponse(
        timingsMap: Map<String, String>,
        date: String,
        timezone: String,
        cityKey: String,
        todayLabel: String,
    ) {
        val prayerTimes = AladhanTimingsMapper.buildPrayerTimes(timingsMap, date, timezone)
        if (prayerTimes.size >= 6) {
            cacheToRoom(prayerTimes, cityKey, todayLabel)
            cleanupOldEntries(cityKey, timezone)
        }
    }

    suspend fun cacheToRoom(
        times: List<PrayerTime>,
        cityKey: String,
        dateLabel: String,
    ) {
        // dateLabel is "yyyy-MM-dd" — parse to compute Hijri date
        val hijri =
            try {
                val parts = dateLabel.split("-")
                if (parts.size == 3) {
                    HijriCalculator.gregorianToHijri(
                        parts[0].toInt(),
                        parts[1].toInt(),
                        parts[2].toInt(),
                    )
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }
        database.withTransaction {
            database.prayerTimeDao().deleteByCityAndDate(cityKey, dateLabel)
            database.prayerTimeDao().insertAll(
                times.map { pt ->
                    PrayerTimeEntity(
                        prayer = pt.prayer.name,
                        displayTime = pt.displayTime,
                        timestamp = pt.timestamp,
                        dateLabel = dateLabel,
                        cityKey = cityKey,
                        hijriYear = hijri?.year,
                        hijriMonth = hijri?.month,
                        hijriDay = hijri?.day,
                    )
                },
            )
        }
    }

    suspend fun cleanupOldEntries(
        cityKey: String,
        timezone: String,
        now: Long = System.currentTimeMillis(),
    ) {
        val tz = TimeZone.getTimeZone(timezone)
        val cutoff = PrayerDayLabels.daysAgo(7, tz, now)
        database.prayerTimeDao().deleteOlderThan(cityKey, cutoff)
    }

    fun buildResult(
        times: List<PrayerTime>,
        timezone: String,
    ): PrayerTimesResult =
        PrayerTimeCalculator.buildResult(
            times.dedupeByPrayer(),
            timezone = TimeZone.getTimeZone(timezone),
        )

    /** Room has no unique (city, day, prayer) constraint; stale rows can duplicate columns in the widget. */
    private fun List<PrayerTime>.dedupeByPrayer(): List<PrayerTime> {
        val seen = LinkedHashSet<Prayer>()
        return filter { seen.add(it.prayer) }
    }

    fun todayDateLabel(timezone: String): String =
        PrayerDayLabels.format(
            System.currentTimeMillis(),
            TimeZone.getTimeZone(timezone),
        )

    fun cityKeyFrom(config: CityConfig): String = "${config.countryCode}_${config.cityName}"

    private fun PrayerTimeEntity.toDomain(): PrayerTime =
        PrayerTime(
            prayer = parseCachedPrayer(prayer),
            displayTime = displayTime,
            timestamp = timestamp,
        )

    private fun parseCachedPrayer(name: String): Prayer =
        when (name) {
            "DUHA" -> Prayer.SHURUQ
            else -> Prayer.valueOf(name)
        }
}
