package com.prayertime.data.remote

interface PrayerApi {
    suspend fun getTimingsByCity(
        city: String,
        country: String,
    ): PrayerApiResponse?

    /** Single call that returns both timings and geocode — avoids a second [getTimingsByCity] round-trip. */
    suspend fun getTimingsWithGeocode(
        city: String,
        country: String,
    ): TimingsWithGeocode?
}

data class PrayerApiResponse(
    val timingsMap: Map<String, String>,
    val date: String,
    val timezone: String,
)

data class TimingsWithGeocode(
    val timingsMap: Map<String, String>,
    val date: String,
    val timezone: String,
    val latitude: Double,
    val longitude: Double,
)
