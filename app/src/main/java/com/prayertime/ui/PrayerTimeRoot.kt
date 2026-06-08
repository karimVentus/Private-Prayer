package com.prayertime.ui

import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.prayertime.R
import com.prayertime.alarm.PrayerAlarmScheduler
import com.prayertime.permission.AdhanPermissions
import com.prayertime.ui.city.CityInputActions
import com.prayertime.ui.city.CityInputUiState
import com.prayertime.ui.city.CitySetupViewModel
import com.prayertime.ui.components.AppBottomNavItem
import com.prayertime.ui.components.AppBottomNavigationBar
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
import com.prayertime.ui.screens.QiblaScreen
import com.prayertime.ui.screens.ThemeUiState
import com.prayertime.ui.settings.AppSettingsViewModel
import com.prayertime.ui.theme.PrayerTimeTheme
import kotlinx.coroutines.launch

private val bottomNavItems =
    listOf(
        AppBottomNavItem("prayer_times", R.string.nav_prayer_times, Icons.Filled.Mosque, Icons.Outlined.Mosque),
        AppBottomNavItem("qibla", R.string.nav_qibla, Icons.Filled.Explore, Icons.Outlined.Explore),
        AppBottomNavItem("calendar", R.string.nav_calendar, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
        AppBottomNavItem("settings", R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings),
    )

@Suppress("LongMethod")
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
                val snackbarHostState = remember { SnackbarHostState() }
                val navController = rememberNavController()
                val offlineOnly by settingsViewModel.offlineOnly.collectAsState()
                var showChangeCityConfirm by remember { mutableStateOf(false) }

                PrayerTimeSideEffects(
                    activity,
                    prayerTimesViewModel,
                    settingsViewModel,
                    prayerState,
                    citySetupViewModel,
                    snackbarHostState,
                )

                if (showChangeCityConfirm) {
                    ChangeCityConfirmDialog(
                        onCityOnly = {
                            showChangeCityConfirm = false
                            citySetupViewModel.resetWizard()
                            prayerTimesViewModel.clearCity()
                        },
                        onResetAll = {
                            showChangeCityConfirm = false
                            citySetupViewModel.resetWizard()
                            activity.lifecycleScope.launch {
                                settingsViewModel.resetAllSettings()
                                prayerTimesViewModel.clearCity(clearAllPrayerCache = true)
                            }
                        },
                        onDismiss = { showChangeCityConfirm = false },
                    )
                }

                PrayerTimeNavHost(
                    navController,
                    activity,
                    citySetupViewModel,
                    prayerTimesViewModel,
                    settingsViewModel,
                    prayerState,
                    offlineOnly,
                    snackbarHostState,
                )
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
        AdhanPermissions.areNotificationsAllowed(context),
        (prayerState as? PrayerTimesUiState.Success)?.result?.times?.map { it.timestamp },
    ) {
        if (!adhanEnabled || !AdhanPermissions.areNotificationsAllowed(context)) {
            PrayerAlarmScheduler.cancelAllPrayerAlarms(context)
            return@LaunchedEffect
        }
        val success = prayerState as? PrayerTimesUiState.Success ?: return@LaunchedEffect
        PrayerAlarmScheduler.schedulePrayerAlarms(
            context,
            success.result.times,
            useReliableAlarms = true,
        )
    }
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
    notificationsGranted: Boolean,
) {
    val prayerState by prayerTimesViewModel.uiState.collectAsState()
    val successTimestamps =
        (prayerState as? PrayerTimesUiState.Success)?.result?.times?.map { it.timestamp }
    LaunchedEffect(adhanEnabled, notificationsGranted, successTimestamps) {
        if (!adhanEnabled || !notificationsGranted) {
            PrayerAlarmScheduler.cancelAllPrayerAlarms(activity)
            return@LaunchedEffect
        }
        val success = prayerState as? PrayerTimesUiState.Success ?: return@LaunchedEffect
        PrayerAlarmScheduler.schedulePrayerAlarms(
            activity,
            success.result.times,
            useReliableAlarms = true,
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
    onCalendar: () -> Unit,
    onQibla: () -> Unit,
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
    var showChangeCityConfirm by remember { mutableStateOf(false) }

    if (showChangeCityConfirm) {
        ChangeCityConfirmDialog(
            onCityOnly = {
                showChangeCityConfirm = false
                citySetupViewModel.resetWizard()
                prayerTimesViewModel.clearCity()
            },
            onResetAll = {
                showChangeCityConfirm = false
                citySetupViewModel.resetWizard()
                activity.lifecycleScope.launch {
                    settingsViewModel.resetAllSettings()
                    prayerTimesViewModel.clearCity(clearAllPrayerCache = true)
                }
            },
            onDismiss = { showChangeCityConfirm = false },
        )
    }

    PrayerTimeLanguagePickerGate(
        show = showLanguagePicker,
        currentTag = appLanguageTag,
        activity = activity,
        settingsViewModel = settingsViewModel,
        onDismiss = { showLanguagePicker = false },
    )

    Box(modifier = Modifier.fillMaxSize()) {
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
                                onChangeCity = { showChangeCityConfirm = true },
                                onCalendar = onCalendar,
                                onQibla = onQibla,
                                onAbout = settingsViewModel::showAbout,
                                onLanguage = { showLanguagePicker = true },
                                onToggleMute = { prayer ->
                                    val wasMuted = mutedPrayers.contains(prayer.name)
                                    settingsViewModel.toggleMutedPrayer(prayer.name)
                                    if (wasMuted) {
                                        prayerTimesViewModel.playAdhanIfPrayerWindow(prayer)
                                    }
                                },
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
                            languageTag = appLanguageTag,
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

@Composable
private fun ChangeCityConfirmDialog(
    onCityOnly: () -> Unit,
    onResetAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.change_city_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(R.string.change_city_dialog_message),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = onCityOnly, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.change_city_dialog_city_only))
                }
                Text(
                    text = stringResource(R.string.change_city_dialog_city_only_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onResetAll, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.change_city_dialog_reset_all))
                }
                Text(
                    text = stringResource(R.string.change_city_dialog_reset_all_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(R.string.change_city_dialog_cancel))
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

// --- Bottom navigation + NavHost ---

@Composable
@Suppress("LongMethod")
private fun PrayerTimeNavHost(
    navController: NavHostController,
    activity: AppCompatActivity,
    citySetupViewModel: CitySetupViewModel,
    prayerTimesViewModel: PrayerTimesViewModel,
    settingsViewModel: AppSettingsViewModel,
    prayerState: PrayerTimesUiState,
    offlineOnly: Boolean,
    snackbarHostState: SnackbarHostState,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current

    LaunchedEffect(prayerTimesViewModel) {
        prayerTimesViewModel.refreshFeedback.collect { messageRes ->
            snackbarHostState.showSnackbar(context.getString(messageRes))
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets.statusBars,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AppBottomNavigationBar(
                items = bottomNavItems,
                currentRoute = currentRoute,
                onItemSelected = { route ->
                    if (currentRoute != route) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id)
                            launchSingleTop = true
                        }
                    }
                },
            )
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "prayer_times",
            modifier = Modifier.padding(padding),
        ) {
            composable("prayer_times") {
                PrayerTimeMainRoute(
                    activity = activity,
                    citySetupViewModel = citySetupViewModel,
                    prayerTimesViewModel = prayerTimesViewModel,
                    settingsViewModel = settingsViewModel,
                    prayerState = prayerState,
                    onCalendar = {},
                    onQibla = {},
                )
            }
            composable("qibla") {
                val success = prayerState as? PrayerTimesUiState.Success
                if (success?.latitude != null && success.longitude != null) {
                    QiblaScreen(
                        latitude = success.latitude,
                        longitude = success.longitude,
                        cityLabel = success.city,
                        showBackLink = false,
                        onClose = { navController.popBackStack() },
                    )
                } else {
                    LaunchedEffect(Unit) { navController.navigate("prayer_times") { popUpTo(0) { inclusive = true } } }
                }
            }
            composable("calendar") {
                val success = prayerState as? PrayerTimesUiState.Success
                if (success != null) {
                    HijriCalendarScreen(
                        timezone = success.timezone,
                        showBackLink = false,
                        onClose = { navController.popBackStack() },
                    )
                } else {
                    LaunchedEffect(Unit) { navController.navigate("prayer_times") { popUpTo(0) { inclusive = true } } }
                }
            }
            composable("settings") {
                val adhanEnabled by settingsViewModel.adhanNotificationsEnabled.collectAsState()
                val adhanPlayWhenSilent by settingsViewModel.adhanPlayWhenSilent.collectAsState()
                val adhanSound by settingsViewModel.adhanSound.collectAsState()
                val customSoundsVersion by settingsViewModel.customSoundsVersion.collectAsState()
                val adhanPermissions = rememberAboutAdhanPermissions(activity, settingsViewModel)
                val aboutTheme by settingsViewModel.appTheme.collectAsState()

                AboutAdhanAlarmScheduler(
                    activity = activity,
                    prayerTimesViewModel = prayerTimesViewModel,
                    adhanEnabled = adhanEnabled,
                    notificationsGranted = adhanPermissions.notificationsGranted,
                )
                AboutScreen(
                    theme = ThemeUiState(selected = aboutTheme, onThemeChanged = settingsViewModel::setAppTheme),
                    privacy =
                        PrivacyModeUiState(
                            offlineOnly = offlineOnly,
                            onOfflineOnlyChanged = settingsViewModel::setOfflineOnly,
                        ),
                    adhan =
                        AdhanNotificationsUiState(
                            enabled = adhanEnabled,
                            playWhenSilent = adhanPlayWhenSilent,
                            notificationsGranted = adhanPermissions.notificationsGranted,
                            exactAlarmsGranted = adhanPermissions.exactAlarmsGranted,
                            batteryOptimizationExempt = adhanPermissions.batteryOptimizationExempt,
                            adhanSound = adhanSound,
                            onEnabledChanged = adhanPermissions.onEnabledChanged,
                            onPlayWhenSilentChanged = settingsViewModel::setAdhanPlayWhenSilent,
                            customSoundsVersion = customSoundsVersion,
                            onRequestNotifications = adhanPermissions.requestNotifications,
                            onRequestExactAlarms = adhanPermissions.onRequestExactAlarms,
                            onRequestBatteryOptimization = adhanPermissions.onRequestBatteryOptimization,
                            onAdhanSoundChanged = settingsViewModel::setAdhanSound,
                            onImportCustomSound = settingsViewModel::importCustomSound,
                            onDeleteCustomSound = settingsViewModel::deleteCustomSound,
                        ),
                    onRefreshTimes = { prayerTimesViewModel.refreshTimes() },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
