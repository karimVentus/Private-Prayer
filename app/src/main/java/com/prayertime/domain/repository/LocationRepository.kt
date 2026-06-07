package com.prayertime.domain.repository

import com.prayertime.domain.model.CityResolutionResult
import com.prayertime.domain.model.Country

/** Read-only access to bundled country/city catalogs for setup and geocoding. */
interface LocationRepository {
    fun countries(): List<Country>

    fun citiesForCountry(countryCode: String): List<String>

    fun isCatalogLoaded(): Boolean

    suspend fun awaitReady()

    fun countryByCode(code: String): Country?

    fun arabicCityName(
        countryCode: String,
        englishName: String,
    ): String?

    /** Map user input (English or Arabic display name) to canonical English catalog key. */
    fun resolveCanonicalCityName(
        countryCode: String,
        input: String,
    ): String

    fun formatCityHeader(
        cityName: String,
        countryCode: String,
        languageTag: String?,
    ): String

    /** Resolve bundled coordinates for [countryCode] + [cityName] before persisting [CityConfig]. */
    fun resolveCityCoordinates(
        countryCode: String,
        cityName: String,
    ): CityResolutionResult
}
