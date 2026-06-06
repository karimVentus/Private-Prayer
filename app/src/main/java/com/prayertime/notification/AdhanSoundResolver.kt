package com.prayertime.notification

import androidx.annotation.RawRes
import androidx.annotation.StringRes
import com.prayertime.R

object AdhanSoundResolver {
    const val DEFAULT_KEY = "adhan"

    data class Option(
        val storageKey: String,
        @StringRes val labelRes: Int,
        @RawRes val rawRes: Int,
    )

    private val catalog: List<Option> =
        listOf(
            Option(DEFAULT_KEY, R.string.adhan_sound_default, R.raw.adhan),
            Option("ahmad_alhaddad", R.string.adhan_sound_ahmad_alhaddad, R.raw.ahmad_alhaddad),
            Option("alhram", R.string.adhan_sound_alhram, R.raw.alhram),
            Option("alhram2", R.string.adhan_sound_alhram2, R.raw.alhram2),
            Option("almadina", R.string.adhan_sound_almadina, R.raw.almadina),
            Option("almadina2", R.string.adhan_sound_almadina2, R.raw.almadina2),
            Option("alsbeawey", R.string.adhan_sound_alsbeawey, R.raw.alsbeawey),
            Option("easa_alhjlawey", R.string.adhan_sound_easa_alhjlawey, R.raw.easa_alhjlawey),
        )

    val options: List<Option> = catalog

    @RawRes
    fun rawResFor(storageKey: String): Int = catalog.firstOrNull { it.storageKey == storageKey }?.rawRes ?: R.raw.adhan
}
