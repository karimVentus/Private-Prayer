package com.prayertime.locale

import java.text.Normalizer
import java.util.Locale

object TextNormalizer {
    private val arabicScript = Regex("[\u0600-\u06FF]")

    /** Case/diacritic-insensitive folding; Arabic alef/ya variants normalized for search. */
    fun foldForLookup(text: String): String {
        val stripped =
            Normalizer.normalize(text, Normalizer.Form.NFD)
                .replace(Regex("\\p{M}+"), "")
        return if (arabicScript.containsMatchIn(stripped)) {
            foldArabic(stripped)
        } else {
            stripped.lowercase(Locale.US)
        }
    }

    private fun foldArabic(text: String): String =
        text
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ة', 'ه')
            .replace('ى', 'ي')
            .replace('ؤ', 'و')
            .replace('ئ', 'ي')
}
