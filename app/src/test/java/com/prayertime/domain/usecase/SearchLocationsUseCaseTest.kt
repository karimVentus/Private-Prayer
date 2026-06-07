package com.prayertime.domain.usecase

import com.prayertime.data.LocationCatalog
import com.prayertime.data.LocationDataSource
import com.prayertime.domain.model.CityCoords
import com.prayertime.domain.model.Country
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SearchLocationsUseCaseTest {
    private val useCase = SearchLocationsUseCase(TestLocationRepository())

    @Before
    fun setup() {
        LocationDataSource.initializeForTests(
            LocationCatalog(
                countries =
                    listOf(
                        Country("Germany", "DE"),
                        Country("Syria", "SY"),
                    ),
                citiesByCountry =
                    mapOf(
                        "DE" to listOf("Hameln", "Berlin"),
                        "SY" to listOf("Damascus", "Aleppo"),
                    ),
                citiesArByCountry =
                    mapOf(
                        "DE" to mapOf("Hameln" to "هاملن", "Berlin" to "برلين"),
                        "SY" to mapOf("Damascus" to "دمشق", "Aleppo" to "حلب"),
                    ),
                countryDefaults =
                    mapOf(
                        "DE" to CityCoords(52.0, 9.0, "Europe/Berlin"),
                        "SY" to CityCoords(33.0, 36.0, "Asia/Damascus"),
                    ),
                knownCityCoords = emptyMap(),
            ),
        )
    }

    @After
    fun tearDown() {
        LocationDataSource.resetForTests()
    }

    @Test
    fun filterCountries_arabic_query_matches_locale_country_name() {
        val filtered = useCase.filterCountries("ألمان", "ar")
        assertEquals(listOf("DE"), filtered.map { it.code })
    }

    @Test
    fun filterCities_arabic_query_matches_arabic_city_name() {
        val filtered = useCase.filterCities("SY", "دمش", "ar")
        assertEquals(listOf("Damascus"), filtered.map { it.canonicalName })
        assertEquals(listOf("دمشق"), filtered.map { it.displayName })
    }

    @Test
    fun filterCities_english_ui_keeps_english_display() {
        val filtered = useCase.filterCities("DE", "ham", "en")
        assertTrue(filtered.any { it.canonicalName == "Hameln" && it.displayName == "Hameln" })
    }
}

private class TestLocationRepository : com.prayertime.domain.repository.LocationRepository by
    com.prayertime.data.repository.LocalLocationRepository()
