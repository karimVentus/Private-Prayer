package com.prayertime.data.repository

import com.prayertime.data.LocationCatalog
import com.prayertime.data.LocationDataSource
import com.prayertime.domain.model.CityResolutionResult
import com.prayertime.domain.model.Country
import com.prayertime.domain.repository.LocationRepository
import com.prayertime.locale.LocationNames
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalLocationRepository
    @Inject
    constructor() : LocationRepository {
        override fun countries(): List<Country> = accessibleCatalog()?.countries ?: emptyList()

        override fun citiesForCountry(countryCode: String): List<String> =
            accessibleCatalog()?.citiesByCountry?.get(countryCode) ?: emptyList()

        override fun isCatalogLoaded(): Boolean =
            LocationDataSource.catalogLoadState() == LocationDataSource.CatalogLoadState.READY &&
                LocationDataSource.loadedCatalog() != null

        override suspend fun awaitReady() {
            LocationDataSource.awaitReady()
        }

        override fun countryByCode(code: String): Country? = accessibleCatalog()?.countries?.firstOrNull { it.code == code }

        override fun arabicCityName(
            countryCode: String,
            englishName: String,
        ): String? = LocationDataSource.arabicCityName(countryCode, englishName)

        override fun resolveCanonicalCityName(
            countryCode: String,
            input: String,
        ): String = LocationDataSource.resolveCanonicalCityName(countryCode, input)

        override fun formatCityHeader(
            cityName: String,
            countryCode: String,
            languageTag: String?,
        ): String {
            val country = countryByCode(countryCode) ?: fallbackCountry(countryCode)
            val arabic = arabicCityName(countryCode, cityName)
            return LocationNames.formatCityHeader(cityName, country, arabic, languageTag)
        }

        private fun fallbackCountry(code: String): Country {
            val english =
                Locale.Builder()
                    .setRegion(code)
                    .build()
                    .getDisplayCountry(Locale.ENGLISH)
                    .ifBlank { code }
            return Country(name = english, code = code)
        }

        override fun resolveCityCoordinates(
            countryCode: String,
            cityName: String,
        ): CityResolutionResult = LocationDataSource.resolveCityCoordinates(countryCode, cityName)

        /**
         * Returns the catalog when [LocationDataSource.CatalogLoadState.READY].
         * Returns null while loading or after failure (callers show empty UI until [awaitReady]).
         * Throws if [LocationDataSource.initialize] was never started.
         */
        private fun accessibleCatalog(): LocationCatalog? =
            when (LocationDataSource.catalogLoadState()) {
                LocationDataSource.CatalogLoadState.NOT_STARTED ->
                    error(
                        "Location catalog not initialized — inject LocationCatalogInitializer " +
                            "or call LocationDataSource.initialize(context) at app startup",
                    )
                LocationDataSource.CatalogLoadState.LOADING,
                LocationDataSource.CatalogLoadState.FAILED,
                -> null
                LocationDataSource.CatalogLoadState.READY -> LocationDataSource.loadedCatalog()
            }
    }
