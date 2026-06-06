package com.prayertime.domain.usecase

import com.prayertime.domain.model.Country
import com.prayertime.domain.repository.LocationRepository
import com.prayertime.locale.TextNormalizer
import javax.inject.Inject

/** Filters bundled country/city catalogs for the setup wizard. */
class SearchLocationsUseCase
    @Inject
    constructor(
        private val locations: LocationRepository,
    ) {
        fun filterCountries(query: String): List<Country> {
            val base =
                if (query.isBlank()) {
                    locations.countries()
                } else {
                    locations.countries().filter {
                        it.name.contains(query, ignoreCase = true)
                    }
                }
            return base.distinctBy { "${it.code}\u0000${it.name}" }
        }

        fun filterCities(
            countryCode: String,
            query: String,
        ): List<String> {
            val cities = locations.citiesForCountry(countryCode)
            val base =
                if (query.isBlank()) {
                    cities
                } else {
                    val foldedQuery = TextNormalizer.foldForLookup(query)
                    cities.filter { TextNormalizer.foldForLookup(it).contains(foldedQuery) }
                }
            return base.distinct()
        }

        fun showCustomCityFallback(cityQuery: String): Boolean = cityQuery.isNotBlank()
    }
