package com.prayertime.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface AladhanApiService {
    @GET("v1/timingsByCity")
    suspend fun getTimingsByCity(
        @Query("city") city: String,
        @Query("country") country: String,
        @Query("method") method: Int = 4,
        @Query("school") school: Int = 0,
        @Query("latitudeAdjustmentMethod") latitudeAdjustmentMethod: Int = 3,
    ): AladhanResponse
}
