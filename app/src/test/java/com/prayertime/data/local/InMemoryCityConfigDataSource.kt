package com.prayertime.data.local

import com.prayertime.domain.model.CityConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class InMemoryCityConfigDataSource : CityConfigDataSource {
    private val _cityConfig = MutableStateFlow<CityConfig?>(null)
    private val _offlineOnly = MutableStateFlow(true)

    override val cityConfig: Flow<CityConfig?> = _cityConfig

    override val offlineOnly: Flow<Boolean> = _offlineOnly

    override suspend fun save(config: CityConfig) {
        _cityConfig.value = config
    }

    override suspend fun setOfflineOnly(enabled: Boolean) {
        _offlineOnly.value = enabled
    }

    /** Test-only: simulate external DataStore wipe without [resetCityStore] offlineOnly side effects. */
    fun emitCityConfig(config: CityConfig?) {
        _cityConfig.value = config
    }

    override suspend fun clearCitySelection() {
        _cityConfig.value = null
    }

    override suspend fun resetCityStore() {
        _cityConfig.value = null
        _offlineOnly.value = true
    }
}
