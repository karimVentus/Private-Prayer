package com.prayertime.data.remote

import com.prayertime.domain.model.FetchError
import retrofit2.HttpException
import java.net.UnknownHostException

object NetworkMapper {
    fun mapError(throwable: Throwable): FetchError {
        return when (throwable) {
            is UnknownHostException -> FetchError.NETWORK
            is java.io.IOException -> FetchError.NETWORK
            is HttpException ->
                when (throwable.code()) {
                    404 -> FetchError.CITY_NOT_FOUND
                    400 -> FetchError.CITY_NOT_FOUND
                    else -> FetchError.UNKNOWN
                }
            is com.google.gson.JsonSyntaxException -> FetchError.INVALID_RESPONSE
            else -> FetchError.UNKNOWN
        }
    }
}
