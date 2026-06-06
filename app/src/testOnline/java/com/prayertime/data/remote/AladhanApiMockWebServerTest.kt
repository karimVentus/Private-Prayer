package com.prayertime.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Retrofit + Gson over [MockWebServer] — validates HTTP wire path for [AladhanApi]. */
class AladhanApiMockWebServerTest {
    private lateinit var server: MockWebServer
    private lateinit var api: AladhanApi

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        val retrofit =
            Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        api = AladhanApi(retrofit.create(AladhanApiService::class.java))
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun getTimingsByCity_parsesMeccaFixture() =
        runBlocking {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(AladhanTestFixtures.loadResourceRaw("aladhan/mecca_2024-06-03.json")),
            )

            val result = api.getTimingsByCity("Mecca", "SA")

            assertNotNull(result)
            assertEquals("03-06-2024", result!!.date)
            assertEquals("Asia/Riyadh", result.timezone)
            assertEquals("04:02 (AST)", result.timingsMap["Fajr"])

            val request = server.takeRequest()
            assertTrue(request.path!!.contains("timingsByCity"))
            assertTrue(request.path!!.contains("city=Mecca"))
            assertTrue(request.path!!.contains("country=SA"))
            assertTrue(request.path!!.contains("method=4"))
        }

    @Test
    fun getTimingsWithGeocode_returnsCoordinatesFromMeta() =
        runBlocking {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(AladhanTestFixtures.loadResourceRaw("aladhan/mecca_2024-06-03.json")),
            )

            val result = api.getTimingsWithGeocode("Mecca", "SA")

            assertNotNull(result)
            assertEquals(21.4225, result!!.latitude, 0.0001)
            assertEquals(39.8262, result.longitude, 0.0001)
            assertEquals("Asia/Riyadh", result.timezone)
        }

    @Test
    fun getTimingsByCity_http500_propagatesHttpException() =
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(500).setBody("{}"))
            try {
                api.getTimingsByCity("Mecca", "SA")
                fail("Expected HttpException for HTTP 500")
            } catch (e: HttpException) {
                assertEquals(500, e.code())
            }
        }
}
