package com.prayertime.data

import com.prayertime.domain.model.CityCoords
import com.prayertime.domain.model.Country

internal data class LocationCatalog(
    val countries: List<Country>,
    val citiesByCountry: Map<String, List<String>>,
    val citiesArByCountry: Map<String, Map<String, String>> = emptyMap(),
    val countryDefaults: Map<String, CityCoords>,
    val knownCityCoords: Map<String, CityCoords>,
)
