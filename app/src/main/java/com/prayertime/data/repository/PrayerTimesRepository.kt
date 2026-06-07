package com.prayertime.data.repository

import com.prayertime.domain.model.CityConfig
import com.prayertime.domain.model.PrayerTimesResult
import com.prayertime.domain.model.SaveCityResult
import kotlinx.coroutines.flow.Flow

interface PrayerTimesRepository {
    val cityConfig: Flow<CityConfig?>
    val offlineOnly: Flow<Boolean>

    suspend fun setOfflineOnly(enabled: Boolean)

    suspend fun saveCityConfig(config: CityConfig): SaveCityResult

    /**
     * Clears the saved city from DataStore only.
     *
     * Room cache is intentionally retained for city-scoped reuse; see [clearAllCaches]
     * or [invalidateTodayCache] to drop cached prayer times.
     */
    suspend fun clearCityConfig()

    /** Clears city DataStore entirely (city + privacy/offline mode). */
    suspend fun resetCityStore()

    /** Deletes ALL cached prayer times from Room. Use for cache poisoning recovery or storage pressure. */
    suspend fun clearAllCaches()

    /** Drops today's cached prayer times for [config]'s city so the next fetch recalculates or re-downloads. */
    suspend fun invalidateTodayCache(config: CityConfig)

    suspend fun fetchTodayTimes(config: CityConfig): PrayerTimesResult

    /** Room cache for today's city date only — does not calculate or call the network. */
    suspend fun getCachedTodayTimes(config: CityConfig): PrayerTimesResult?
}
