package com.prayertime.data.repository

import com.prayertime.data.local.AppDatabase
import com.prayertime.data.local.CityConfigDataSource
import com.prayertime.domain.model.CityConfig
import com.prayertime.domain.model.PrayerTimesResult
import com.prayertime.domain.model.SaveCityResult
import kotlinx.coroutines.flow.Flow

/** Offline-only repository: local geocode + Room cache + adhan-java. No network dependencies. */
class LocalPrayerTimesRepository private constructor(
    private val cityConfigDataSource: CityConfigDataSource,
    private val engine: PrayerTimesLocalEngine,
) : PrayerTimesRepository {
    constructor(
        cityConfigDataSource: CityConfigDataSource,
        database: AppDatabase,
    ) : this(cityConfigDataSource, PrayerTimesLocalEngine(database))

    internal companion object {
        fun withEngine(
            cityConfigDataSource: CityConfigDataSource,
            engine: PrayerTimesLocalEngine,
        ): LocalPrayerTimesRepository = LocalPrayerTimesRepository(cityConfigDataSource, engine)
    }

    override val cityConfig: Flow<CityConfig?> = cityConfigDataSource.cityConfig

    override val offlineOnly: Flow<Boolean> = cityConfigDataSource.offlineOnly

    override suspend fun setOfflineOnly(enabled: Boolean) {
        cityConfigDataSource.setOfflineOnly(enabled)
    }

    override suspend fun saveCityConfig(config: CityConfig): SaveCityResult {
        return when (val resolved = engine.resolveLocalCoordinates(config)) {
            is GeocodeResult.Success -> {
                cityConfigDataSource.save(resolved.config)
                SaveCityResult.Success(resolved.config)
            }
            is GeocodeResult.Error -> SaveCityResult.Error(resolved.type)
        }
    }

    /** DataStore city keys only — Room cache is kept for city-scoped reuse. */
    override suspend fun clearCityConfig() {
        cityConfigDataSource.clearCitySelection()
    }

    override suspend fun resetCityStore() {
        cityConfigDataSource.resetCityStore()
    }

    override suspend fun clearAllCaches() {
        engine.clearAllPrayerTimeCache()
    }

    override suspend fun invalidateTodayCache(config: CityConfig) {
        engine.invalidateTodayCache(
            engine.cityKeyFrom(config),
            engine.todayDateLabel(config.timezone),
        )
    }

    override suspend fun fetchTodayTimes(
        config: CityConfig,
        forceRefresh: Boolean,
    ): PrayerTimesResult {
        val cityKey = engine.cityKeyFrom(config)
        val todayLabel = engine.todayDateLabel(config.timezone)
        val cache =
            if (forceRefresh) {
                null
            } else {
                engine.getCachedTimes(cityKey, todayLabel, config.timezone)
            }
        return engine.fetchLocalTimes(config, cityKey, todayLabel, cache)
    }

    override suspend fun getCachedTodayTimes(config: CityConfig): PrayerTimesResult? {
        val cityKey = engine.cityKeyFrom(config)
        val todayLabel = engine.todayDateLabel(config.timezone)
        return engine.getCachedTimes(cityKey, todayLabel, config.timezone)
    }
}
