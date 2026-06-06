package com.prayertime.ui

import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.lifecycleScope
import com.prayertime.BuildConfig
import com.prayertime.alarm.PrayerAlarmScheduler
import com.prayertime.permission.AdhanPermissions
import com.prayertime.ui.city.CityInputActions
import com.prayertime.ui.city.CityInputUiState
import com.prayertime.ui.city.CitySetupViewModel
import com.prayertime.ui.components.screenSafeInsets
import com.prayertime.ui.prayer.PrayerTimesActions
import com.prayertime.ui.prayer.PrayerTimesUiState
import com.prayertime.ui.prayer.PrayerTimesViewModel
import com.prayertime.ui.screens.AboutScreen
import com.prayertime.ui.screens.AdhanNotificationsUiState
import com.prayertime.ui.screens.CityInputScreen
import com.prayertime.ui.screens.HijriCalendarScreen
import com.prayertime.ui.screens.LanguagePickerDialog
import com.prayertime.ui.screens.PrayerTimesScreen
import com.prayertime.ui.screens.PrivacyModeUiState
import com.prayertime.ui.screens.ThemeUiState
import com.prayertime.ui.settings.AppSettingsViewModel
import com.prayertime.ui.theme.PrayerTimeTheme
import kotlinx.coroutines.launch

@Composable
fun PrayerTimeRoot(activity: AppCompatActivity) {
    val layoutDirection =
        when (LocalConfiguration.current.layoutDirection) {
            View.LAYOUT_DIRECTION_RTL -> LayoutDirection.Rtl
            else -> LayoutDirection.Ltr
        }
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        val settingsViewModel: AppSettingsViewModel = hiltViewModel()
        val appTheme by settingsViewModel.appTheme.collectAsState()
        PrayerTimeTheme(theme = appTheme) {
            Surface(modifier = Modifier.fillMaxSize()) {
                val citySetupViewModel: CitySetupViewModel = hiltViewModel()
                val prayerTimesViewModel: PrayerTimesViewModel = hiltViewModel()
                val prayerState by prayerTimesViewModel.uiState.collectAsState()
                val showAbout by settingsViewModel.showAbout.collectAsState()
                var showCalendar by remember { mutableStateOf(false) }
                val snackbarHostState = remember { SnackbarHostState() }

                PrayerTimeSideEffects(
                    activity,
                    prayerTimesViewModel,
                    settingsViewModel,
                    prayerState,
                    citySetupViewModel,
                    snackbarHostState,
                )

                if (showAbout) {
                    PrayerTimeAboutRoute(activity, settingsViewModel, prayerTimesViewModel)
                } else if (showCalendar) {
                    val success = prayerState as? PrayerTimesUiState.Success
                    if (success != null) {
                        HijriCalendarRoute(
                            timezone = success.timezone,
                            onClose = { showCalendar = false },
                        )
                    } else {
                        showCalendar = false
                    }
                } else {
                    PrayerTimeMainRoute(
                        activity,
                        citySetupViewModel,
                        prayerTimesViewModel,
                        settingsViewModel,
                        prayerState,
                        snackbarHostState,
                        onCalendar = { showCalendar = true },
                    )
                }
            }
        }
    }
}

