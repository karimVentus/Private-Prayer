package com.prayertime.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

/** Production-shaped Retrofit client for opt-in live [AladhanApi] tests. */
internal object LiveAladhanTestSupport {
    private const val BASE_URL = "https://api.aladhan.com/"
    private const val HOST = "api.aladhan.com"
    private const val CONNECT_TIMEOUT_SEC = 15L
    private const val READ_TIMEOUT_SEC = 20L

    /**
     * Live HTTP tests are opt-in: set env `PRAYERTIME_LIVE_HTTP=1` (or `true`).
     * When unset, tests skip (default for CI / air-gapped runs).
     */
    fun assumeLiveApiReachable() {
        org.junit.Assume.assumeTrue(
            "Live Aladhan HTTP disabled — set PRAYERTIME_LIVE_HTTP=1 to enable",
            isLiveHttpEnabled(),
        )
        org.junit.Assume.assumeTrue(
            "api.aladhan.com unreachable — skipping live HTTP integration test",
            canReachAladhan(),
        )
    }

    fun createApi(): AladhanApi {
        val client =
            OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SEC, TimeUnit.SECONDS)
                .writeTimeout(READ_TIMEOUT_SEC, TimeUnit.SECONDS)
                .build()
        val retrofit =
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        return AladhanApi(retrofit.create(AladhanApiService::class.java))
    }

    internal fun isLiveHttpEnabled(envValue: String? = System.getenv("PRAYERTIME_LIVE_HTTP")): Boolean {
        envValue ?: return false
        return envValue == "1" || envValue.equals("true", ignoreCase = true)
    }

    private fun canReachAladhan(): Boolean =
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(HOST, 443), 5_000)
            }
            true
        } catch (_: Exception) {
            false
        }
}
