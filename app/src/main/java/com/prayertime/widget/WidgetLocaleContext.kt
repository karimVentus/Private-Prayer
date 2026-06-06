package com.prayertime.widget

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate

/** Resolves strings with the same per-app locale as Compose (AppCompat application locales). */
internal fun Context.withAppWidgetLocale(): Context {
    val appLocales = AppCompatDelegate.getApplicationLocales()
    if (appLocales.isEmpty) {
        return this
    }
    val config = Configuration(resources.configuration)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        config.setLocales(LocaleList.forLanguageTags(appLocales.toLanguageTags()))
    } else {
        @Suppress("DEPRECATION")
        config.locale = appLocales[0]
    }
    return createConfigurationContext(config)
}