@Composable
private fun PrayerTimeSideEffects(
    activity: AppCompatActivity,
    prayerTimesViewModel: PrayerTimesViewModel,
    settingsViewModel: AppSettingsViewModel,
    prayerState: PrayerTimesUiState,
    citySetupViewModel: CitySetupViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val adhanEnabled by settingsViewModel.adhanNotificationsEnabled.collectAsState()
    val adhanSound by settingsViewModel.adhanSound.collectAsState()
    val saveError by citySetupViewModel.saveError.collectAsState()

    LifecycleResumeEffect(activity, lifecycleOwner = activity) {
        prayerTimesViewModel.refreshIfPrayerDayStale()
        onPauseOrDispose {}
    }

    LaunchedEffect(saveError) {
        val messageResId = saveError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(context.getString(messageResId))
        citySetupViewModel.consumeSaveError()
    }

    LaunchedEffect(prayerState) {
        if (prayerState is PrayerTimesUiState.FetchError) {
            snackbarHostState.showSnackbar(context.getString(prayerState.messageResId))
        }
    }

    LaunchedEffect(
        adhanEnabled,
        adhanSound,
        AdhanPermissions.areNotificationsAllowed(context),
        (prayerState as? PrayerTimesUiState.Success)?.result?.times?.map { it.timestamp },
    ) {
        if (!adhanEnabled || !AdhanPermissions.hasPostNotificationsPermission(context)) {
            PrayerAlarmScheduler.cancelAllPrayerAlarms(context)
            return@LaunchedEffect
        }
        val success = prayerState as? PrayerTimesUiState.Success ?: return@LaunchedEffect
        PrayerAlarmScheduler.schedulePrayerAlarms(
            context,
            success.result.times,
            useReliableAlarms = true,
            adhanSound = adhanSound,
        )
    }
}

@Composable
private fun PrayerTimeAboutRoute(
    activity: AppCompatActivity,
    settingsViewModel: AppSettingsViewModel,
    prayerTimesViewModel: PrayerTimesViewModel,
) {
    val offlineOnly by settingsViewModel.offlineOnly.collectAsState()
    val adhanEnabled by settingsViewModel.adhanNotificationsEnabled.collectAsState()
    val adhanSound by settingsViewModel.adhanSound.collectAsState()
    val appTheme by settingsViewModel.appTheme.collectAsState()
    val adhanPermissions = rememberAboutAdhanPermissions(activity, settingsViewModel)
    AboutAdhanAlarmScheduler(
        activity = activity,
        prayerTimesViewModel = prayerTimesViewModel,
        adhanEnabled = adhanEnabled,
        adhanSound = adhanSound,
        notificationsGranted = adhanPermissions.notificationsGranted,
    )
    AboutScreen(
        modifier = Modifier.screenSafeInsets(),
        theme =
            ThemeUiState(
                selected = appTheme,
                onThemeChanged = settingsViewModel::setAppTheme,
            ),
        privacy =
            PrivacyModeUiState(
                offlineOnly = offlineOnly,
                networkModeAvailable = BuildConfig.NETWORK_MODE_AVAILABLE,
                onOfflineOnlyChanged = settingsViewModel::setOfflineOnly,
            ),
        adhan =
            AdhanNotificationsUiState(
                enabled = adhanEnabled,
                notificationsGranted = adhanPermissions.notificationsGranted,
                exactAlarmsGranted = adhanPermissions.exactAlarmsGranted,
                batteryOptimizationExempt = adhanPermissions.batteryOptimizationExempt,
                adhanSound = adhanSound,
                onEnabledChanged = adhanPermissions.onEnabledChanged,
                onRequestNotifications = adhanPermissions.requestNotifications,
                onRequestExactAlarms = adhanPermissions.onRequestExactAlarms,
                onRequestBatteryOptimization = adhanPermissions.onRequestBatteryOptimization,
                onAdhanSoundChanged = settingsViewModel::setAdhanSound,
            ),
        onRefreshTimes = {
            settingsViewModel.hideAbout()
            prayerTimesViewModel.refreshTimes()
        },
        onBack = settingsViewModel::hideAbout,
    )
}

private data class AboutAdhanPermissionHandles(
    val notificationsGranted: Boolean,
    val exactAlarmsGranted: Boolean,
    val batteryOptimizationExempt: Boolean,
    val requestNotifications: () -> Unit,
    val onEnabledChanged: (Boolean) -> Unit,
    val onRequestExactAlarms: () -> Unit,
    val onRequestBatteryOptimization: () -> Unit,
)

