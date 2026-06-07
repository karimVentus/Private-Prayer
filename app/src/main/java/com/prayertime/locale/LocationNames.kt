package com.prayertime.locale

import com.prayertime.domain.model.Country
import java.util.Locale

/** Localized display names for bundled country/city catalogs. */
object LocationNames {
    fun effectiveLanguageTag(storedTag: String?): String {
        val normalized = AppLocale.normalizeStoredTag(storedTag)
        if (normalized != null) return normalized
        return if (Locale.getDefault().language.equals("ar", ignoreCase = true)) {
            "ar"
        } else {
            "en"
        }
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
        val foldedQuery = TextNormalizer.foldForLookup(query)
        return TextNormalizer.foldForLookup(primary).contains(foldedQuery) ||
            TextNormalizer.foldForLookup(alternate).contains(foldedQuery)
    }
}
