package com.prayertime.data.repository

import com.prayertime.domain.model.CityConfig
import com.prayertime.domain.model.SaveCityError

sealed class GeocodeResult {
    data class Success(val config: CityConfig) : GeocodeResult()

    data class Error(val type: SaveCityError) : GeocodeResult()
}
