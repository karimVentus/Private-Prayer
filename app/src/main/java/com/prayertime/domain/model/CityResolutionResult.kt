package com.prayertime.domain.model

/**
 * Result of resolving city coordinates in offline mode.
 * Distinguishes between exact matches, country fallbacks, and errors.
 */
sealed class CityResolutionResult {
    /**
     * City found in knownCityCoords with exact coordinates.
     */
    data class Found(val coords: CityCoords) : CityResolutionResult()

    /**
     * City not found; using country-level coordinates as fallback.
     * Returned when city is not in knownCityCoords but country is valid.
     */
    data class Fallback(val coords: CityCoords, val cityName: String, val countryCode: String) :
        CityResolutionResult()

    /**
     * Country code is not recognized or supported.
     * No fallback available.
     */
    data class InvalidCountry(val countryCode: String) : CityResolutionResult()
}
