package com.prayertime.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.prayertime.domain.model.HijriDate
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HijriDateFormatterTest {

    private val resources = ApplicationProvider.getApplicationContext<Context>().resources

    @Test
    fun `format returns non empty string for known date`() {
        val date = HijriDate(year = 1447, month = 9, day = 1)
        val result = HijriDateFormatter.format(date, resources)
        assertNotNull(result)
        assertTrue(result.isNotBlank())
    }

    @Test
    fun `format ramadan 1 1447 contains day month and year`() {
        val date = HijriDate(year = 1447, month = 9, day = 1)
        val result = HijriDateFormatter.format(date, resources)
        assertTrue(result.contains("1"))
        assertTrue(result.contains("1447"))
        assertTrue(result.contains("Ramadan", ignoreCase = true))
    }
}
