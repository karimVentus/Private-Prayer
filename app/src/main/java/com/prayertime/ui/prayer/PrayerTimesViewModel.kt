package com.prayertime.ui.prayer

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prayertime.data.local.AppPreferencesDataSource
import com.prayertime.data.repository.PrayerTimesRepository
import com.prayertime.domain.calculator.HijriCalculator
import com.prayertime.domain.calculator.PrayerTimeCalculator
import com.prayertime.domain.model.CityConfig
import com.prayertime.domain.model.FetchError
import com.prayertime.domain.model.HijriDate
import com.prayertime.domain.model.Prayer
import com.prayertime.domain.model.PrayerTime
import com.prayertime.domain.model.PrayerTimesResult
import com.prayertime.domain.model.UpcomingEvent
import com.prayertime.domain.repository.LocationRepository
import com.prayertime.ui.LivePrayerCountdown
import com.prayertime.ui.PrayerTimesErrorMapper
import com.prayertime.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class PrayerTimesViewModel
    @Inject
    constructor(
        private val repository: PrayerTimesRepository,
        private val locationRepository: LocationRepository,
        private val preferences: AppPreferencesDataSource,
        private val widgetUpdater: WidgetUpdater,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<PrayerTimesUiState>(PrayerTimesUiState.Loading)
        val uiState: StateFlow<PrayerTimesUiState> = _uiState.asStateFlow()

        private val _liveCountdown = MutableStateFlow<LivePrayerCountdown?>(null)
        val liveCountdown: StateFlow<LivePrayerCountdown?> = _liveCountdown.asStateFlow()

        private var countdownJob: Job? = null
        private var currentConfig: CityConfig? = null
        private var lastWidgetPrayer: Prayer? = null

        init {
            viewModelScope.launch {
                repository.cityConfig.collect { config ->
                    if (config != null) {
                        fetchTimes(config)
                    } else {
                        onCityConfigCleared()
                    }
                }
            }
            viewModelScope.launch {
                repository.offlineOnly.drop(1).collect {
                    val config = currentConfig ?: return@collect
                    if (_uiState.value is PrayerTimesUiState.Success ||
                        _uiState.value is PrayerTimesUiState.FetchError
                    ) {
                        fetchTimes(config)
                    }
                }
            }
            viewModelScope.launch {
                preferences.appLanguageTag.collect { languageTag ->
                    val success = _uiState.value as? PrayerTimesUiState.Success ?: return@collect
                    val config = currentConfig ?: return@collect
                    locationRepository.awaitReady()
                    _uiState.value =
                        success.copy(
                            city = formatCityHeader(config, languageTag),
                        )
                }
            }
        }

        private fun formatCityHeader(
            config: CityConfig,
            languageTag: String?,
        ): String =
            locationRepository.formatCityHeader(
                cityName = config.cityName,
                countryCode = config.countryCode,
                languageTag = languageTag,
            )

        fun clearCity(clearAllPrayerCache: Boolean = false) {
            currentConfig = null
            stopCountdownTicker()
            _uiState.value = PrayerTimesUiState.NoCity
            widgetUpdater.requestImmediateUpdate()
            viewModelScope.launch {
                if (clearAllPrayerCache) {
                    repository.clearAllCaches()
                }
                clearCityConfig()
            }
        }

        fun refreshTimes() {
            refreshTimesInternal(showLoading = true, bypassCache = true)
        }

        /** City-calendar midnight rollover: reload times without replacing the screen with Loading. */
        fun refreshTimesForNewDay() {
            refreshTimesInternal(showLoading = false)
        }

        /**
         * Resume / reopen: silent reload when displayed times belong to a past city calendar day.
         * Uses the loaded Fajr anchor and city [timezone], not device local midnight or elapsed hours.
         */
        fun refreshIfPrayerDayStale() {
            val success = _uiState.value as? PrayerTimesUiState.Success ?: return
            val dayAnchor = success.result.times.firstOrNull()?.timestamp ?: return
            if (needsCityDayRefresh(dayAnchor, success.timezone)) {
                refreshTimesForNewDay()
                return
            }
            // Detect manual clock changes: if current time is far from the expected
            // prayer-day window (before Fajr-1h or after Fajr+25h), force refresh.
            val now = System.currentTimeMillis()
            if (now < dayAnchor - 3_600_000L || now > dayAnchor + 90_000_000L) {
                refreshTimesForNewDay()
            }
        }

        private fun refreshTimesInternal(
            showLoading: Boolean,
            bypassCache: Boolean = false,
        ) {
            val config = currentConfig ?: return
            if (_uiState.value is PrayerTimesUiState.Loading) return
            viewModelScope.launch {
                if (bypassCache) {
                    repository.invalidateTodayCache(config)
                }
                fetchTimes(config, showLoading)
            }
        }

        private suspend fun fetchTimes(
            config: CityConfig,
            showLoading: Boolean = true,
        ) {
            currentConfig = config
            val previousSuccess = _uiState.value as? PrayerTimesUiState.Success
            if (showLoading) {
                _uiState.value = PrayerTimesUiState.Loading
                stopCountdownTicker()
            }
            val offlineOnly = repository.offlineOnly.first()
            locationRepository.awaitReady()
            val languageTag = preferences.readAppLanguageTagOnce()
            when (val result = repository.fetchTodayTimes(config)) {
                is PrayerTimesResult.Success -> {
                    startCountdownTicker(result.times, config.timezone, result.nextPrayer, result.countdown)
                    widgetUpdater.requestImmediateUpdate()
                    _uiState.value =
                        PrayerTimesUiState.Success(
                            city = formatCityHeader(config, languageTag),
                            timezone = config.timezone,
                            latitude = config.latitude,
                            longitude = config.longitude,
                            result = result,
                            todayHijriDate = todayHijriDate(config.timezone),
                            upcomingEvent = upcomingEvent(config.timezone),
                        )
                }
                is PrayerTimesResult.Error -> {
                    if (result.type == FetchError.CITY_NOT_FOUND) {
                        stopCountdownTicker()
                        _uiState.value =
                            PrayerTimesUiState.FetchError(
                                PrayerTimesErrorMapper.fetchError(result.type, offlineOnly),
                            )
                        clearCityConfig()
                        return
                    }
                    if (!showLoading && previousSuccess != null) {
                        return
                    }
                    stopCountdownTicker()
                    _uiState.value =
                        PrayerTimesUiState.FetchError(
                            PrayerTimesErrorMapper.fetchError(result.type, offlineOnly),
                        )
                }
            }
        }

        private fun startCountdownTicker(
            times: List<PrayerTime>,
            timezone: String,
            fallbackNextPrayer: Prayer,
            initialCountdownMillis: Long,
        ) {
            val dayAnchor = times.firstOrNull()?.timestamp ?: return
            countdownJob?.cancel()
            _liveCountdown.value =
                LivePrayerCountdown(
                    nextPrayer = fallbackNextPrayer,
                    countdownMillis = initialCountdownMillis,
                )
            val dayAnchorTimestamp = dayAnchor
            lastWidgetPrayer = fallbackNextPrayer
            countdownJob =
                viewModelScope.launch {
                    var dayRefreshTriggered = false
                    val wallAnchor = System.currentTimeMillis()
                    val elapsedAnchor = SystemClock.elapsedRealtime()
                    var nextTickElapsed = elapsedAnchor
                    val cityTz = TimeZone.getTimeZone(timezone)
                    while (isActive) {
                        // Monotonic extrapolation avoids NTP micro-jumps skewing the 1s countdown UI.
                        val now = wallAnchor + (SystemClock.elapsedRealtime() - elapsedAnchor)
                        if (
                            !dayRefreshTriggered &&
                            needsCityDayRefresh(dayAnchorTimestamp, timezone, now)
                        ) {
                            dayRefreshTriggered = true
                            refreshTimesForNewDay()
                            return@launch
                        }
                        val nextPrayer = PrayerTimeCalculator.getNextPrayer(times, now, cityTz) ?: fallbackNextPrayer
                        if (nextPrayer != lastWidgetPrayer) {
                            lastWidgetPrayer = nextPrayer
                            widgetUpdater.requestImmediateUpdate()
                        }
                        _liveCountdown.value =
                            LivePrayerCountdown(
                                nextPrayer = nextPrayer,
                                countdownMillis =
                                    PrayerTimeCalculator.getCountdownToNext(
                                        times,
                                        now,
                                        cityTz,
                                    ),
                            )
                        nextTickElapsed += 1_000L
                        delay((nextTickElapsed - SystemClock.elapsedRealtime()).coerceAtLeast(0L))
                    }
                }
        }

        private fun needsCityDayRefresh(
            dayAnchorTimestamp: Long,
            timezone: String,
            now: Long = System.currentTimeMillis(),
        ): Boolean =
            PrayerTimeCalculator.needsPrayerDayRefresh(
                dayAnchorTimestamp,
                now,
                TimeZone.getTimeZone(timezone),
            )

        private fun todayHijriDate(timezone: String): HijriDate {
            val cal = Calendar.getInstance(TimeZone.getTimeZone(timezone))
            return HijriCalculator.gregorianToHijri(
                cal[Calendar.YEAR],
                cal[Calendar.MONTH] + 1,
                cal[Calendar.DAY_OF_MONTH],
            )
        }

        private fun upcomingEvent(timezone: String): UpcomingEvent? {
            val todayHijri = todayHijriDate(timezone)
            return HijriCalculator.nextUpcomingEvent(todayHijri)
        }

        private fun stopCountdownTicker() {
            countdownJob?.cancel()
            countdownJob = null
            lastWidgetPrayer = null
            _liveCountdown.value = null
        }

        /**
         * DataStore cleared to null. Keep UI the ViewModel already set for intentional clears
         * ([PrayerTimesUiState.NoCity] / [PrayerTimesUiState.FetchError]); only drive NoCity for
         * unexpected external clears while times were still shown.
         */
        private fun onCityConfigCleared() {
            currentConfig = null
            stopCountdownTicker()
            when (_uiState.value) {
                is PrayerTimesUiState.NoCity,
                is PrayerTimesUiState.FetchError,
                -> Unit
                else -> _uiState.value = PrayerTimesUiState.NoCity
            }
        }

        private suspend fun clearCityConfig() {
            repository.clearCityConfig()
        }

        override fun onCleared() {
            stopCountdownTicker()
            super.onCleared()
        }
    }
