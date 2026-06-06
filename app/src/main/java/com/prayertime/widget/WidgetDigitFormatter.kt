package com.prayertime.widget

import android.content.Context
import android.os.Build
import com.prayertime.locale.ArabicDigits

internal object WidgetDigitFormatter {
    fun toEasternArabicDigits(text: String): String = ArabicDigits.format(text)
}

internal fun usesArabicWidgetDigits(context: Context): Boolean {
    val config = context.resources.configuration
    val language =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.locales[0].language
        } else {
            @Suppress("DEPRECATION")
            config.locale.language
        }
    return language == "ar"
}

internal fun localizeWidgetDigits(
    text: String,
    useArabicDigits: Boolean,
): String =
    if (useArabicDigits) {
        WidgetDigitFormatter.toEasternArabicDigits(text)
    } else {
        text
    }

internal fun Context.localizeWidgetDigits(text: String): String = localizeWidgetDigits(text, usesArabicWidgetDigits(this))
