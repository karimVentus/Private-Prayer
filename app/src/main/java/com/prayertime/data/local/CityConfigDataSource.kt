package com.prayertime.data.local

import com.prayertime.domain.model.CityConfig
import kotlinx.coroutines.flow.Flow

interface CityConfigDataSource {
    val cityConfig: Flow<CityConfig?>

    val offlineOnly: Flow<Boolean>

    suspend fun save(config: CityConfig)

    suspend fun setOfflineOnly(enabled: Boolean)

    suspend fun clear()
}
