package com.prayertime.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/** Applies per-app UI language via AppCompat (API 23+). Empty/null tag = follow system. */
object AppLocale {
    data class SupportedLanguage(
        val tag: String?,
        val labelResId: Int,
    )

    val supportedLanguages: List<SupportedLanguage> =
        listOf(
            SupportedLanguage(null, com.prayertime.R.string.language_system),
            SupportedLanguage("en", com.prayertime.R.string.language_english),
            SupportedLanguage("ar", com.prayertime.R.string.language_arabic),
        )

    fun apply(languageTag: String?) {
        val tag = languageTag?.trim()
        val locales =
            when {
                tag.isNullOrEmpty() || tag == "system" -> LocaleListCompat.getEmptyLocaleList()
                else -> LocaleListCompat.forLanguageTags(tag)
            }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    /** First-launch default: Arabic UI when the device primary locale is Arabic, else English. */
    fun defaultTagFromSystem(): String =
        if (Locale.getDefault().language.equals("ar", ignoreCase = true)) {
            "ar"
        } else {
            "en"
        }

    /** Normalizes persisted/DataStore tags for picker UI (null = system). */
    fun normalizeStoredTag(tag: String?): String? =
        when (tag?.trim()) {
            null, "", "system" -> null
            else -> tag
        }
}
