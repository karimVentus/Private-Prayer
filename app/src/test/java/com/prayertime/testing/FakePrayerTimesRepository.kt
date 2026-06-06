package com.prayertime.testing

import com.prayertime.data.local.InMemoryCityConfigDataSource
import com.prayertime.data.repository.PrayerTimesRepository
import com.prayertime.domain.model.CityConfig
import com.prayertime.domain.model.FetchError
import com.prayertime.domain.model.Prayer
import com.prayertime.domain.model.PrayerTime
import com.prayertime.domain.model.PrayerTimesResult
import com.prayertime.domain.model.SaveCityError
import com.prayertime.domain.model.SaveCityResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Configurable [PrayerTimesRepository] for ViewModel/worker tests.
 * Use companion factories for common presets.
 */
class FakePrayerTimesRepository private constructor(
    private val citySource: InMemoryCityConfigDataSource?,
    val workerCityConfig: MutableStateFlow<CityConfig?>?,
    private val workerOfflineOnly: MutableStateFlow<Boolean>?,
    private val saveHandler: suspend (CityConfig) -> SaveCityResult,
    private val defaultFetch: suspend (CityConfig) -> PrayerTimesResult,
) : PrayerTimesRepository {
    var fetchInvoked: Boolean = false
        private set

    /** Prayer-times ViewModel tests: one-shot fetch behavior override. */
    var fetchOverride: (() -> PrayerTimesResult)? = null

    /** Worker tests: assign before [fetchTodayTimes]. */
    var fetchResult: PrayerTimesResult = PrayerTimesResult.Error(FetchError.UNKNOWN)

    override val cityConfig: Flow<CityConfig?> =
        citySource?.cityConfig ?: checkNotNull(workerCityConfig)

    override val offlineOnly: Flow<Boolean> =
        citySource?.offlineOnly ?: checkNotNull(workerOfflineOnly)

    override suspend fun setOfflineOnly(enabled: Boolean) {
        if (citySource != null) {
            citySource.setOfflineOnly(enabled)
        } else {
            checkNotNull(workerOfflineOnly).value = enabled
        }
    }

    override suspend fun saveCityConfig(config: CityConfig): SaveCityResult = saveHandler(config)

    override suspend fun clearCityConfig() {
        citySource?.clear()
        workerCityConfig?.value = null
    }

    override suspend fun clearAllCaches() = Unit

    override suspend fun invalidateTodayCache(config: CityConfig) = Unit

    override suspend fun fetchTodayTimes(config: CityConfig): PrayerTimesResult {
        fetchInvoked = true
        return fetchOverride?.invoke()
            ?: if (workerCityConfig != null) {
                fetchResult
            } else {
                defaultFetch(config)
            }
    }

    /** Boot resolver tests: return from [getCachedTodayTimes]. */
    var cachedTodayOverride: PrayerTimesResult? = null

    override suspend fun getCachedTodayTimes(config: CityConfig): PrayerTimesResult? = cachedTodayOverride

    companion object {
        fun forCitySetup(citySource: InMemoryCityConfigDataSource): FakePrayerTimesRepository =
            FakePrayerTimesRepository(
                citySource = citySource,
                workerCityConfig = null,
                workerOfflineOnly = null,
                saveHandler = { config ->
                    val enriched =
                        config.copy(
                            latitude = config.latitude ?: 33.5,
                            longitude = config.longitude ?: 36.3,
                        )
                    citySource.save(enriched)
                    SaveCityResult.Success(enriched)
                },
                defaultFetch = { PrayerTimesResult.Error(FetchError.UNKNOWN) },
            )

        fun failingSave(citySource: InMemoryCityConfigDataSource): FakePrayerTimesRepository =
            FakePrayerTimesRepository(
                citySource = citySource,
                workerCityConfig = null,
                workerOfflineOnly = null,
                saveHandler = { SaveCityResult.Error(SaveCityError.NETWORK) },
                defaultFetch = { PrayerTimesResult.Error(FetchError.NETWORK) },
            )

        fun forSettings(citySource: InMemoryCityConfigDataSource): FakePrayerTimesRepository =
            FakePrayerTimesRepository(
                citySource = citySource,
                workerCityConfig = null,
                workerOfflineOnly = null,
                saveHandler = { SaveCityResult.Error(SaveCityError.UNKNOWN) },
                defaultFetch = { PrayerTimesResult.Error(FetchError.UNKNOWN) },
            )

        fun forPrayerTimes(citySource: InMemoryCityConfigDataSource): FakePrayerTimesRepository =
            FakePrayerTimesRepository(
                citySource = citySource,
                workerCityConfig = null,
                workerOfflineOnly = null,
                saveHandler = { config ->
                    citySource.save(config)
                    SaveCityResult.Success(config)
                },
                defaultFetch = { defaultSuccessResult() },
            )

        fun forWorker(): FakePrayerTimesRepository =
            FakePrayerTimesRepository(
                citySource = null,
                workerCityConfig = MutableStateFlow(null),
                workerOfflineOnly = MutableStateFlow(true),
                saveHandler = { SaveCityResult.Error(SaveCityError.UNKNOWN) },
                defaultFetch = { defaultSuccessResult() },
            )

        fun defaultSuccessResult(): PrayerTimesResult.Success {
            val now = System.currentTimeMillis()
            return PrayerTimesResult.Success(
                times =
                    listOf(
                        PrayerTime(Prayer.FAJR, "05:00", now + 3_600_000),
                        PrayerTime(Prayer.SHURUQ, "06:30", now + 7_200_000),
                        PrayerTime(Prayer.DHUHR, "12:30", now + 25_200_000),
                        PrayerTime(Prayer.ASR, "15:45", now + 36_000_000),
                        PrayerTime(Prayer.MAGHRIB, "18:10", now + 45_000_000),
                        PrayerTime(Prayer.ISHA, "19:40", now + 50_000_000),
                    ),
                nextPrayer = Prayer.FAJR,
                countdown = 3_600_000,
            )
        }
    }
}
