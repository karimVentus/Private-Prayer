package com.prayertime.locale

object ArabicDigits {
    private val westernToEasternArabic =
        mapOf(
            '0' to '٠',
            '1' to '١',
            '2' to '٢',
            '3' to '٣',
            '4' to '٤',
            '5' to '٥',
            '6' to '٦',
            '7' to '٧',
            '8' to '٨',
            '9' to '٩',
        )

    fun format(text: String): String = text.map { westernToEasternArabic[it] ?: it }.joinToString("")
}

fun Int.toEasternArabicDigits(): String = ArabicDigits.format(toString())
