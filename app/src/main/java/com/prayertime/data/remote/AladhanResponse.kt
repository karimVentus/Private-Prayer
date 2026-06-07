package com.prayertime.data.remote

import com.google.gson.annotations.SerializedName

data class AladhanResponse(
    @SerializedName("data")
    val data: TimingData?,
)

data class TimingData(
    @SerializedName("timings")
    val timings: Timings?,
    @SerializedName("date")
    val date: DateInfo?,
    @SerializedName("meta")
    val meta: Meta?,
)

data class Timings(
    @SerializedName("Fajr")
    val fajr: String?,
    @SerializedName("Sunrise")
    val sunrise: String?,
    @SerializedName("Dhuhr")
    val dhuhr: String?,
    @SerializedName("Asr")
    val asr: String?,
    @SerializedName("Maghrib")
    val maghrib: String?,
    @SerializedName("Isha")
    val isha: String?,
)

data class DateInfo(
    @SerializedName("readable")
    val readable: String?,
    @SerializedName("timestamp")
    val timestamp: String?,
    @SerializedName("gregorian")
    val gregorian: GregorianDate?,
)

data class GregorianDate(
    @SerializedName("date")
    val date: String?,
)

data class Meta(
    @SerializedName("timezone")
    val timezone: String?,
    @SerializedName("latitude")
    val latitude: Double?,
    @SerializedName("longitude")
    val longitude: Double?,
)
