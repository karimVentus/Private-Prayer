package com.prayertime.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.prayertime.notification.AdhanSoundResolver
import com.prayertime.ui.theme.AppTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appSettingsStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class AppPreferencesDataSource
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val adhanEnabledKey = booleanPreferencesKey("adhan_notifications_enabled")
        private val adhanPlayWhenSilentKey = booleanPreferencesKey("adhan_play_when_silent")
        private val appLanguageTagKey = stringPreferencesKey("app_language_tag")
        private val adhanSoundKey = stringPreferencesKey("adhan_sound")
        private val appThemeKey = stringPreferencesKey("app_theme")

        val adhanNotificationsEnabled: Flow<Boolean> =
            context.appSettingsStore.data.map { prefs ->
                prefs[adhanEnabledKey] ?: true
            }

        suspend fun setAdhanNotificationsEnabled(enabled: Boolean) {
            context.appSettingsStore.edit { prefs ->
                prefs[adhanEnabledKey] = enabled
            }
        }

        val adhanPlayWhenSilent: Flow<Boolean> =
            context.appSettingsStore.data.map { prefs ->
                prefs[adhanPlayWhenSilentKey] ?: false
            }

        suspend fun setAdhanPlayWhenSilent(enabled: Boolean) {
            context.appSettingsStore.edit { prefs ->
                if (enabled) {
                    prefs[adhanPlayWhenSilentKey] = true
                } else {
                    prefs.remove(adhanPlayWhenSilentKey)
                }
            }
        }

        suspend fun readAdhanPlayWhenSilentOnce(): Boolean = adhanPlayWhenSilent.first()

        val appLanguageTag: Flow<String?> =
            context.appSettingsStore.data.map { prefs ->
                prefs[appLanguageTagKey]
            }

        suspend fun setAppLanguageTag(tag: String?) {
            context.appSettingsStore.edit { prefs ->
                if (tag.isNullOrBlank()) {
                    prefs.remove(appLanguageTagKey)
                } else {
                    prefs[appLanguageTagKey] = tag
                }
            }
            writeAppLanguageCache(tag)
        }

        suspend fun readAppLanguageTagOnce(): String? = appLanguageTag.first()

        /** Sync read for widget bind — mirrors DataStore via [writeAppLanguageCache]. */
        fun readAppLanguageTagSync(): String? = themeCachePrefs.getString(APP_LANGUAGE_CACHE_KEY, null)

        /** Backfill cache after upgrade when DataStore already has a language tag. */
        suspend fun warmAppLanguageCache() {
            writeAppLanguageCache(readAppLanguageTagOnce())
        }

        val adhanSound: Flow<String> =
            context.appSettingsStore.data.map { prefs ->
                prefs[adhanSoundKey] ?: DEFAULT_ADHAN_SOUND
            }

        suspend fun setAdhanSound(sound: String) {
            context.appSettingsStore.edit { prefs ->
                prefs[adhanSoundKey] = sound
            }
        }

        val appTheme: Flow<String> =
            context.appSettingsStore.data.map { prefs ->
                prefs[appThemeKey] ?: AppTheme.DEFAULT_STORAGE_KEY
            }

        suspend fun setAppTheme(theme: String) {
            context.appSettingsStore.edit { prefs ->
                prefs[appThemeKey] = theme
            }
            writeAppThemeCache(theme)
        }

        suspend fun readAppThemeOnce(): String = appTheme.first()

        /** Sync read for widget bind — mirrors DataStore via [writeAppThemeCache]. */
        fun readAppThemeSync(): AppTheme =
            AppTheme.fromStorage(
                themeCachePrefs.getString(APP_THEME_CACHE_KEY, AppTheme.DEFAULT_STORAGE_KEY),
            )

        /** Backfill cache after upgrade when DataStore already has a theme. */
        suspend fun warmAppThemeCache() {
            writeAppThemeCache(readAppThemeOnce())
        }

        private fun writeAppThemeCache(theme: String) {
            themeCachePrefs.edit().putString(APP_THEME_CACHE_KEY, theme).apply()
        }

        private fun writeAppLanguageCache(tag: String?) {
            themeCachePrefs.edit().apply {
                if (tag.isNullOrBlank()) {
                    remove(APP_LANGUAGE_CACHE_KEY)
                } else {
                    putString(APP_LANGUAGE_CACHE_KEY, tag)
                }
            }.apply()
        }

        private val themeCachePrefs =
            context.getSharedPreferences(THEME_CACHE_PREFS, Context.MODE_PRIVATE)

        private val mutedPrayersKey = stringPreferencesKey("muted_prayers")

        val mutedPrayers: Flow<Set<String>> =
            context.appSettingsStore.data.map { prefs ->
                prefs[mutedPrayersKey]
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?.toSet()
                    ?: emptySet()
            }

        suspend fun setMutedPrayers(prayers: Set<String>) {
            context.appSettingsStore.edit { prefs ->
                if (prayers.isEmpty()) {
                    prefs.remove(mutedPrayersKey)
                } else {
                    prefs[mutedPrayersKey] = prayers.joinToString(",")
                }
            }
        }

        suspend fun resetToDefaults() {
            context.appSettingsStore.edit { it.clear() }
            writeAppThemeCache(AppTheme.DEFAULT_STORAGE_KEY)
            writeAppLanguageCache(null)
        }

        suspend fun isPrayerMuted(prayer: String): Boolean = mutedPrayers.first().contains(prayer)

        companion object {
            const val DEFAULT_ADHAN_SOUND = AdhanSoundResolver.DEFAULT_KEY
            private const val THEME_CACHE_PREFS = "widget_theme_cache"
            private const val APP_THEME_CACHE_KEY = "app_theme"
            private const val APP_LANGUAGE_CACHE_KEY = "app_language_tag"
        }
    }
