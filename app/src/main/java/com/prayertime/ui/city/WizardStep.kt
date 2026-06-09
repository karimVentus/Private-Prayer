package com.prayertime.ui.city

import com.prayertime.domain.model.Country

sealed class WizardStep {
    data object CountrySelection : WizardStep()

    data class CitySelection(val country: Country) : WizardStep()

    data class ManualCoords(
        val country: Country,
        val cityName: String,
        val defaultTimezone: String,
    ) : WizardStep()
}
