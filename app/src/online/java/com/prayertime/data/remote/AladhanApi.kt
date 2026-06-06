package com.prayertime.data.remote

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AladhanApi
    @Inject
    constructor(
        private val service: AladhanApiService,
    ) : PrayerApi {
        override suspend fun getTimingsByCity(
            city: String,
            country: String,
        ): PrayerApiResponse? {
            val response = service.getTimingsByCity(city, country)
            val timings = response.data?.timings ?: return null
            val date = response.data?.date?.gregorian?.date ?: return null
            val timezone = response.data?.meta?.timezone ?: ""
            val timingsMap =
                mapOf(
                    "Fajr" to (timings.fajr ?: return null),
                    "Sunrise" to (timings.sunrise ?: return null),
                    "Dhuhr" to (timings.dhuhr ?: return null),
                    "Asr" to (timings.asr ?: return null),
                    "Maghrib" to (timings.maghrib ?: return null),
                    "Isha" to (timings.isha ?: return null),
                )
            return PrayerApiResponse(timingsMap, date, timezone)
        }

        override suspend fun getTimingsWithGeocode(
            city: String,
            country: String,
        ): TimingsWithGeocode? {
            val response = service.getTimingsByCity(city, country)
            val timings = response.data?.timings ?: return null
            val date = response.data?.date?.gregorian?.date ?: return null
            val meta = response.data?.meta ?: return null
            val lat = meta.latitude ?: return null
            val lon = meta.longitude ?: return null
            val timingsMap =
                mapOf(
                    "Fajr" to (timings.fajr ?: return null),
                    "Sunrise" to (timings.sunrise ?: return null),
                    "Dhuhr" to (timings.dhuhr ?: return null),
                    "Asr" to (timings.asr ?: return null),
                    "Maghrib" to (timings.maghrib ?: return null),
                    "Isha" to (timings.isha ?: return null),
                )
            return TimingsWithGeocode(
                timingsMap = timingsMap,
                date = date,
                timezone = meta.timezone ?: "",
                latitude = lat,
                longitude = lon,
            )
        }
    }
