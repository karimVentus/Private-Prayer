package com.prayertime.ui

import androidx.annotation.StringRes
import com.prayertime.R
import com.prayertime.domain.model.FetchError
import com.prayertime.domain.model.SaveCityError

object PrayerTimesErrorMapper {
    @StringRes
    fun saveError(
        type: SaveCityError,
        offlineOnly: Boolean,
    ): Int =
        when (type) {
            SaveCityError.NETWORK ->
                if (offlineOnly) {
                    R.string.error_save_network_offline
                } else {
                    R.string.error_save_network_online
                }
            SaveCityError.CITY_NOT_FOUND -> R.string.error_save_city_not_found
            SaveCityError.INVALID_COUNTRY -> R.string.error_save_invalid_response
            SaveCityError.INVALID_COORDINATES -> R.string.error_save_invalid_coordinates
            SaveCityError.UNKNOWN -> R.string.error_save_unknown
        }

    @StringRes
    fun fetchError(
        type: FetchError,
        offlineOnly: Boolean,
    ): Int =
        when (type) {
            FetchError.NETWORK ->
                if (offlineOnly) {
                    R.string.error_fetch_network_offline
                } else {
                    R.string.error_fetch_network_online
                }
            FetchError.CITY_NOT_FOUND -> R.string.error_fetch_city_not_found
            FetchError.INVALID_RESPONSE -> R.string.error_fetch_invalid_response
            FetchError.UNKNOWN -> R.string.error_fetch_unknown
            FetchError.MISSING_COORDINATES -> R.string.error_missing_coords
        }
}
