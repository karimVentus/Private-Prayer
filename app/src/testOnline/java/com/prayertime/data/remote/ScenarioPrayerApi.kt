package com.prayertime.data.remote

/**
 * Test double that returns city-specific [PrayerApiResponse] payloads instead of one hardcoded
 * timing set for every request.
 */
class ScenarioPrayerApi(
    private val scenarios: Map<String, PrayerApiResponse> = defaultScenarios(),
) : PrayerApi {
    data class Request(
        val city: String,
        val country: String,
    )

    val timingsCalls = mutableListOf<Request>()
    val timingsWithGeocodeCalls = mutableListOf<Request>()
    var geocodeFailure: Throwable? = null
    var timingsFailure: Throwable? = null
    var forceNullTimings: Boolean = false

    override suspend fun getTimingsByCity(
        city: String,
        country: String,
    ): PrayerApiResponse? {
        timingsFailure?.let { throw it }
        timingsCalls += Request(city, country)
        if (forceNullTimings) return null
        return scenarios[key(city, country)]
    }

    override suspend fun getTimingsWithGeocode(
        city: String,
        country: String,
    ): TimingsWithGeocode? {
        geocodeFailure?.let { throw it }
        timingsFailure?.let { throw it }
        timingsWithGeocodeCalls += Request(city, country)
        if (forceNullTimings) return null
        val response = scenarios[key(city, country)] ?: return null
        val lat = scenarioLatitudes[key(city, country)] ?: return null
        val lon = scenarioLongitudes[key(city, country)] ?: return null
        return TimingsWithGeocode(
            timingsMap = response.timingsMap,
            date = response.date,
            timezone = response.timezone,
            latitude = lat,
            longitude = lon,
        )
    }

    companion object {
        private fun key(
            city: String,
            country: String,
        ) = "${city.lowercase()}|${country.uppercase()}"

        private val scenarioLatitudes =
            mapOf(
                key("Mecca", "SA") to 21.4225,
                key("Berlin", "DE") to 52.52,
            )

        private val scenarioLongitudes =
            mapOf(
                key("Mecca", "SA") to 39.8262,
                key("Berlin", "DE") to 13.405,
            )

        fun meccaJune2024(): PrayerApiResponse =
            PrayerApiResponse(
                timingsMap =
                    mapOf(
                        "Fajr" to "04:02 (AST)",
                        "Sunrise" to "05:23 (AST)",
                        "Dhuhr" to "12:15 (AST)",
                        "Asr" to "15:42 (AST)",
                        "Maghrib" to "18:47 (AST)",
                        "Isha" to "20:17 (AST)",
                    ),
                date = "03-06-2024",
                timezone = "Asia/Riyadh",
            )

        fun berlinJune2024(): PrayerApiResponse =
            PrayerApiResponse(
                timingsMap =
                    mapOf(
                        "Fajr" to "03:18 (CEST)",
                        "Sunrise" to "05:12 (CEST)",
                        "Dhuhr" to "13:24 (CEST)",
                        "Asr" to "17:18 (CEST)",
                        "Maghrib" to "21:02 (CEST)",
                        "Isha" to "22:48 (CEST)",
                    ),
                date = "03-06-2024",
                timezone = "Europe/Berlin",
            )

        fun incomplete(): PrayerApiResponse =
            PrayerApiResponse(
                timingsMap =
                    mapOf(
                        "Fajr" to "04:02 (AST)",
                        "Dhuhr" to "12:15 (AST)",
                        "Isha" to "20:17 (AST)",
                    ),
                date = "03-06-2024",
                timezone = "Asia/Riyadh",
            )

        /** Legacy FakePrayerApi-style flat response — used to prove tests reject it. */
        fun legacyFlatMock(): PrayerApiResponse =
            PrayerApiResponse(
                timingsMap =
                    mapOf(
                        "Fajr" to "05:12",
                        "Sunrise" to "06:30",
                        "Dhuhr" to "12:30",
                        "Asr" to "15:45",
                        "Maghrib" to "18:10",
                        "Isha" to "19:40",
                    ),
                date = "03-06-2024",
                timezone = "UTC",
            )

        fun defaultScenarios(): Map<String, PrayerApiResponse> =
            mapOf(
                key("Mecca", "SA") to meccaJune2024(),
                key("Berlin", "DE") to berlinJune2024(),
            )
    }
}
