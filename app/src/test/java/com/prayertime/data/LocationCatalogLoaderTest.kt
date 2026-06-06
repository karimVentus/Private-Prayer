package com.prayertime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationCatalogLoaderTest {
    @Test
    fun parse_valid_minimal_catalog() {
        val json =
            """
            {
              "countries": [{"name": "Testland", "code": "TL"}],
              "citiesByCountry": {"TL": ["Capital"]},
              "countryDefaults": {
                "TL": {"latitude": 1.0, "longitude": 2.0, "timezone": "UTC"}
              },
              "knownCityCoords": {
                "TL_Capital": {"latitude": 1.1, "longitude": 2.1, "timezone": "UTC"}
              }
            }
            """.trimIndent()

        val catalog = LocationCatalogLoader.parse(json)
        assertEquals(1, catalog.countries.size)
        assertEquals("TL", catalog.countries.first().code)
        assertEquals(listOf("Capital"), catalog.citiesByCountry["TL"])
    }

    @Test
    fun parse_missing_countries_throws() {
        val json = """{"citiesByCountry":{},"countryDefaults":{},"knownCityCoords":{}}"""
        try {
            LocationCatalogLoader.parse(json)
            error("Expected InvalidCatalogException")
        } catch (e: LocationCatalogLoader.InvalidCatalogException) {
            assertTrue(e.message!!.contains("countries"))
        }
    }

    @Test
    fun parse_malformed_root_throws() {
        try {
            LocationCatalogLoader.parse("[]")
            error("Expected InvalidCatalogException")
        } catch (e: LocationCatalogLoader.InvalidCatalogException) {
            assertTrue(e.message!!.contains("JSON object"))
        }
    }
}
