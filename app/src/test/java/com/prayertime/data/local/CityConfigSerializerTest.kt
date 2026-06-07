package com.prayertime.data.local

import androidx.test.core.app.ApplicationProvider
import com.prayertime.domain.model.CityConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CityConfigSerializerTest {
    private fun serializer() = CityConfigSerializer(ApplicationProvider.getApplicationContext())

    @Test
    fun `offlineOnly defaults to true`() =
        runTest {
            assertTrue(serializer().offlineOnly.first())
        }

    @Test
    fun `offlineOnly persists false then true`() =
        runTest {
            val s = serializer()
            s.setOfflineOnly(false)
            assertFalse(s.offlineOnly.first())
            s.setOfflineOnly(true)
            assertTrue(s.offlineOnly.first())
        }

    @Test
    fun `missing latitude becomes null after save`() =
        runTest {
            val s = serializer()
            val config = CityConfig("Hamburg", "DE", "Europe/Berlin", latitude = null, longitude = 10.0)
            s.save(config)
            val saved = s.cityConfig.first()
            assertNotNull(saved)
            assertNull(saved!!.latitude)
        }

    @Test
    fun `missing longitude becomes null after save`() =
        runTest {
            val s = serializer()
            val config = CityConfig("Munich", "DE", "Europe/Berlin", latitude = 48.0, longitude = null)
            s.save(config)
            val saved = s.cityConfig.first()
            assertNotNull(saved)
            assertNull(saved!!.longitude)
        }

    @Test
    fun `resetCityStore clears city and privacy mode`() =
        runTest {
            val s = serializer()
            s.save(CityConfig("Berlin", "DE", "Europe/Berlin", 52.52, 13.405))
            s.setOfflineOnly(false)

            // Verify preconditions
            assertNotNull(s.cityConfig.first())
            assertFalse(s.offlineOnly.first())

            s.resetCityStore()

            assertNull(s.cityConfig.first())
            // offlineOnly defaults to true after clear
            assertTrue(s.offlineOnly.first())
        }

    @Test
    fun `sentinel 0,0 coordinates are treated as null`() =
        runTest {
            val s = serializer()
            val config = CityConfig("TestCity", "XX", "UTC", latitude = 0.0, longitude = 0.0)
            s.save(config)
            val saved = s.cityConfig.first()
            assertNotNull(saved)
            assertNull(saved!!.latitude)
            assertNull(saved.longitude)
        }
}