@Composable
private fun rememberAboutAdhanPermissions(
    activity: AppCompatActivity,
    settingsViewModel: AppSettingsViewModel,
): AboutAdhanPermissionHandles {
    var permissionRefreshTick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(activity, lifecycleOwner = activity) {
        permissionRefreshTick++
        onPauseOrDispose {}
    }
    val notificationsGranted =
        remember(permissionRefreshTick) {
            AdhanPermissions.areNotificationsAllowed(activity)
        }
    val exactAlarmsGranted =
        remember(permissionRefreshTick) {
            AdhanPermissions.canScheduleExactAlarms(activity)
        }
    val batteryOptimizationExempt =
        remember(permissionRefreshTick) {
            AdhanPermissions.isIgnoringBatteryOptimizations(activity)
        }
    var pendingAdhanEnable by rememberSaveable { mutableStateOf(false) }
    val notificationLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            permissionRefreshTick++
            if (pendingAdhanEnable) {
                pendingAdhanEnable = false
                if (granted) {
                    settingsViewModel.setAdhanNotificationsEnabled(true)
                }
            }
        }
    val requestNotifications =
        remember(notificationLauncher) {
            {
                if (AdhanPermissions.needsPostNotificationsPermission() &&
                    !AdhanPermissions.hasPostNotificationsPermission(activity)
                ) {
                    pendingAdhanEnable = true
                    notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    AdhanPermissions.openNotificationSettings(activity)
                }
            }
        }
    val onEnabledChanged =
        remember(notificationLauncher, notificationsGranted) {
            { enabled: Boolean ->
                when {
                    !enabled -> {
                        pendingAdhanEnable = false
                        settingsViewModel.setAdhanNotificationsEnabled(false)
                    }
                    !AdhanPermissions.areNotificationsAllowed(activity) -> {
                        pendingAdhanEnable = true
                        if (AdhanPermissions.needsPostNotificationsPermission() &&
                            !AdhanPermissions.hasPostNotificationsPermission(activity)
                        ) {
                            notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            AdhanPermissions.openNotificationSettings(activity)
                        }
                    }
                    else -> settingsViewModel.setAdhanNotificationsEnabled(true)
                }
            }
        }
    return AboutAdhanPermissionHandles(
        notificationsGranted = notificationsGranted,
        exactAlarmsGranted = exactAlarmsGranted,
        batteryOptimizationExempt = batteryOptimizationExempt,
        requestNotifications = requestNotifications,
        onEnabledChanged = onEnabledChanged,
        onRequestExactAlarms = { AdhanPermissions.openExactAlarmSettings(activity) },
        onRequestBatteryOptimization = { AdhanPermissions.openBatteryOptimizationSettings(activity) },
    )
}

@Composable
private fun AboutAdhanAlarmScheduler(
    activity: AppCompatActivity,
    prayerTimesViewModel: PrayerTimesViewModel,
    adhanEnabled: Boolean,
    adhanSound: String,
    notificationsGranted: Boolean,
) {
    val prayerState by prayerTimesViewModel.uiState.collectAsState()
    val successTimestamps =
        (prayerState as? PrayerTimesUiState.Success)?.result?.times?.map { it.timestamp }
    LaunchedEffect(adhanEnabled, adhanSound, notificationsGranted, successTimestamps) {
        if (!adhanEnabled || !notificationsGranted) {
            PrayerAlarmScheduler.cancelAllPrayerAlarms(activity)
            return@LaunchedEffect
        }
        val success = prayerState as? PrayerTimesUiState.Success ?: return@LaunchedEffect
        PrayerAlarmScheduler.schedulePrayerAlarms(
            activity,
            success.result.times,
            useReliableAlarms = true,
            adhanSound = adhanSound,
        )
    }
}

