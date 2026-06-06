package com.prayertime.domain.model

data class CityConfig(
    val cityName: String,
    val countryCode: String,
    val timezone: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
) {
    val hasValidCoordinates: Boolean
        get() = latitude != null && longitude != null
}
