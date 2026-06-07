package com.prayertime.ui.screens

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import com.prayertime.R
import com.prayertime.domain.model.Country
import com.prayertime.domain.model.HijriDate
import com.prayertime.domain.model.Prayer
import com.prayertime.domain.model.PrayerTime
import com.prayertime.domain.model.PrayerTimesResult
import com.prayertime.ui.city.CityInputActions
import com.prayertime.ui.city.CityInputUiState
import com.prayertime.ui.city.WizardStep
import com.prayertime.ui.prayer.PrayerTimesActions
import com.prayertime.ui.theme.PrayerTimeTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.AfterClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ComposeScreenSmokeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @After
    fun drainMainLooper() {
        composeRule.waitForIdle()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    }

    @Test
    fun prayerTimesScreen_showsCityAndFajrRow() {
        val times =
            listOf(
                PrayerTime(Prayer.FAJR, "05:00", 1_700_000_000_000L),
                PrayerTime(Prayer.SHURUQ, "06:30", 1_700_005_400_000L),
                PrayerTime(Prayer.DHUHR, "12:30", 1_700_027_000_000L),
                PrayerTime(Prayer.ASR, "15:45", 1_700_038_700_000L),
                PrayerTime(Prayer.MAGHRIB, "18:10", 1_700_047_400_000L),
                PrayerTime(Prayer.ISHA, "19:40", 1_700_052_800_000L),
            )
        composeRule.setContent {
            PrayerTimeTheme {
                PrayerTimesScreen(
                    city = "Hameln, DE",
                    todayHijriDate = HijriDate(1447, 9, 1),
                    upcomingEvent = null,
                    result = PrayerTimesResult.Success(times, Prayer.DHUHR, 3_600_000L),
                    liveCountdownFlow = MutableStateFlow(null),
                    offlineOnly = true,
                    actions =
                        PrayerTimesActions(
                            onChangeCity = {},
                            onCalendar = {},
                            onQibla = {},
                            onAbout = {},
                            onLanguage = {},
                            onToggleMute = {},
                        ),
                )
            }
        }
        composeRule.onNodeWithText("Hameln, DE").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.fajr)).assertIsDisplayed()
    }

    @Test
    fun cityInputScreen_showsCountryStepTitle() {
        composeRule.setContent {
            PrayerTimeTheme {
                CityInputScreen(
                    state =
                        CityInputUiState(
                            wizardStep = WizardStep.CountrySelection,
                            countrySearchQuery = "",
                            citySearchQuery = "",
                            filteredCountries = listOf(Country("Germany", "DE")),
                            filteredCities = emptyList(),
                            showCustomCityFallback = false,
                            catalogReady = true,
                        ),
                    actions =
                        CityInputActions(
                            onCountrySearchQueryChanged = {},
                            onCitySearchQueryChanged = {},
                            selectCountry = {},
                            clearSelectedCountry = {},
                            saveCity = {},
                        ),
                )
            }
        }
        composeRule.onNodeWithText(context.getString(R.string.select_country)).assertIsDisplayed()
        composeRule.onNodeWithText("Germany").assertIsDisplayed()
    }

    @Test
    fun hijriCalendarScreen_rendersMonthlyTab() {
        composeRule.setContent {
            PrayerTimeTheme {
                HijriCalendarScreen(
                    timezone = "Europe/Berlin",
                    onClose = {},
                )
            }
        }
        composeRule.onNodeWithText(context.getString(R.string.calendar_tab_monthly)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.calendar_day_fri)).assertIsDisplayed()
    }

    @Test
    fun languagePickerDialog_showsArabicLabel_inRtlLayout() {
        val arabicLabel = context.getString(R.string.language_arabic)
        composeRule.setContent {
            PrayerTimeTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    LanguagePickerDialog(
                        currentTag = "en",
                        onDismiss = {},
                        onSelect = {},
                    )
                }
            }
        }
        composeRule.onNodeWithText(arabicLabel).assertIsDisplayed()
        composeRule
            .onNodeWithText(context.getString(R.string.language_dialog_close))
            .assertIsDisplayed()
    }

    companion object {
        @AfterClass
        @JvmStatic
        fun afterClass() {
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        }
    }
}
