package com.prayertime.locale

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class AppLocaleTest {
    @Test
    fun `defaultTagFromSystem returns ar for Arabic device locale`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-SA"))
            assertEquals("ar", AppLocale.defaultTagFromSystem())
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun `defaultTagFromSystem returns en for non-Arabic device locale`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("de-DE"))
            assertEquals("en", AppLocale.defaultTagFromSystem())
        } finally {
            Locale.setDefault(previous)
        }
    }
}
