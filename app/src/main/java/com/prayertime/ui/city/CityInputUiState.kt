package com.prayertime.ui.city

import com.prayertime.domain.model.Country

data class CityInputUiState(
    val wizardStep: WizardStep,
    val countrySearchQuery: String,
    val citySearchQuery: String,
    val filteredCountries: List<Country>,
    val filteredCities: List<String>,
    val showCustomCityFallback: Boolean,
    val catalogReady: Boolean,
)

data class CityInputActions(
    val onCountrySearchQueryChanged: (String) -> Unit,
    val onCitySearchQueryChanged: (String) -> Unit,
    val selectCountry: (Country) -> Unit,
    val clearSelectedCountry: () -> Unit,
    val saveCity: (String) -> Unit,
)
