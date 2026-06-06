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

    /** Test-only: simulate external DataStore wipe without [clear] offlineOnly side effects. */
    fun emitCityConfig(config: CityConfig?) {
        _cityConfig.value = config
    }

    override suspend fun clear() {
        _cityConfig.value = null
        // Mirror DataStore clear: offlineOnly snapshot re-emits even when still true.
        _offlineOnly.value = false
        _offlineOnly.value = true
    }
}
