package com.prayertime.ui

import com.prayertime.R
import com.prayertime.domain.model.FetchError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PrayerTimesErrorMapperTest {

    @Test
    fun `NETWORK maps to error_fetch_network_offline when offlineOnly is true`() {
        val res = PrayerTimesErrorMapper.fetchError(FetchError.NETWORK, offlineOnly = true)
        assertEquals(R.string.error_fetch_network_offline, res)
    }

    @Test
    fun `NETWORK maps to error_fetch_network_online when offlineOnly is false`() {
        val res = PrayerTimesErrorMapper.fetchError(FetchError.NETWORK, offlineOnly = false)
        assertEquals(R.string.error_fetch_network_online, res)
    }

    @Test
    fun `CITY_NOT_FOUND maps differently depending on offlineOnly`() {
        val offlineRes = PrayerTimesErrorMapper.fetchError(FetchError.CITY_NOT_FOUND, offlineOnly = true)
        val onlineRes = PrayerTimesErrorMapper.fetchError(FetchError.CITY_NOT_FOUND, offlineOnly = false)
        assertEquals(offlineRes, onlineRes)
        assertEquals(R.string.error_fetch_city_not_found, offlineRes)
    }

    @Test
    fun `INVALID_RESPONSE maps to error_fetch_invalid_response`() {
        val res = PrayerTimesErrorMapper.fetchError(FetchError.INVALID_RESPONSE, offlineOnly = false)
        assertEquals(R.string.error_fetch_invalid_response, res)
    }

    @Test
    fun `UNKNOWN maps to error_fetch_unknown`() {
        val res = PrayerTimesErrorMapper.fetchError(FetchError.UNKNOWN, offlineOnly = false)
        assertEquals(R.string.error_fetch_unknown, res)
    }

    @Test
    fun `MISSING_COORDINATES maps to error_missing_coords`() {
        val res = PrayerTimesErrorMapper.fetchError(FetchError.MISSING_COORDINATES, offlineOnly = false)
        assertEquals(R.string.error_missing_coords, res)
    }
}
