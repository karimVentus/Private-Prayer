package com.prayertime.widget

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import com.prayertime.locale.AppLocale
import java.util.Locale

/**
 * Resolves widget strings with the same per-app locale as Compose.
 * Prefers persisted DataStore tag (sync cache) so background WorkManager updates stay
 * localized when [AppCompatDelegate.getApplicationLocales] is still empty after doze.
 */
internal fun Context.withAppWidgetLocale(storedLanguageTag: String? = null): Context {
    val tag =
        AppLocale.normalizeStoredTag(storedLanguageTag)
            ?: run {
                val appLocales = AppCompatDelegate.getApplicationLocales()
                if (appLocales.isEmpty) {
                    null
                } else {
                    appLocales[0]?.toLanguageTag()
                }
            }
    if (tag.isNullOrEmpty()) {
        return this
    }
    val config = Configuration(resources.configuration)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        config.setLocales(LocaleList.forLanguageTags(tag))
    } else {
        @Suppress("DEPRECATION")
        config.locale = Locale.forLanguageTag(tag)
    }
    return createConfigurationContext(config)
}
