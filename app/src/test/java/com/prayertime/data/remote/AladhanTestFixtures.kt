package com.prayertime.data.remote

import com.google.gson.Gson

/** Mirrors [AladhanApi] response extraction for fixture-driven tests. */
object AladhanTestFixtures {
    private val gson = Gson()

    fun parseFixture(json: String): PrayerApiResponse? {
        val response = gson.fromJson(json, AladhanResponse::class.java)
        return toPrayerApiResponse(response)
    }

    fun loadResource(path: String): PrayerApiResponse? {
        return parseFixture(loadResourceRaw(path))
    }

    fun loadResourceRaw(path: String): String {
        val stream =
            checkNotNull(javaClass.classLoader?.getResourceAsStream(path)) {
                "Missing test resource: $path"
            }
        return stream.bufferedReader().readText()
    }

    fun toPrayerApiResponse(response: AladhanResponse): PrayerApiResponse? {
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
}
