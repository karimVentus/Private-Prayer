package com.prayertime.data.repository

import com.prayertime.data.local.AppDatabase
import com.prayertime.data.local.CityConfigDataSource
import com.prayertime.data.remote.NetworkMapper
import com.prayertime.data.remote.PrayerApi
import com.prayertime.domain.model.CityConfig
import com.prayertime.domain.model.FetchError
import com.prayertime.domain.model.PrayerTimesResult
import com.prayertime.domain.model.SaveCityResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Minimum prayer times expected from the API: 5 fard + 1 shuruq. */
private const val REQUIRED_PRAYER_COUNT = 6

/** Online flavor: network fetch when privacy mode is off; composes [LocalPrayerTimesRepository] for fallback. */
class OnlinePrayerTimesRepository(
    private val cityConfigDataSource: CityConfigDataSource,
    database: AppDatabase,
    private val api: PrayerApi,
) : PrayerTimesRepository {
    private val engine = PrayerTimesLocalEngine(database)
    private val local = LocalPrayerTimesRepository.withEngine(cityConfigDataSource, engine)

    override val cityConfig: Flow<CityConfig?> = local.cityConfig

    override val offlineOnly: Flow<Boolean> = local.offlineOnly

    override suspend fun setOfflineOnly(enabled: Boolean) {
        local.setOfflineOnly(enabled)
    }

    override suspend fun saveCityConfig(config: CityConfig): SaveCityResult {
        if (cityConfigDataSource.offlineOnly.first()) {
            return local.saveCityConfig(config)
        }
        return try {
            val combined = api.getTimingsWithGeocode(config.cityName, config.countryCode)
            if (combined == null) {
                return fallbackSave(config)
            }
            val resolvedTz = combined.timezone.ifEmpty { null }
            if (resolvedTz == null || combined.latitude == 0.0 && combined.longitude == 0.0) {
                return fallbackSave(config)
            }
            val enriched =
                config.copy(
                    timezone = resolvedTz,
                    latitude = combined.latitude,
                    longitude = combined.longitude,
                )
            val cityKey = engine.cityKeyFrom(enriched)
            val todayLabel = engine.todayDateLabel(enriched.timezone)
            cityConfigDataSource.save(enriched)
            try {
                engine.cacheTimingsFromApiResponse(
                    combined.timingsMap,
                    combined.date,
                    enriched.timezone,
                    cityKey,
                    todayLabel,
                )
            } catch (_: Exception) {
                // cache failure is non-fatal — config already saved
            }
            SaveCityResult.Success(enriched)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            fallbackSave(config)
        }
    }

    /** Keep wizard-resolved coords when API fails; geocode only when coords are still missing. */
    private suspend fun fallbackSave(config: CityConfig): SaveCityResult {
        if (config.hasValidCoordinates) {
            cityConfigDataSource.save(config)
            return SaveCityResult.Success(config)
        }
        return local.saveCityConfig(config)
    }

    override suspend fun clearCityConfig() {
        local.clearCityConfig()
    }

    override suspend fun resetCityStore() {
        local.resetCityStore()
    }

    override suspend fun clearAllCaches() {
        local.clearAllCaches()
    }

    override suspend fun invalidateTodayCache(config: CityConfig) {
        local.invalidateTodayCache(config)
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

        if (!cityConfigDataSource.offlineOnly.first()) {
            if (cache != null) {
                return cache
            }
            when (val apiResult = fetchFromApi(config, cityKey, todayLabel)) {
                is PrayerTimesResult.Success -> return apiResult
                is PrayerTimesResult.Error -> {
                    handleApiError(apiResult, config, cache)?.let { return it }
                }
                null -> Unit
            }
        }

        return engine.fetchLocalTimes(config, cityKey, todayLabel, cache)
    }

    override suspend fun getCachedTodayTimes(config: CityConfig): PrayerTimesResult? {
        val cityKey = engine.cityKeyFrom(config)
        val todayLabel = engine.todayDateLabel(config.timezone)
        return engine.getCachedTimes(cityKey, todayLabel, config.timezone)
    }

    private suspend fun fetchFromApi(
        config: CityConfig,
        cityKey: String,
        todayLabel: String,
    ): PrayerTimesResult? {
        return try {
            val response = api.getTimingsByCity(config.cityName, config.countryCode) ?: return null
            val apiTimezone = response.timezone.ifEmpty { config.timezone }
            val prayerTimes =
                AladhanTimingsMapper.buildPrayerTimes(
                    response.timingsMap,
                    response.date,
                    apiTimezone,
                )
            if (prayerTimes.size < REQUIRED_PRAYER_COUNT) return null
            engine.cacheToRoom(prayerTimes, cityKey, todayLabel)
            engine.cleanupOldEntries(cityKey, config.timezone)
            engine.buildResult(prayerTimes, apiTimezone)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            PrayerTimesResult.Error(NetworkMapper.mapError(e))
        }
    }

    private fun handleApiError(
        apiResult: PrayerTimesResult.Error,
        config: CityConfig,
        cache: PrayerTimesResult?,
    ): PrayerTimesResult? {
        val hasValidCoords = config.hasValidCoordinates
        return if (!hasValidCoords) {
            cache ?: apiResult
        } else if (apiResult.type != FetchError.NETWORK) {
            apiResult
        } else {
            null
        }
    }
}
