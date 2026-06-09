package com.prayertime.data

import com.prayertime.domain.model.CityResolutionResult
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LocationDataSourceTest {
    @Before
    fun setUp() {
        LocationDataSourceTestSupport.initializeFromTestResource()
    }

    @After
    fun tearDown() {
        LocationDataSource.resetForTests()
    }

    @Test
    fun countries_have_unique_iso_codes() {
        val codes = LocationDataSource.loadedCatalog()!!.countries.map { it.code }
        assertEquals(codes.size, codes.distinct().size)
    }

    @Test
    fun countries_have_unique_lazy_column_keys() {
        val keys = LocationDataSource.loadedCatalog()!!.countries.map { "${it.code}-${it.name}" }
        assertEquals(keys.size, keys.distinct().size)
    }

    @Test
    fun germany_includes_cities_from_50k_tier() {
        val cities = LocationDataSource.loadedCatalog()?.citiesByCountry["DE"] ?: emptyList()
        assertTrue(cities.contains("Hameln"))
        assertTrue(cities.size >= 80)
    }

    @Test
    fun known_city_coords_match_reference() {
        val hameln = LocationDataSource.resolveCityCoordinates("DE", "Hameln")
        assertTrue("Expected Found for known city", hameln is CityResolutionResult.Found)
        hameln as CityResolutionResult.Found
        assertEquals(52.103, hameln.coords.latitude, 0.01)
        assertEquals(9.356, hameln.coords.longitude, 0.01)
        assertEquals("Europe/Berlin", hameln.coords.timezone)

        val damascus = LocationDataSource.resolveCityCoordinates("SY", "Damascus")
        assertTrue("Expected Found for Damascus", damascus is CityResolutionResult.Found)
        damascus as CityResolutionResult.Found
        assertEquals(33.513, damascus.coords.latitude, 0.01)
        assertEquals(36.292, damascus.coords.longitude, 0.01)
        assertEquals("Asia/Damascus", damascus.coords.timezone)

        val mecca = LocationDataSource.resolveCityCoordinates("SA", "Mecca")
        assertTrue("Expected Found for Mecca", mecca is CityResolutionResult.Found)
        mecca as CityResolutionResult.Found
        assertEquals(21.422, mecca.coords.latitude, 0.01)
        assertEquals(39.826, mecca.coords.longitude, 0.01)
        assertEquals("Asia/Riyadh", mecca.coords.timezone)
    }

    @Test
    fun newly_added_german_city_resolves() {
        val result = LocationDataSource.resolveCityCoordinates("DE", "Flensburg")
        assertTrue("Expected Found for Flensburg", result is CityResolutionResult.Found)
        result as CityResolutionResult.Found
        assertEquals(54.785, result.coords.latitude, 0.01)
        assertEquals(9.437, result.coords.longitude, 0.01)
        assertEquals("Europe/Berlin", result.coords.timezone)
    }

    @Test
    fun unknown_city_returns_fallback() {
        val result = LocationDataSource.resolveCityCoordinates("DE", "NonExistentVillage")
        assertTrue("Expected Fallback for unknown city", result is CityResolutionResult.Fallback)
        result as CityResolutionResult.Fallback
        // Fallback uses country defaults
        assertEquals(51.5, result.coords.latitude, 0.5)
        assertEquals(10.5, result.coords.longitude, 0.5)
        assertEquals("Europe/Berlin", result.coords.timezone)
        assertEquals("NonExistentVillage", result.cityName)
        assertEquals("DE", result.countryCode)
    }

    @Test
    fun unsupported_country_returns_invalid() {
        val result = LocationDataSource.resolveCityCoordinates("XX", "Nowhere")
        assertTrue("Expected InvalidCountry for unsupported country", result is CityResolutionResult.InvalidCountry)
        result as CityResolutionResult.InvalidCountry
        assertEquals("XX", result.countryCode)
    }

    @Test
    fun german_picker_ascii_names_match_umlaut_coord_keys() {
        for (city in listOf("Osnabruck", "Saarbrucken")) {
            val result = LocationDataSource.resolveCityCoordinates("DE", city)
            assertTrue("$city should resolve to Found", result is CityResolutionResult.Found)
        }
    }

    @Test
    fun `german umlaut city name directly resolves to Found`() {
        for (city in listOf("Osnabrück", "Saarbrücken")) {
            val result = LocationDataSource.resolveCityCoordinates("DE", city)
            assertTrue("$city should resolve to Found", result is CityResolutionResult.Found)
        }
    }

    @Test
    fun `typo of known city returns Fallback not Found`() {
        for (typo in listOf("Osnabruc", "Saarbruken", "Hamburrg")) {
            val result = LocationDataSource.resolveCityCoordinates("DE", typo)
            assertTrue(
                "Typo '$typo' should resolve to Fallback, got $result",
                result is CityResolutionResult.Fallback,
            )
        }
    }

    @Test
    fun every_germany_picker_city_resolves_to_found() {
        assertEveryPickerCityResolvesToFound("DE")
    }

    @Test
    fun regional_picker_cities_have_bundled_coords() {
        val filledRegionCountryCodes =
            listOf(
                // Europe
                "AL", "AD", "AT", "BE", "BA", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR", "DE",
                "GR", "HU", "IS", "IE", "IT", "XK", "LV", "LT", "LU", "MT", "MD", "MC", "ME", "MK",
                "NL", "NO", "PL", "PT", "RO", "RS", "SK", "SI", "ES", "SE", "CH", "UA", "GB", "VA",
                // Africa
                "DJ", "DZ", "EG", "ET", "GH", "KE", "KM", "LY", "MA", "MR", "NG", "SD", "SL", "SN",
                "SO", "TN", "TZ", "ZA",
                // Asia
                "AF", "AM", "AZ", "BH", "BD", "BN", "KH", "CN", "GE", "IN", "ID", "IR", "IQ", "IL",
                "JO", "JP", "KZ", "KW", "KG", "LA", "LB", "MY", "MV", "MN", "MM", "NP", "OM", "PK",
                "PS", "PH", "QA", "SA", "SG", "KR", "LK", "SY", "TW", "TJ", "TH", "TL", "TR", "TM",
                "AE", "UZ", "VN", "YE",
                // Americas
                "AR", "BR", "CA", "CL", "CO", "PE", "US", "UY", "VE",
            )
        assertEveryPickerCityHasKnownCoords(filledRegionCountryCodes)
    }

    @Test
    fun americas_reference_city_coords_match() {
        val saoPaulo = LocationDataSource.resolveCityCoordinates("BR", "São Paulo")
        assertTrue(saoPaulo is CityResolutionResult.Found)
        saoPaulo as CityResolutionResult.Found
        assertEquals(-23.552, saoPaulo.coords.latitude, 0.1)
        assertEquals(-46.634, saoPaulo.coords.longitude, 0.1)
        assertEquals("America/Sao_Paulo", saoPaulo.coords.timezone)

        val toronto = LocationDataSource.resolveCityCoordinates("CA", "Toronto")
        assertTrue(toronto is CityResolutionResult.Found)
        toronto as CityResolutionResult.Found
        assertEquals(43.653, toronto.coords.latitude, 0.1)
        assertEquals(-79.383, toronto.coords.longitude, 0.1)
        assertEquals("America/Toronto", toronto.coords.timezone)
    }

    @Test
    fun asia_reference_city_coords_match() {
        val tokyo = LocationDataSource.resolveCityCoordinates("JP", "Tokyo")
        assertTrue(tokyo is CityResolutionResult.Found)
        tokyo as CityResolutionResult.Found
        assertEquals(35.694, tokyo.coords.latitude, 0.1)
        assertEquals(139.754, tokyo.coords.longitude, 0.1)
        assertEquals("Asia/Tokyo", tokyo.coords.timezone)

        val singapore = LocationDataSource.resolveCityCoordinates("SG", "Singapore")
        assertTrue(singapore is CityResolutionResult.Found)
        singapore as CityResolutionResult.Found
        assertEquals(1.357, singapore.coords.latitude, 0.1)
        assertEquals(103.819, singapore.coords.longitude, 0.1)
        assertEquals("Asia/Singapore", singapore.coords.timezone)
    }

    @Test
    fun africa_reference_city_coords_match() {
        val lagos = LocationDataSource.resolveCityCoordinates("NG", "Lagos")
        assertTrue(lagos is CityResolutionResult.Found)
        lagos as CityResolutionResult.Found
        assertEquals(6.455, lagos.coords.latitude, 0.1)
        assertEquals(3.394, lagos.coords.longitude, 0.1)
        assertEquals("Africa/Lagos", lagos.coords.timezone)

        val nairobi = LocationDataSource.resolveCityCoordinates("KE", "Nairobi")
        assertTrue(nairobi is CityResolutionResult.Found)
        nairobi as CityResolutionResult.Found
        assertEquals(-1.283, nairobi.coords.latitude, 0.1)
        assertEquals(36.817, nairobi.coords.longitude, 0.1)
        assertEquals("Africa/Nairobi", nairobi.coords.timezone)
    }

    @Test
    fun europe_reference_city_coords_match() {
        val paris = LocationDataSource.resolveCityCoordinates("FR", "Paris")
        assertTrue(paris is CityResolutionResult.Found)
        paris as CityResolutionResult.Found
        assertEquals(48.857, paris.coords.latitude, 0.05)
        assertEquals(2.352, paris.coords.longitude, 0.05)
        assertEquals("Europe/Paris", paris.coords.timezone)

        val amsterdam = LocationDataSource.resolveCityCoordinates("NL", "Amsterdam")
        assertTrue(amsterdam is CityResolutionResult.Found)
        amsterdam as CityResolutionResult.Found
        assertEquals(52.374, amsterdam.coords.latitude, 0.05)
        assertEquals(4.890, amsterdam.coords.longitude, 0.05)
        assertEquals("Europe/Amsterdam", amsterdam.coords.timezone)
    }

    private fun assertEveryPickerCityResolvesToFound(countryCode: String) {
        val cities = LocationDataSource.loadedCatalog()?.citiesByCountry[countryCode] ?: emptyList()
        val failures = mutableListOf<String>()
        for (city in cities) {
            when (LocationDataSource.resolveCityCoordinates(countryCode, city)) {
                is CityResolutionResult.Found -> {}
                else -> failures.add(city)
            }
        }
        assertTrue(
            "Picker cities without exact coords: ${failures.take(10)}${if (failures.size > 10) "..." else ""}",
            failures.isEmpty(),
        )
    }

    /** Fast catalog key check — avoids per-city resolveCityCoordinates on ~2000 picker rows in CI. */
    private fun assertEveryPickerCityHasKnownCoords(countryCodes: List<String>) {
        val catalog = LocationDataSource.loadedCatalog()!!
        val coords = catalog.knownCityCoords
        val failures = mutableListOf<String>()
        for (countryCode in countryCodes) {
            for (city in catalog.citiesByCountry[countryCode] ?: emptyList()) {
                if ("${countryCode}_$city" !in coords) {
                    failures.add("$countryCode:$city")
                }
            }
        }
        assertTrue(
            "Picker cities without bundled coords: ${failures.take(10)}${if (failures.size > 10) "..." else ""}",
            failures.isEmpty(),
        )
    }

    private fun namingMismatchKey(
        countryCode: String,
        city: String,
    ): String? {
        val key = "${countryCode}_$city"
        return if (
            !LocationDataSource.loadedCatalog()!!.knownCityCoords.containsKey(key) &&
            LocationDataSource.resolveCityCoordinates(countryCode, city) is CityResolutionResult.Found
        ) {
            key
        } else {
            null
        }
    }

    @Test
    fun every_picker_city_key_name_matches_known_coords_exactly_or_is_truly_unknown() {
        val mismatches =
            LocationDataSource.loadedCatalog()!!.citiesByCountry
                .flatMap { (cc, cities) -> cities.mapNotNull { namingMismatchKey(cc, it) } }
        assertTrue(
            "Picker cities that resolve via foldForLookup fallback only (key naming mismatch): ${mismatches.joinToString(", ")}",
            mismatches.isEmpty(),
        )
    }

    @Test
    fun germany_known_coords_keys_match_picker_no_orphans() {
        val picker = LocationDataSource.loadedCatalog()?.citiesByCountry["DE"] ?: emptyList()
        val pickerSet = picker.toSet()
        val orphanKeys =
            LocationDataSource.loadedCatalog()!!.knownCityCoords.keys
                .filter { it.startsWith("DE_") }
                .map { it.removePrefix("DE_") }
                .filter { it !in pickerSet }
        assertTrue(
            "knownCityCoords DE keys not in picker (dead data): $orphanKeys",
            orphanKeys.isEmpty(),
        )
    }

    @Test
    fun awaitReadyCompletesWhenAsyncLoadFails() {
        LocationDataSource.resetForTests()
        LocationDataSource.simulateFailedLoadForTests()
        runBlocking {
            withTimeout(5_000) {
                LocationDataSource.awaitReady()
            }
        }
        assertTrue(LocationDataSource.catalogLoadState() == LocationDataSource.CatalogLoadState.FAILED)
        assertFalse(
            LocationDataSource.catalogLoadState() == LocationDataSource.CatalogLoadState.READY &&
                LocationDataSource.loadedCatalog() != null,
        )
        assertTrue((LocationDataSource.loadedCatalog()?.countries ?: emptyList()).isEmpty())
    }

    @Test(expected = IllegalStateException::class)
    fun awaitReadyWithoutInitializeThrows() {
        LocationDataSource.resetForTests()
        runBlocking {
            LocationDataSource.awaitReady()
        }
    }
}
