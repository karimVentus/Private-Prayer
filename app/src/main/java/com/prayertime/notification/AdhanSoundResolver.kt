package com.prayertime.notification

import android.content.Context
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import com.prayertime.R
import java.io.File

object AdhanSoundResolver {
    const val DEFAULT_KEY = "adhan"
    const val CUSTOM_PREFIX = "custom_"
    private const val CUSTOM_DIR = "custom_adhans"

    data class Option(
        val storageKey: String,
        @StringRes val labelRes: Int,
        @RawRes val rawRes: Int,
    )

    /** Unified display option for UI (built-in + custom). */
    data class DisplayOption(
        val storageKey: String,
        val label: String,
        val isCustom: Boolean,
        @RawRes val rawRes: Int = 0,
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
    fun rawResFor(storageKey: String): Int =
        catalog.firstOrNull { it.storageKey == storageKey }?.rawRes ?: R.raw.adhan

    fun isCustom(storageKey: String): Boolean = storageKey.startsWith(CUSTOM_PREFIX)

    /** Returns the directory where custom adhan files are stored. */
    fun customDir(context: Context): File = File(context.filesDir, CUSTOM_DIR)

    /** Returns the absolute file path for a custom sound key. */
    fun filePathForCustom(context: Context, storageKey: String): String {
        val fileName = storageKey.removePrefix(CUSTOM_PREFIX)
        return File(customDir(context), fileName).absolutePath
    }

    /** Returns the display label for a custom sound key (filename without extension). */
    fun labelForCustom(storageKey: String): String {
        val fileName = storageKey.removePrefix(CUSTOM_PREFIX)
        return fileName.substringBeforeLast(".")
    }

    /** Lists custom audio files from internal storage. */
    fun listCustomFiles(context: Context): List<File> {
        val dir = customDir(context)
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        return dir.listFiles()
            ?.filter { it.isFile && it.name.lowercase().let { n -> n.endsWith(".mp3") || n.endsWith(".ogg") || n.endsWith(".wav") || n.endsWith(".flac") || n.endsWith(".m4a") || n.endsWith(".aac") } }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
    }

    /** Merged list of built-in and custom display options for the picker UI. */
    fun mergedOptions(context: Context): List<DisplayOption> {
        val builtIn = options.map { opt ->
            DisplayOption(
                storageKey = opt.storageKey,
                label = context.getString(opt.labelRes),
                isCustom = false,
                rawRes = opt.rawRes,
            )
        }
        val custom = listCustomFiles(context).map { file ->
            val key = CUSTOM_PREFIX + file.name
            DisplayOption(
                storageKey = key,
                label = file.nameWithoutExtension,
                isCustom = true,
            )
        }
        return builtIn + custom
    }
}