@Composable
private fun PrayerTimeMainRoute(
    activity: AppCompatActivity,
    citySetupViewModel: CitySetupViewModel,
    prayerTimesViewModel: PrayerTimesViewModel,
    settingsViewModel: AppSettingsViewModel,
    prayerState: PrayerTimesUiState,
    snackbarHostState: SnackbarHostState,
    onCalendar: () -> Unit,
) {
    val wizardStep by citySetupViewModel.wizardStep.collectAsState()
    val countrySearchQuery by citySetupViewModel.countrySearchQuery.collectAsState()
    val citySearchQuery by citySetupViewModel.citySearchQuery.collectAsState()
    val isSaving by citySetupViewModel.isSaving.collectAsState()
    val catalogReady by citySetupViewModel.catalogReady.collectAsState()
    val filteredCountries by citySetupViewModel.filteredCountries.collectAsState()
    val filteredCities by citySetupViewModel.filteredCities.collectAsState()
    val offlineOnly by settingsViewModel.offlineOnly.collectAsState()
    val appLanguageTag by settingsViewModel.appLanguageTag.collectAsState()
    val mutedPrayers by settingsViewModel.mutedPrayers.collectAsState()
    var showLanguagePicker by remember { mutableStateOf(false) }

    PrayerTimeLanguagePickerGate(
        show = showLanguagePicker,
        currentTag = appLanguageTag,
        activity = activity,
        settingsViewModel = settingsViewModel,
        onDismiss = { showLanguagePicker = false },
    )

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            when {
                prayerState is PrayerTimesUiState.Loading || isSaving -> {
                    PrayerTimeMainLoading()
                }
                prayerState is PrayerTimesUiState.Success -> {
                    val success = prayerState as PrayerTimesUiState.Success
                    key(success.timezone, success.result.times.first().timestamp) {
                        PrayerTimesScreen(
                            city = success.city,
                            todayHijriDate = success.todayHijriDate,
                            upcomingEvent = success.upcomingEvent,
                            result = success.result,
                            liveCountdownFlow = prayerTimesViewModel.liveCountdown,
                            offlineOnly = offlineOnly,
                            actions =
                                PrayerTimesActions(
                                    onChangeCity = {
                                        citySetupViewModel.resetWizard()
                                        prayerTimesViewModel.clearCity()
                                    },
                                    onCalendar = onCalendar,
                                    onAbout = settingsViewModel::showAbout,
                                    onLanguage = { showLanguagePicker = true },
                                    onToggleMute = { prayer -> settingsViewModel.toggleMutedPrayer(prayer.name) },
                                    mutedPrayers = mutedPrayers,
                                ),
                        )
                    }
                }
                else -> {
                    CityInputScreen(
                        state =
                            CityInputUiState(
                                wizardStep = wizardStep,
                                countrySearchQuery = countrySearchQuery,
                                citySearchQuery = citySearchQuery,
                                filteredCountries = filteredCountries,
                                filteredCities = filteredCities,
                                showCustomCityFallback = citySetupViewModel.showCustomCityFallback,
                                catalogReady = catalogReady,
                            ),
                        actions =
                            CityInputActions(
                                onCountrySearchQueryChanged = citySetupViewModel::onCountrySearchQueryChanged,
                                onCitySearchQueryChanged = citySetupViewModel::onCitySearchQueryChanged,
                                selectCountry = citySetupViewModel::selectCountry,
                                clearSelectedCountry = citySetupViewModel::clearSelectedCountry,
                                saveCity = citySetupViewModel::saveCity,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun PrayerTimeMainLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun PrayerTimeLanguagePickerGate(
    show: Boolean,
    currentTag: String?,
    activity: AppCompatActivity,
    settingsViewModel: AppSettingsViewModel,
    onDismiss: () -> Unit,
) {
    if (!show) return
    LanguagePickerDialog(
        currentTag = currentTag,
        onDismiss = onDismiss,
        onSelect = { tag ->
            onDismiss()
            activity.lifecycleScope.launch {
                settingsViewModel.applyAppLanguage(tag)
                activity.recreate()
            }
        },
    )
}

@Composable
private fun HijriCalendarRoute(
    timezone: String,
    onClose: () -> Unit,
) {
    HijriCalendarScreen(
        modifier = Modifier.screenSafeInsets(),
        timezone = timezone,
        onClose = onClose,
    )
}
