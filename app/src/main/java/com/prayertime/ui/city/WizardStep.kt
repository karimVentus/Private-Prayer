package com.prayertime.ui.city

import com.prayertime.domain.model.Country

sealed class WizardStep {
    data object CountrySelection : WizardStep()

    data class CitySelection(val country: Country) : WizardStep()
}
