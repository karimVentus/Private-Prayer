package com.prayertime.domain.usecase

import com.prayertime.domain.model.CityListItem
import com.prayertime.domain.model.Country
import com.prayertime.domain.repository.LocationRepository
import com.prayertime.domain.util.LocationNames
import javax.inject.Inject

/** Filters bundled country/city catalogs for the setup wizard. */
class SearchLocationsUseCase
    @Inject
    constructor(
        private val locations: LocationRepository,
    ) {
        fun filterCountries(
            query: String,
            languageTag: String?,
        ): List<Country> {
            val base =
                if (query.isBlank()) {
                    locations.countries()
                } else {
                    locations.countries().filter { country ->
                        val display = LocationNames.countryDisplay(country, languageTag)
                        LocationNames.matchesQuery(display, country.name, query)
                    }
                }
            return base.distinctBy { "${it.code}\u0000${it.name}" }
        }

        fun filterCities(
            countryCode: String,
            query: String,
            languageTag: String?,
        ): List<CityListItem> {
            val cities = locations.citiesForCountry(countryCode)
            val base =
                if (query.isBlank()) {
                    cities
                } else {
                    cities.filter { englishName ->
                        val arabicName = locations.arabicCityName(countryCode, englishName)
                        val display = LocationNames.cityDisplay(englishName, arabicName, languageTag)
                        LocationNames.matchesQuery(display, englishName, query) ||
                            (arabicName != null && LocationNames.matchesQuery(arabicName, englishName, query))
                    }
                }
            return base
                .map { englishName ->
                    val arabicName = locations.arabicCityName(countryCode, englishName)
                    CityListItem(
                        canonicalName = englishName,
                        displayName = LocationNames.cityDisplay(englishName, arabicName, languageTag),
                    )
                }
                .distinctBy { it.canonicalName }
        }

        fun showCustomCityFallback(cityQuery: String): Boolean = cityQuery.isNotBlank()
    }
