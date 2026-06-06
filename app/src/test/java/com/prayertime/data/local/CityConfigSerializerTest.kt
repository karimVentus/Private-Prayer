package com.prayertime.data.local

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
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
}
