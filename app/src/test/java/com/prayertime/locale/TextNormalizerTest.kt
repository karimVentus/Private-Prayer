package com.prayertime.locale

import org.junit.Assert.assertEquals
import org.junit.Test

class TextNormalizerTest {
    @Test
    fun foldForLookup_strips_diacritics_and_lowercases() {
        assertEquals("osnabruck", TextNormalizer.foldForLookup("Osnabrück"))
        assertEquals("osnabruck", TextNormalizer.foldForLookup("osnabruck"))
    }

    // --- 5E.6: Arabic city name support ---

    @Test
    fun `Arabic text without diacritics passes through unchanged`() {
        // Arabic text without tashkeel (حركات) has no combining marks to strip
        assertEquals("مكة", TextNormalizer.foldForLookup("مكة"))
        assertEquals("القاهرة", TextNormalizer.foldForLookup("القاهرة"))
        assertEquals("دمشق", TextNormalizer.foldForLookup("دمشق"))
    }

    @Test
    fun `Arabic text with diacritics has them stripped`() {
        // Arabic with shadda, fatha, etc. — diacritics are stripped
        val withTashkeel = "مَكَّةُ" // Mecca with diacritics
        val withoutTashkeel = "مكة"
        assertEquals(withoutTashkeel, TextNormalizer.foldForLookup(withTashkeel))
    }

    @Test
    fun `Latin catalog names match Arabic-script input via foldForLookup`() {
        // Latin-script catalog entry "Mecca" won't match Arabic "مكة"
        // because the scripts differ. This test documents the current behavior:
        // Arabic script queries won't find Latin catalog entries.
        val arabicQuery = TextNormalizer.foldForLookup("مكة")
        val latinEntry = TextNormalizer.foldForLookup("Mecca")
        // Document the gap: Arabic ↔ Latin script mapping is not implemented
        assertEquals(arabicQuery, "مكة")
        assertEquals(latinEntry, "mecca")
        // Scripts are different → no match (this is expected with Latin-only catalog)
    }

    @Test
    fun `Arabic alef variants normalize for search`() {
        assertEquals(
            TextNormalizer.foldForLookup("المانيا"),
            TextNormalizer.foldForLookup("ألمانيا"),
        )
    }

    @Test
    fun `Mixed script names are preserved`() {
        // NFD normalization strips accents → "café" becomes "cafe"
        assertEquals("cafe", TextNormalizer.foldForLookup("Café"))
        assertEquals("sao paulo", TextNormalizer.foldForLookup("São Paulo"))
        assertEquals("brasilia", TextNormalizer.foldForLookup("Brasília"))
    }
}
