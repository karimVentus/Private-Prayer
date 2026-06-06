package com.prayertime.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.prayertime.data.local.AppDatabase
import com.prayertime.data.local.CityConfigSerializer
import com.prayertime.domain.model.CityConfig
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PrayerTimeCacheTest {
    private lateinit var database: AppDatabase
    private lateinit var cityConfigSerializer: CityConfigSerializer

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        cityConfigSerializer = CityConfigSerializer(context)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun fetch_twice_same_day_keeps_six_cached_rows() =
        runTest {
            val repo = LocalPrayerTimesRepository(cityConfigSerializer, database)
            val config =
                CityConfig(
                    cityName = "Hameln",
                    countryCode = "DE",
                    timezone = "Europe/Berlin",
                    latitude = 52.103,
                    longitude = 9.356,
                )
            repo.fetchTodayTimes(config)
            repo.fetchTodayTimes(config)

            val label =
                SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("Europe/Berlin")
                }.format(Date())
            assertEquals(6, database.prayerTimeDao().getByCityAndDate("DE_Hameln", label).size)
        }
}
