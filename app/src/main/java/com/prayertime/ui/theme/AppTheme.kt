package com.prayertime.ui.theme

enum class AppTheme {
    LIGHT,
    GREEN,
    DARK,
    ;

    fun storageKey(): String = name.lowercase()

    companion object {
        const val DEFAULT_STORAGE_KEY = "light"

        fun fromStorage(value: String?): AppTheme =
            when (value?.lowercase()) {
                "green" -> GREEN
                "dark" -> DARK
                else -> LIGHT
            }
    }
}
