package com.prayertime.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prayertime.data.local.AppPreferencesDataSource
import com.prayertime.data.repository.PrayerTimesRepository
import com.prayertime.locale.AppLocale
import com.prayertime.notification.AdhanNotificationHelper
import com.prayertime.notification.AdhanSoundResolver
import com.prayertime.ui.theme.AppTheme
import com.prayertime.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AppSettingsViewModel internal constructor(
    private val repository: PrayerTimesRepository,
    private val preferences: AppPreferencesDataSource,
    private val onLocaleChanged: suspend () -> Unit,
    private val adhanNotificationHelper: AdhanNotificationHelper?,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {
    @Inject
    constructor(
        repository: PrayerTimesRepository,
        preferences: AppPreferencesDataSource,
        widgetUpdater: WidgetUpdater,
        adhanNotificationHelper: AdhanNotificationHelper,
        @ApplicationContext appContext: Context,
    ) : this(
        repository,
        preferences,
        onLocaleChanged = { widgetUpdater.updateAll() },
        adhanNotificationHelper = adhanNotificationHelper,
        appContext = appContext,
    )

    private val _showAbout = MutableStateFlow(false)
    val showAbout: StateFlow<Boolean> = _showAbout.asStateFlow()

    private val _offlineOnly = MutableStateFlow(true)
    val offlineOnly: StateFlow<Boolean> = _offlineOnly.asStateFlow()

    private val _adhanNotificationsEnabled = MutableStateFlow(false)
    val adhanNotificationsEnabled: StateFlow<Boolean> = _adhanNotificationsEnabled.asStateFlow()

    private val _adhanPlayWhenSilent = MutableStateFlow(false)
    val adhanPlayWhenSilent: StateFlow<Boolean> = _adhanPlayWhenSilent.asStateFlow()

    private val _appLanguageTag = MutableStateFlow<String?>(null)
    val appLanguageTag: StateFlow<String?> = _appLanguageTag.asStateFlow()

    private val _adhanSound = MutableStateFlow(AppPreferencesDataSource.DEFAULT_ADHAN_SOUND)
    val adhanSound: StateFlow<String> = _adhanSound.asStateFlow()

    private val _appTheme = MutableStateFlow(AppTheme.LIGHT)
    val appTheme: StateFlow<AppTheme> = _appTheme.asStateFlow()

    private val _mutedPrayers = MutableStateFlow<Set<String>>(emptySet())
    val mutedPrayers: StateFlow<Set<String>> = _mutedPrayers.asStateFlow()

    init {
        viewModelScope.launch {
            _offlineOnly.value = repository.offlineOnly.first()
        }
        viewModelScope.launch {
            preferences.adhanNotificationsEnabled.collect { enabled ->
                _adhanNotificationsEnabled.value = enabled
            }
        }
        viewModelScope.launch {
            preferences.adhanPlayWhenSilent.collect { enabled ->
                _adhanPlayWhenSilent.value = enabled
            }
        }
        viewModelScope.launch {
            preferences.appLanguageTag.collect { tag ->
                _appLanguageTag.value = tag
            }
        }
        viewModelScope.launch {
            preferences.adhanSound.collect { sound ->
                _adhanSound.value = sound
            }
        }
        viewModelScope.launch {
            preferences.appTheme.collect { theme ->
                _appTheme.value = AppTheme.fromStorage(theme)
            }
        }
        viewModelScope.launch {
            preferences.mutedPrayers.collect { prayers ->
                _mutedPrayers.value = prayers
            }
        }
    }

    fun showAbout() {
        _showAbout.value = true
    }

    fun hideAbout() {
        _showAbout.value = false
    }

    fun setOfflineOnly(enabled: Boolean) {
        viewModelScope.launch {
            repository.setOfflineOnly(enabled)
            _offlineOnly.value = enabled
        }
    }

    fun setAdhanNotificationsEnabled(enabled: Boolean) {
        _adhanNotificationsEnabled.value = enabled
        viewModelScope.launch {
            preferences.setAdhanNotificationsEnabled(enabled)
            if (enabled) {
                adhanNotificationHelper?.showAdhanEnabledConfirmation()
            }
        }
    }

    fun setAdhanPlayWhenSilent(enabled: Boolean) {
        _adhanPlayWhenSilent.value = enabled
        viewModelScope.launch {
            preferences.setAdhanPlayWhenSilent(enabled)
        }
    }

    suspend fun setAppLanguageTag(tag: String?) {
        preferences.setAppLanguageTag(tag)
        _appLanguageTag.value = tag
    }

    fun setAdhanSound(sound: String) {
        _adhanSound.value = sound
        viewModelScope.launch {
            preferences.setAdhanSound(sound)
        }
    }

    private val _customSoundsVersion = MutableStateFlow(0)
    val customSoundsVersion: StateFlow<Int> = _customSoundsVersion.asStateFlow()

    /** Copies the selected audio file to internal storage and sets it as the active sound. */
    fun importCustomSound(uri: Uri) {
        viewModelScope.launch {
            try {
                val fileName = resolveFileName(uri) ?: return@launch
                val dir = AdhanSoundResolver.customDir(appContext)
                if (!dir.exists()) dir.mkdirs()
                val destFile = File(dir, fileName)
                withContext(Dispatchers.IO) {
                    appContext.contentResolver.openInputStream(uri)?.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                val key = AdhanSoundResolver.CUSTOM_PREFIX + fileName
                setAdhanSound(key)
                _customSoundsVersion.value += 1
            } catch (_: Exception) {
                // Silently ignore import failures
            }
        }
    }

    /** Deletes a custom sound file from internal storage. If it was the selected sound, reverts to default. */
    fun deleteCustomSound(storageKey: String) {
        viewModelScope.launch {
            try {
                val file = File(AdhanSoundResolver.filePathForCustom(appContext, storageKey))
                withContext(Dispatchers.IO) {
                    if (file.exists()) file.delete()
                }
                if (_adhanSound.value == storageKey) {
                    setAdhanSound(AppPreferencesDataSource.DEFAULT_ADHAN_SOUND)
                }
                _customSoundsVersion.value += 1
            } catch (_: Exception) {
                // Silently ignore deletion failures
            }
        }
    }

    private fun resolveFileName(uri: Uri): String? {
        var name: String? = null
        appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        if (name.isNullOrBlank()) {
            name = uri.lastPathSegment?.substringAfterLast('/')
        }
        return if (!name.isNullOrBlank()) name else null
    }

    fun setAppTheme(theme: AppTheme) {
        _appTheme.value = theme
        viewModelScope.launch {
            applyAppTheme(theme)
        }
    }

    suspend fun applyAppTheme(theme: AppTheme) {
        preferences.setAppTheme(theme.storageKey())
        _appTheme.value = theme
        onLocaleChanged()
    }

    suspend fun applyAppLanguage(tag: String?) {
        val normalized = AppLocale.normalizeStoredTag(tag)
        preferences.setAppLanguageTag(normalized, recordUserChoice = true)
        _appLanguageTag.value = normalized
        AppLocale.apply(normalized)
        onLocaleChanged()
    }

    fun toggleMutedPrayer(prayer: String) {
        val current = _mutedPrayers.value.toMutableSet()
        if (current.contains(prayer)) {
            current.remove(prayer)
        } else {
            current.add(prayer)
        }
        _mutedPrayers.value = current
        viewModelScope.launch {
            preferences.setMutedPrayers(current)
        }
    }

    suspend fun resetAllSettings() {
        preferences.resetToDefaults()
        repository.resetCityStore()
        _offlineOnly.value = true
        _adhanNotificationsEnabled.value = false
        _adhanPlayWhenSilent.value = true
        _appLanguageTag.value = null
        _adhanSound.value = AppPreferencesDataSource.DEFAULT_ADHAN_SOUND
        _appTheme.value = AppTheme.LIGHT
        _mutedPrayers.value = emptySet()
        AppLocale.apply(null)
        onLocaleChanged()
    }
}
