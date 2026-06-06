package com.prayertime.locale

import java.text.Normalizer
import java.util.Locale

object TextNormalizer {
    /** Case- and diacritic-insensitive folding (e.g. "Osnabrück" ↔ "osnabruck"). */
    fun foldForLookup(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.US)
}
