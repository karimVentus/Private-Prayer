package com.prayertime.domain.util

import com.prayertime.domain.model.Country
import java.util.Locale

object LocationNames {
    fun effectiveLanguageTag(storedTag: String?): String {
        val normalized = normalizeTag(storedTag)
        if (normalized != null) return normalized
        return if (Locale.getDefault().language.equals("ar", ignoreCase = true)) "ar" else "en"
    }

    private fun normalizeTag(tag: String?): String? =
        when (tag?.trim()) {
            null, "", "system" -> null
            else -> tag.trim()
        }

    fun countryDisplay(
        country: Country,
        languageTag: String?,
    ): String {
        if (!effectiveLanguageTag(languageTag).startsWith("ar")) return country.name
        val arabic =
            Locale.Builder()
                .setRegion(country.code)
                .build()
                .getDisplayCountry(Locale("ar"))
        return arabic.ifBlank { country.name }
    }

    fun cityDisplay(
        canonicalEnglish: String,
        arabicName: String?,
        languageTag: String?,
    ): String {
        if (!effectiveLanguageTag(languageTag).startsWith("ar")) return canonicalEnglish
        return arabicName?.takeIf { it.isNotBlank() } ?: canonicalEnglish
    }

    fun formatCityHeader(
        cityName: String,
        country: Country,
        cityArabic: String?,
        languageTag: String?,
    ): String {
        val city = cityDisplay(cityName, cityArabic, languageTag)
        val countryLabel = countryDisplay(country, languageTag)
        val separator = if (effectiveLanguageTag(languageTag).startsWith("ar")) "، " else ", "
        return "$city$separator$countryLabel"
    }

    fun matchesQuery(
        primary: String,
        alternate: String,
        query: String,
    ): Boolean {
        if (query.isBlank()) return true
        val foldedQuery = normalizeForLookup(query)
        return normalizeForLookup(primary).contains(foldedQuery) ||
            normalizeForLookup(alternate).contains(foldedQuery)
    }

    private fun normalizeForLookup(text: String): String {
        val sb = StringBuilder(text.length)
        for (c in text.lowercase(Locale.ROOT)) {
            sb.append(foldLatinAccent(c) ?: c.toString())
        }
        return sb.toString()
    }

    private fun foldLatinAccent(c: Char): String? =
        when (c) {
            'á', 'à', 'â', 'ä', 'ã', 'å', 'ā' -> "a"
            'é', 'è', 'ê', 'ë', 'ē' -> "e"
            'í', 'ì', 'î', 'ï', 'ī' -> "i"
            'ó', 'ò', 'ô', 'ö', 'õ', 'ø', 'ō' -> "o"
            'ú', 'ù', 'û', 'ü', 'ū' -> "u"
            'ñ', 'ń' -> "n"
            'ç', 'ć' -> "c"
            'š' -> "s"
            'ž' -> "z"
            'đ' -> "d"
            'ł' -> "l"
            'ß' -> "ss"
            else -> null
        }
}
