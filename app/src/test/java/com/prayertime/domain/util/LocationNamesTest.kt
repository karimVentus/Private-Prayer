package com.prayertime.domain.util

import com.prayertime.domain.model.Country
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationNamesTest {
    private val germany = Country(name = "Germany", code = "DE")

    @Test
    fun countryDisplay_english_uses_catalog_name() {
        assertEquals("Germany", LocationNames.countryDisplay(germany, "en"))
    }

    @Test
    fun countryDisplay_arabic_uses_locale_display_country() {
        val display = LocationNames.countryDisplay(germany, "ar")
        assertTrue(display.contains("مانيا") || display.contains("ألمانيا"))
    }

    @Test
    fun cityDisplay_arabic_prefers_arabic_name() {
        assertEquals("دمشق", LocationNames.cityDisplay("Damascus", "دمشق", "ar"))
    }

    @Test
    fun matchesQuery_finds_arabic_country_name() {
        val display = LocationNames.countryDisplay(germany, "ar")
        assertTrue(LocationNames.matchesQuery(display, germany.name, "ألمان"))
    }

    @Test
    fun formatCityHeader_arabic_uses_arabic_comma_and_translations() {
        val header =
            LocationNames.formatCityHeader(
                cityName = "Hameln",
                country = germany,
                cityArabic = "هاملن",
                languageTag = "ar",
            )
        assertTrue(header.contains("هاملن"))
        assertTrue(header.contains("،"))
    }
}
