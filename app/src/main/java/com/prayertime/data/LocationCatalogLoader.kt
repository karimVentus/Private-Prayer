package com.prayertime.data

import com.prayertime.domain.model.CityCoords
import com.prayertime.domain.model.Country
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

internal object LocationCatalogLoader {
    private const val ASSET_NAME = "locations.json"
    private const val CITIES_AR_ASSET = "cities_ar.json"

    class InvalidCatalogException(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)

    fun loadFromAssets(assets: android.content.res.AssetManager): LocationCatalog {
        val catalog = assets.open(ASSET_NAME).use(::load)
        val citiesAr = loadCitiesArFromAssets(assets)
        return catalog.copy(citiesArByCountry = citiesAr)
    }

    fun load(input: InputStream): LocationCatalog = parse(input.bufferedReader().readText())

    fun parse(
        json: String,
        citiesArByCountry: Map<String, Map<String, String>> = emptyMap(),
    ): LocationCatalog {
        val root =
            try {
                JSONObject(json)
            } catch (e: Exception) {
                throw InvalidCatalogException("Location catalog root must be a JSON object", e)
            }
        val countries = parseCountries(requireArray(root, "countries"))
        val citiesByCountry =
            requireObject(root, "citiesByCountry").toStringListMap().mapValues { (_, cities) ->
                cities.distinct()
            }
        val countryDefaults = requireObject(root, "countryDefaults").toCityCoordsMap()
        val knownCityCoords = requireObject(root, "knownCityCoords").toCityCoordsMap()
        return LocationCatalog(
            countries = countries,
            citiesByCountry = citiesByCountry,
            citiesArByCountry = citiesArByCountry,
            countryDefaults = countryDefaults,
            knownCityCoords = knownCityCoords,
        )
    }

    fun parseCitiesAr(json: String): Map<String, Map<String, String>> {
        val root =
            try {
                JSONObject(json)
            } catch (e: Exception) {
                throw InvalidCatalogException("Arabic city catalog root must be a JSON object", e)
            }
        val result = linkedMapOf<String, Map<String, String>>()
        root.keys().forEach { countryCode ->
            val countryObj =
                try {
                    root.getJSONObject(countryCode)
                } catch (e: Exception) {
                    throw InvalidCatalogException("cities_ar['$countryCode'] must be an object", e)
                }
            val cityMap = linkedMapOf<String, String>()
            countryObj.keys().forEach { englishName ->
                try {
                    cityMap[englishName] = countryObj.getString(englishName)
                } catch (e: Exception) {
                    throw InvalidCatalogException(
                        "cities_ar['$countryCode']['$englishName'] must be a string",
                        e,
                    )
                }
            }
            result[countryCode] = cityMap
        }
        return result
    }

    private fun loadCitiesArFromAssets(assets: android.content.res.AssetManager): Map<String, Map<String, String>> =
        try {
            assets.open(CITIES_AR_ASSET).use { stream ->
                parseCitiesAr(stream.bufferedReader().readText())
            }
        } catch (_: Exception) {
            emptyMap()
        }

    private fun requireObject(
        root: JSONObject,
        key: String,
    ): JSONObject {
        if (!root.has(key)) throw InvalidCatalogException("Missing required key '$key'")
        return try {
            root.getJSONObject(key)
        } catch (e: Exception) {
            throw InvalidCatalogException("'$key' must be a JSON object", e)
        }
    }

    private fun requireArray(
        root: JSONObject,
        key: String,
    ): JSONArray {
        if (!root.has(key)) throw InvalidCatalogException("Missing required key '$key'")
        return try {
            root.getJSONArray(key)
        } catch (e: Exception) {
            throw InvalidCatalogException("'$key' must be a JSON array", e)
        }
    }

    private fun parseCountries(array: JSONArray): List<Country> =
        buildList {
            for (i in 0 until array.length()) {
                val item =
                    try {
                        array.getJSONObject(i)
                    } catch (e: Exception) {
                        throw InvalidCatalogException("countries[$i] must be an object", e)
                    }
                try {
                    add(Country(name = item.getString("name"), code = item.getString("code")))
                } catch (e: Exception) {
                    throw InvalidCatalogException("countries[$i] missing 'name' or 'code'", e)
                }
            }
        }.distinctBy { "${it.code}\u0000${it.name}" }

    private fun JSONObject.toStringListMap(): Map<String, List<String>> {
        val result = linkedMapOf<String, List<String>>()
        keys().forEach { key ->
            val array =
                try {
                    getJSONArray(key)
                } catch (e: Exception) {
                    throw InvalidCatalogException("citiesByCountry['$key'] must be an array", e)
                }
            result[key] =
                buildList {
                    for (i in 0 until array.length()) {
                        try {
                            add(array.getString(i))
                        } catch (e: Exception) {
                            throw InvalidCatalogException("citiesByCountry['$key'][$i] must be a string", e)
                        }
                    }
                }
        }
        return result
    }

    private fun JSONObject.toCityCoordsMap(): Map<String, CityCoords> {
        val result = linkedMapOf<String, CityCoords>()
        keys().forEach { key ->
            val item =
                try {
                    getJSONObject(key)
                } catch (e: Exception) {
                    throw InvalidCatalogException("coords['$key'] must be an object", e)
                }
            try {
                result[key] =
                    CityCoords(
                        latitude = item.getDouble("latitude"),
                        longitude = item.getDouble("longitude"),
                        timezone = item.getString("timezone"),
                    )
            } catch (e: Exception) {
                throw InvalidCatalogException("coords['$key'] missing lat/lng/timezone", e)
            }
        }
        return result
    }
}
