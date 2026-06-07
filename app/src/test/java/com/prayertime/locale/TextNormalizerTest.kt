package com.prayertime.locale

import org.junit.Assert.assertEquals
import org.junit.Test

class TextNormalizerTest {
    @Test
    fun foldForLookup_strips_diacritics_and_lowercases() {
        assertEquals("osnabruck", TextNormalizer.foldForLookup("Osnabrück"))
        assertEquals("osnabruck", TextNormalizer.foldForLookup("osnabruck"))
    }

    // --- Arabic location search: fold tashkeel + common letter variants ---

    @Test
    fun `Arabic text folds ta marbuta and alef variants for lookup`() {
        assertEquals("مكه", TextNormalizer.foldForLookup("مكة"))
        assertEquals("القاهره", TextNormalizer.foldForLookup("القاهرة"))
        assertEquals("دمشق", TextNormalizer.foldForLookup("دمشق"))
    }

    @Test
    fun `Arabic text with diacritics folds to same form as plain text`() {
        val withTashkeel = "مَكَّةُ"
        assertEquals(TextNormalizer.foldForLookup("مكة"), TextNormalizer.foldForLookup(withTashkeel))
    }

    @Test
    fun `Latin and Arabic script folds stay in separate buckets`() {
        val arabicQuery = TextNormalizer.foldForLookup("مكة")
        val latinEntry = TextNormalizer.foldForLookup("Mecca")
        assertEquals("مكه", arabicQuery)
        assertEquals("mecca", latinEntry)
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
