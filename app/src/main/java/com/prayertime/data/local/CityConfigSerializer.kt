package com.prayertime.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.prayertime.domain.model.CityConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "city_config")

class CityConfigSerializer
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : CityConfigDataSource {
        private val cityNameKey = stringPreferencesKey("city_name")
        private val countryCodeKey = stringPreferencesKey("country_code")
        private val timezoneKey = stringPreferencesKey("timezone")
        private val latitudeKey = doublePreferencesKey("latitude")
        private val longitudeKey = doublePreferencesKey("longitude")
        private val offlineOnlyKey = booleanPreferencesKey("offline_only")

        override val cityConfig: Flow<CityConfig?> =
            context.dataStore.data.map { prefs ->
                val cityName = prefs[cityNameKey] ?: return@map null
                val countryCode = prefs[countryCodeKey] ?: return@map null
                val timezone = prefs[timezoneKey] ?: return@map null
                val (latitude, longitude) = readCoordinates(prefs)
                CityConfig(cityName, countryCode, timezone, latitude, longitude)
            }

        override val offlineOnly: Flow<Boolean> =
            context.dataStore.data.map { prefs ->
                prefs[offlineOnlyKey] ?: true
            }

        override suspend fun save(config: CityConfig) {
            context.dataStore.edit { prefs ->
                prefs[cityNameKey] = config.cityName
                prefs[countryCodeKey] = config.countryCode
                prefs[timezoneKey] = config.timezone
                if (config.latitude != null && config.longitude != null) {
                    prefs[latitudeKey] = config.latitude
                    prefs[longitudeKey] = config.longitude
                } else {
                    prefs.remove(latitudeKey)
                    prefs.remove(longitudeKey)
                }
            }
        }

        /** Missing keys or legacy 0,0 sentinel → unresolved coords. */
        private fun readCoordinates(prefs: Preferences): Pair<Double?, Double?> {
            val latitude = prefs[latitudeKey]
            val longitude = prefs[longitudeKey]
            return when {
                latitude == null || longitude == null -> null to null
                latitude == 0.0 && longitude == 0.0 -> null to null
                else -> latitude to longitude
            }
        }

        override suspend fun setOfflineOnly(enabled: Boolean) {
            context.dataStore.edit { prefs ->
                prefs[offlineOnlyKey] = enabled
            }
        }

        override suspend fun clear() {
            context.dataStore.edit { it.clear() }
        }
    }
