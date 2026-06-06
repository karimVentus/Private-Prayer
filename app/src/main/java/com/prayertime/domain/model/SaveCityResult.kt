package com.prayertime.domain.model

sealed class SaveCityResult {
    data class Success(val config: CityConfig) : SaveCityResult()

    data class Error(val type: SaveCityError) : SaveCityResult()
}
