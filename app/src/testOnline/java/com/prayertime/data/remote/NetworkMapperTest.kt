package com.prayertime.data.remote

import com.google.gson.JsonSyntaxException
import com.prayertime.domain.model.FetchError
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.UnknownHostException

class NetworkMapperTest {
    @Test
    fun `UnknownHostException maps to NETWORK`() {
        assertEquals(FetchError.NETWORK, NetworkMapper.mapError(UnknownHostException("no internet")))
    }

    @Test
    fun `IOException maps to NETWORK`() {
        assertEquals(FetchError.NETWORK, NetworkMapper.mapError(IOException("timeout")))
    }

    @Test
    fun `HttpException 404 maps to CITY_NOT_FOUND`() {
        val response = Response.error<String>(404, "Not Found".toResponseBody())
        assertEquals(FetchError.CITY_NOT_FOUND, NetworkMapper.mapError(HttpException(response)))
    }

    @Test
    fun `HttpException 400 maps to CITY_NOT_FOUND`() {
        val response = Response.error<String>(400, "Bad Request".toResponseBody())
        assertEquals(FetchError.CITY_NOT_FOUND, NetworkMapper.mapError(HttpException(response)))
    }

    @Test
    fun `HttpException 500 maps to UNKNOWN`() {
        val response = Response.error<String>(500, "Server Error".toResponseBody())
        assertEquals(FetchError.UNKNOWN, NetworkMapper.mapError(HttpException(response)))
    }

    @Test
    fun `HttpException 503 maps to UNKNOWN`() {
        val response = Response.error<String>(503, "Service Unavailable".toResponseBody())
        assertEquals(FetchError.UNKNOWN, NetworkMapper.mapError(HttpException(response)))
    }

    @Test
    fun `JsonSyntaxException maps to INVALID_RESPONSE`() {
        assertEquals(
            FetchError.INVALID_RESPONSE,
            NetworkMapper.mapError(JsonSyntaxException("malformed json")),
        )
    }

    @Test
    fun `RuntimeException maps to UNKNOWN`() {
        assertEquals(FetchError.UNKNOWN, NetworkMapper.mapError(RuntimeException("something broke")))
    }

    @Test
    fun `NullPointerException maps to UNKNOWN`() {
        assertEquals(FetchError.UNKNOWN, NetworkMapper.mapError(NullPointerException("null!")))
    }

    @Test
    fun `IllegalStateException maps to UNKNOWN`() {
        assertEquals(FetchError.UNKNOWN, NetworkMapper.mapError(IllegalStateException("bad state")))
    }
}
