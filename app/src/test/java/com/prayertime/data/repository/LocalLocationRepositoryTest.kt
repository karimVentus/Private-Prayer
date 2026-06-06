package com.prayertime.data.repository

import com.prayertime.data.LocationDataSource
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalLocationRepositoryTest {
    private val repository = LocalLocationRepository()

    @Before
    fun resetCatalog() {
        LocationDataSource.resetForTests()
    }

    @After
    fun tearDown() {
        LocationDataSource.resetForTests()
    }

    @Test
    fun countries_beforeInitialize_throws() {
        assertThrows(IllegalStateException::class.java) {
            repository.countries()
        }
    }

    @Test
    fun isCatalogLoaded_beforeInitialize_isFalse() {
        assertTrue(!repository.isCatalogLoaded())
    }
}
