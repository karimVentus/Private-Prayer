package com.prayertime.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetDigitFormatterTest {
    @Test
    fun toEasternArabicDigits_mapsWesternDigits() {
        assertEquals("١٧:٤٣", WidgetDigitFormatter.toEasternArabicDigits("17:43"))
        assertEquals("١٠س ٢٧د", WidgetDigitFormatter.toEasternArabicDigits("10س 27د"))
    }
}
