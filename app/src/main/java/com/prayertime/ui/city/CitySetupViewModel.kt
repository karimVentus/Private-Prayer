package com.prayertime.ui.city

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prayertime.data.local.AppPreferencesDataSource
import com.prayertime.data.repository.PrayerTimesRepository
import com.prayertime.domain.model.CityConfig
import com.prayertime.domain.model.CityListItem
import com.prayertime.domain.model.CityResolutionResult
import com.prayertime.domain.model.Country
import com.prayertime.domain.model.SaveCityError
import com.prayertime.domain.model.SaveCityResult
import com.prayertime.domain.repository.LocationRepository
import com.prayertime.domain.usecase.SearchLocationsUseCase
import com.prayertime.ui.PrayerTimesErrorMapper
import com.prayertime.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CitySetupViewModel
    @Inject
    constructor(
        private val repository: PrayerTimesRepository,
        private val locationRepository: LocationRepository,
        private val searchLocations: SearchLocationsUseCase,
        private val preferences: AppPreferencesDataSource,
        private val widgetUpdater: WidgetUpdater,
    ) : ViewModel() {
        private val _wizardStep = MutableStateFlow<WizardStep>(WizardStep.CountrySelection)
        val wizardStep: StateFlow<WizardStep> = _wizardStep.asStateFlow()

        private val _countrySearchQuery = MutableStateFlow("")
        val countrySearchQuery: StateFlow<String> = _countrySearchQuery.asStateFlow()

        private val _citySearchQuery = MutableStateFlow("")
        val citySearchQuery: StateFlow<String> = _citySearchQuery.asStateFlow()

        private val _isSaving = MutableStateFlow(false)
        val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

        private val _saveError = MutableStateFlow<Int?>(null)
        val saveError: StateFlow<Int?> = _saveError.asStateFlow()

        private val _catalogReady = MutableStateFlow(locationRepository.isCatalogLoaded())
        val catalogReady: StateFlow<Boolean> = _catalogReady.asStateFlow()

        val filteredCountries: StateFlow<List<Country>> =
            combine(_countrySearchQuery, _catalogReady, preferences.appLanguageTag) { query, ready, languageTag ->
                if (ready) searchLocations.filterCountries(query, languageTag) else emptyList()
            }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        val filteredCities: StateFlow<List<CityListItem>> =
            combine(_wizardStep, _citySearchQuery, _catalogReady, preferences.appLanguageTag) { step, query, ready, languageTag ->
                if (!ready || step !is WizardStep.CitySelection) {
                    emptyList()
                } else {
                    searchLocations.filterCities(step.country.code, query, languageTag)
                }
            }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        val showCustomCityFallback: Boolean
            get() = searchLocations.showCustomCityFallback(_citySearchQuery.value)

        init {
            viewModelScope.launch {
                locationRepository.awaitReady()
                _catalogReady.value = locationRepository.isCatalogLoaded()
            }
        }

        fun onCountrySearchQueryChanged(query: String) {
            _countrySearchQuery.value = query
        }

        fun onCitySearchQueryChanged(query: String) {
            _citySearchQuery.value = query
        }

        fun selectCountry(country: Country) {
            _wizardStep.value = WizardStep.CitySelection(country)
            _citySearchQuery.value = ""
        }

        fun clearSelectedCountry() {
            _wizardStep.value = WizardStep.CountrySelection
            _countrySearchQuery.value = ""
        }

        fun resetWizard() {
            _wizardStep.value = WizardStep.CountrySelection
            _countrySearchQuery.value = ""
            _citySearchQuery.value = ""
        }

        fun consumeSaveError() {
            _saveError.value = null
        }

        fun saveCity(cityName: String) {
            val step = _wizardStep.value
            if (step !is WizardStep.CitySelection) return
            val name = cityName.trim()
            if (name.isBlank()) return

            viewModelScope.launch {
                _isSaving.value = true
                locationRepository.awaitReady()
                val canonical =
                    locationRepository.resolveCanonicalCityName(step.country.code, name)
                val offlineOnly = repository.offlineOnly.first()
                val draft =
                    when (val resolution = locationRepository.resolveCityCoordinates(step.country.code, canonical)) {
                        is CityResolutionResult.Found ->
                            CityConfig(
                                cityName = canonical,
                                countryCode = step.country.code,
                                timezone = resolution.coords.timezone,
                                latitude = resolution.coords.latitude,
                                longitude = resolution.coords.longitude,
                            )
                        is CityResolutionResult.Fallback ->
                            if (offlineOnly) {
                                _saveError.value =
                                    PrayerTimesErrorMapper.saveError(SaveCityError.CITY_NOT_FOUND, offlineOnly)
                                _isSaving.value = false
                                return@launch
                            } else {
                                CityConfig(
                                    cityName = canonical,
                                    countryCode = step.country.code,
                                    timezone = resolution.coords.timezone,
                                    latitude = resolution.coords.latitude,
                                    longitude = resolution.coords.longitude,
                                )
                            }
                        is CityResolutionResult.InvalidCountry -> {
                            _saveError.value =
                                PrayerTimesErrorMapper.saveError(SaveCityError.INVALID_COUNTRY, offlineOnly)
                            _isSaving.value = false
                            return@launch
                        }
                    }
                when (val result = repository.saveCityConfig(draft)) {
                    is SaveCityResult.Success -> widgetUpdater.requestImmediateUpdate()
                    is SaveCityResult.Error -> {
                        _saveError.value = PrayerTimesErrorMapper.saveError(result.type, offlineOnly)
                    }
                }
                _isSaving.value = false
            }
        }

        fun requestManualCoords(cityName: String) {
            val step = _wizardStep.value
            if (step !is WizardStep.CitySelection) return
            val canonical =
                locationRepository.resolveCanonicalCityName(step.country.code, cityName.trim())
            val defaultTz =
                when (val resolution = locationRepository.resolveCityCoordinates(step.country.code, canonical)) {
                    is CityResolutionResult.Found -> resolution.coords.timezone
                    is CityResolutionResult.Fallback -> resolution.coords.timezone
                    is CityResolutionResult.InvalidCountry -> "UTC"
                }
            _wizardStep.value =
                WizardStep.ManualCoords(
                    country = step.country,
                    cityName = canonical.ifBlank { cityName.trim() },
                    defaultTimezone = defaultTz,
                )
        }

        fun saveManualCoords(
            cityName: String,
            latitudeText: String,
            longitudeText: String,
            timezone: String,
        ) {
            val step = _wizardStep.value as? WizardStep.ManualCoords ?: return

            viewModelScope.launch {
                val lat = latitudeText.trim().toDoubleOrNull()
                val lng = longitudeText.trim().toDoubleOrNull()
                val tz = timezone.trim()
                val offlineOnly = repository.offlineOnly.first()

                if (lat == null || lng == null || tz.isBlank()) {
                    _saveError.value =
                        PrayerTimesErrorMapper.saveError(SaveCityError.INVALID_COORDINATES, offlineOnly)
                    return@launch
                }
                if (!isValidCoordinateRange(lat, lng)) {
                    _saveError.value =
                        PrayerTimesErrorMapper.saveError(SaveCityError.INVALID_COORDINATES, offlineOnly)
                    return@launch
                }

                _isSaving.value = true
                val draft =
                    CityConfig(
                        cityName = cityName.trim().ifBlank { step.cityName },
                        countryCode = step.country.code,
                        timezone = tz,
                        latitude = lat,
                        longitude = lng,
                    )
                when (val result = repository.saveCityConfig(draft)) {
                    is SaveCityResult.Success -> widgetUpdater.requestImmediateUpdate()
                    is SaveCityResult.Error -> {
                        _saveError.value =
                            PrayerTimesErrorMapper.saveError(result.type, offlineOnly)
                    }
                }
                _isSaving.value = false
            }
        }

        fun backFromManualCoords() {
            val step = _wizardStep.value as? WizardStep.ManualCoords ?: return
            _wizardStep.value = WizardStep.CitySelection(step.country)
        }

        private fun isValidCoordinateRange(
            latitude: Double,
            longitude: Double,
        ): Boolean = latitude in -90.0..90.0 && longitude in -180.0..180.0
    }
