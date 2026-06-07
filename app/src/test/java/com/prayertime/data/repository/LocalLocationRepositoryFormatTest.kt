package com.prayertime.data.repository

import com.prayertime.data.LocationCatalog
import com.prayertime.data.LocationDataSource
import com.prayertime.domain.model.CityCoords
import com.prayertime.domain.model.Country
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LocalLocationRepositoryFormatTest {
    private val repository = LocalLocationRepository()

    @Before
    fun setup() {
        LocationDataSource.initializeForTests(
            LocationCatalog(
                countries = listOf(Country("Germany", "DE")),
                citiesByCountry = mapOf("DE" to listOf("Hameln")),
                citiesArByCountry = mapOf("DE" to mapOf("Hameln" to "هاملن")),
                countryDefaults = mapOf("DE" to CityCoords(52.0, 9.0, "Europe/Berlin")),
                knownCityCoords = emptyMap(),
            ),
        )
    }

    @After
    fun tearDown() {
        LocationDataSource.resetForTests()
    }

    @Test
    fun formatCityHeader_arabic_shows_localized_city_and_country() {
        val header = repository.formatCityHeader("Hameln", "DE", "ar")
        assertTrue(header.contains("هاملن"))
        assertTrue(header.contains("ألمانيا") || header.contains("المانيا"))
    }
}
