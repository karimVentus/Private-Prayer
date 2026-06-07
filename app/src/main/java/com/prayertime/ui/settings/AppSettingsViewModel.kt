package com.prayertime.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prayertime.data.local.AppPreferencesDataSource
import com.prayertime.data.repository.PrayerTimesRepository
import com.prayertime.locale.AppLocale
import com.prayertime.notification.AdhanNotificationHelper
import com.prayertime.ui.theme.AppTheme
import com.prayertime.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppSettingsViewModel internal constructor(
    private val repository: PrayerTimesRepository,
    private val preferences: AppPreferencesDataSource,
    private val onLocaleChanged: suspend () -> Unit,
    private val adhanNotificationHelper: AdhanNotificationHelper?,
) : ViewModel() {
    @Inject
    constructor(
        repository: PrayerTimesRepository,
        preferences: AppPreferencesDataSource,
        widgetUpdater: WidgetUpdater,
        adhanNotificationHelper: AdhanNotificationHelper,
    ) : this(
        repository,
        preferences,
        onLocaleChanged = { widgetUpdater.updateAll() },
        adhanNotificationHelper = adhanNotificationHelper,
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
        preferences.setAppLanguageTag(normalized)
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
        _adhanPlayWhenSilent.value = false
        _appLanguageTag.value = null
        _adhanSound.value = AppPreferencesDataSource.DEFAULT_ADHAN_SOUND
        _appTheme.value = AppTheme.LIGHT
        _mutedPrayers.value = emptySet()
        AppLocale.apply(null)
        onLocaleChanged()
    }
}
