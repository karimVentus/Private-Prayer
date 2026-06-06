package com.prayertime.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prayertime.R
import com.prayertime.domain.model.Country
import com.prayertime.ui.city.CityInputActions
import com.prayertime.ui.city.CityInputUiState
import com.prayertime.ui.city.WizardStep
import com.prayertime.ui.components.AppTextButton
import com.prayertime.ui.theme.AppSpacing

@Composable
fun CityInputScreen(
    state: CityInputUiState,
    actions: CityInputActions,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        BismillahHeader()
        when (val step = state.wizardStep) {
            is WizardStep.CountrySelection -> {
                CountrySelectionStep(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    searchQuery = state.countrySearchQuery,
                    countries = state.filteredCountries,
                    catalogReady = state.catalogReady,
                    onSearchQueryChanged = actions.onCountrySearchQueryChanged,
                    onCountrySelected = actions.selectCountry,
                )
            }
            is WizardStep.CitySelection -> {
                CitySelectionStep(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    country = step.country,
                    searchQuery = state.citySearchQuery,
                    cities = state.filteredCities,
                    catalogReady = state.catalogReady,
                    showCustomCityFallback = state.showCustomCityFallback,
                    onSearchQueryChanged = actions.onCitySearchQueryChanged,
                    onBack = actions.clearSelectedCountry,
                    onCitySelected = actions.saveCity,
                )
            }
        }
    }
}

@Composable
private fun CountrySelectionStep(
    searchQuery: String,
    countries: List<Country>,
    catalogReady: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onCountrySelected: (Country) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = AppSpacing.screenHorizontal),
    ) {
        Text(
            text = stringResource(R.string.select_country),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(AppSpacing.sectionGap))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            label = { Text(stringResource(R.string.search_countries)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = searchFieldColors(),
        )
        Spacer(modifier = Modifier.height(AppSpacing.itemGap))
        LazyColumn(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
        ) {
            when {
                !catalogReady -> {
                    item(key = "countries-loading") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.sectionGap),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                countries.isEmpty() -> {
                    item(key = "countries-empty") {
                        CatalogEmptyHint(
                            message =
                                if (searchQuery.isBlank()) {
                                    stringResource(R.string.no_countries_loaded)
                                } else {
                                    stringResource(R.string.no_countries_match)
                                },
                        )
                    }
                }
                else -> {
                    items(countries, key = { "${it.code}-${it.name}" }) { country ->
                        CountryItem(country = country, onClick = { onCountrySelected(country) })
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogEmptyHint(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.sectionGap),
    )
}

@Composable
private fun searchFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cursorColor = MaterialTheme.colorScheme.primary,
    )

@Composable
private fun CountryItem(
    country: Country,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = AppSpacing.listItemVertical)
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AppSpacing.cardPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = country.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = country.code,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CitySelectionStep(
    country: Country,
    searchQuery: String,
    cities: List<String>,
    catalogReady: Boolean,
    showCustomCityFallback: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onBack: () -> Unit,
    onCitySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = AppSpacing.screenHorizontal),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppTextButton(onClick = onBack) {
                Text(stringResource(R.string.back))
            }
            Spacer(modifier = Modifier.weight(1f))
        }
        Text(
            text = country.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(AppSpacing.sectionGap))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            label = { Text(stringResource(R.string.search_cities_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = searchFieldColors(),
        )
        Spacer(modifier = Modifier.height(AppSpacing.itemGap))
        LazyColumn(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
        ) {
            when {
                !catalogReady -> {
                    item(key = "cities-loading") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.sectionGap),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                else -> {
                    if (showCustomCityFallback) {
                        item(key = "custom-city-fallback") {
                            CustomCityFallback(
                                query = searchQuery,
                                onClick = { onCitySelected(searchQuery.trim()) },
                            )
                        }
                    }
                    if (cities.isEmpty() && !showCustomCityFallback) {
                        item(key = "cities-empty") {
                            CatalogEmptyHint(
                                message =
                                    if (searchQuery.isBlank()) {
                                        stringResource(R.string.no_cities_loaded)
                                    } else {
                                        stringResource(R.string.no_cities_match)
                                    },
                            )
                        }
                    }
                    items(cities, key = { "${country.code}-$it" }) { city ->
                        CityItem(city = city, onClick = { onCitySelected(city) })
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomCityFallback(
    query: String,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = AppSpacing.listItemVertical)
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AppSpacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.use_city, query),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CityItem(
    city: String,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = AppSpacing.listItemVertical)
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AppSpacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = city,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
