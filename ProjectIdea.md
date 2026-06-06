Application Creation Playbook - Prayer Time Widget
This is an engineering guide specific to the Prayer Time Widget project.

It has been adapted from a general template to suit the nature of this project: a lightweight, specialized Android application that operates without GPS and features a highly specific set of functionalities. An Incident Log has been designed to help prevent future issues.

0) Purpose of this playbook (For this project)
To build the Prayer Time Widget application with absolute integrity:

Honest Scope: No GPS, no additional features (such as *Adhkar*, Quran, or Qibla direction).

Fixed Architectural Boundaries: Complete separation between the User Interface, time calculation logic, local storage, and notifications.

Handling Inevitable Errors: Defining the application's behavior in scenarios involving a lack of internet connectivity, data retrieval failures, or invalid city names.

Testing Protocols: Establishing a mechanism to verify time calculations—including Daylight Saving Time adjustments—while offline.

Release Discipline: Building a single, lightweight, and clean APK.

Avoid:

Feature Creep.

Battery Drain.

Excessive Documentation.

1) Pre-build phase (For this application)
1.1 First Vertical Slice
The Slice: Entering a city name and displaying the six daily prayer times on the main screen (excluding the actual Widget or notifications).

Input: The user types "Damascus, Syria" and taps "Save."

Core Process: The application utilizes a prayer time calculation library (such as the *Prayer Times Library* or the *Aladhan API*—serving as the initial option, with caching enabled) to fetch or calculate the day's prayer times based on the specified city and time zone.

Error Surface: Invalid city name, lack of internet connectivity, or API failure.

Output: Displaying a user interface containing "Damascus, Syria" and a table listing the 6 prayer times. 1.2 Trust Boundaries
Isolated Context (Sandbox): User Interface (Compose), Widget, WorkManager, AlarmManager, Database (Room).

Network Context: Only when fetching city data for the first time or during the daily update.

Privileged Operations: AlarmManager for triggering the Adhan (requires the `SCHEDULE_EXACT_ALARM` permission on Android 12+).

Fallback Behavior: If the permission is denied, the app displays a notification indicating that it will use an approximate alarm.

User Message: "To enable precise Adhan timing, go to Settings and grant 'Precise Alarm' permissions."

1.3 Quality Gate: Pre-Widget Implementation
Prerequisites before starting the Widget:

A "green" Smoke Gate for the core application (no build or type errors).

The time calculation logic runs for 7 consecutive days (simulated) without errors.

The "City not found" error is handled gracefully, displaying a clear message rather than causing a crash.

The database correctly saves and restores the entry for "Damascus, Syria."


2) Architecture Lessons (for Android/Kotlin)
2.1 Contracts First
City Storage Contract:

kotlin
data class CityConfig(
val cityName: String, // "Damascus"
val countryCode: String, // "SY"
val timezone: String // "Asia/Damascus"
)
Prayer Times Result Contract:

kotlin
sealed class PrayerTimesResult {
data class Success(val times: List<PrayerTime>, val nextPrayer: Prayer, val countdown: Long) : PrayerTimesResult()
data class Error(val type: ErrorType) : PrayerTimesResult()
}
enum class ErrorType { NETWORK, CITY_NOT_FOUND, INVALID_RESPONSE, UNKNOWN }
2.2 Explicit Result Objects
Every time calculation or data fetching operation returns a `PrayerTimesResult`. The UI handles only `Success` or `Error` states, rather than unexpected `Exception`s.

2.3 Error Normalization Strategy
System/Library Error	Stable Code	User Message (UI)
UnknownHostException	NETWORK	"No internet connection. Times will be updated later."
404 from API or Search Failure	CITY_NOT_FOUND	"City not found. Please check the name or try 'Country, City'."
JSON parsing error	INVALID_RESPONSE	"An error occurred with the prayer data. Retrying automatically."
2.4 Testable Domain Logic
Example: The logic for calculating the next prayer and the countdown resides in `PrayerTimeCalculator`, not within an `Activity` or `ViewModel`.

2.7 User Data Storage Pattern (Machine-ID Shield – Modified)
Here, the sensitive data consists of the "City" and "Adhan Preferences" (audio vs. notification). This does not require complex encryption.

Storage: `DataStore` for user preferences (City, Audio, Adhan Time). Database: Room, used to store prayer times for the last 7 days (enabling display within the Widget even without an internet connection).

2.11 Maintaining the Knowledge Graph
This is a small project (fewer than 50 Kotlin files). Therefore, we apply the principle of "direct navigation" rather than relying on complex tools:

Structure: ui/ (MainActivity, Widget), data/ (Repository, LocalDatasource, RemoteDatasource), domain/ (PrayerCalculator), utils/ (TimeUtils).

Rule: If the project exceeds 60 files, a graph visualization tool—such as Graphify (or a Kotlin equivalent like Structure or ClassGraph)—will be introduced.



3) Scope Control Lessons (Very Strict Here)
3.1 Scope Freeze During Stabilization
When an issue arises (e.g., an error in time calculation within the +3 time zone), all work on the Widget or new interfaces is immediately frozen.
Only the following are permitted:

Bug fixes.

Contract hardening.

Testing.

3.2 Feature Status Table
Feature | Status | Notes
Six Daily Prayer Times + Shuruq (No GPS) | **Implemented** | Wizard → Aladhan (Umm al-Qura + Shafi) + adhan-java fallback → 6 times. Hameln/Berlin verified.
| Countdown to Next Prayer | **Implemented** | Live 1s ticker, wraps to tomorrow's first prayer after Isha, midnight-safe. |
Change City | **Implemented** | "Change" button → clear DataStore → re-enter city.
| Offline Support | **Implemented** | Offline-only default (`offline_only=true`), About toggle, adhan-java local calc, Room cache; API is optional legacy mode. |
Full-Audio Adhan Notification | **Planned** | Phase 2.
Automatic Daylight Saving Time Adjustment | **Planned** | Relies on `java.time.ZoneId`.
Widget | **Planned** | Phase 3.
Hijri Calendar and Events | **Planned** | Phase 4.
GPS / Qibla / Quran | **Out of Scope (Forever)** | These features will never be added.
4) Testing Strategy Lessons
4.1 The Minimum Testing Ladder
Contract Tests: Is the `CityConfig` I save identical to the one I read back? (DataStore test).

Helper Tests: Does `PrayerTimeCalculator.getNextPrayer(...)` return the correct prayer if the current time is 13:00 and Asr is at 13:08?

Critical Tests: What happens if the API returns an incomplete schedule (e.g., Maghrib is missing)?

4.2 Testing Common Failure Points

**Time Zones (device ≠ city):** Resolved Phase 2E — day refresh uses city `timezone` (`needsPrayerDayRefresh`), not `Calendar.getInstance()` device local midnight. See `PrayerTimeCalculatorTest` 2E.2.

Midnight: Does the countdown crash when only 0 seconds remain until the next prayer?

4.3 Fail Fast on Invalid Response
In `PrayerTimesRemoteDataSource`:

kotlin
if (!json.has("data") || !json.getJSONObject("data").has("timings")) {
return PrayerTimesResult.Error(ErrorType.INVALID_RESPONSE)
}
Never allow an empty or malformed response to reach the UI.





5) Documentation Discipline Lessons
5.1 Documentation as Part of Product Correctness
Any document stating, "The application displays the Hijri date"—while the application does not actually display it—is treated as a bug.

Update: Status updated to reflect actual progress — all features currently Planned.

5.2 Stabilization Checklist
City: The name supports both Arabic and English characters.

Prayer Times: Synchronized with the device's actual system time (whether set manually or automatically).

Adhan: Does it function when the device is in "Do Not Disturb" mode? (It *must* function).

Events: Does it correctly display Eid (1st of Shawwal) exactly 30 days after the start of Ramadan? (Simulated test).

5.3 Verification Instructions Requirement
After making any changes or completing a phase, the agent must provide explicit instructions to the user on how to verify those changes (in English).

6) Commit and PR Discipline Lessons
6.1 One Change Per Commit
feat: add initial City input screen with basic validation (Good)

fix: adjust DST transition for Cairo timezone (Good)

cleanup: move hardcoded strings to resources, fix widget layout, update deps (Bad — three distinct changes in a single commit)

6.4 Bundle Your Pushes
Do not push every tiny commit individually (e.g., "fix typo", "fix typo2"). Instead, group them into a logical bundle: use `git rebase -i`, then push.

6.5 Branching Strategy
After the initial project setup push, all subsequent code modifications and features must be developed on a new git branch. Direct pushes to the `main` branch are forbidden.

6.6 Merge Rules
- The branch must pass the full verification suite `./scripts/smoke-ci.sh` successfully with zero errors.
- The branch must not introduce any features that are out of scope (like GPS or Adhkar).
- The agent is forbidden from merging their own PR/branch without explicit approval and manual sign-off from the user.
- The Phased Plan (PHASED_PLAN.md) and Feature Table (APP_CREATION_PLAYBOOK.md) must be updated to reflect the completed state before merging.
- Any resolved bugs must be added to the Incident Log before the branch is merged.



7) Platform-Specific Hardening (Android Only)
7.1 Host Constraints (Android)
Android Version: Minimum API level 23 (Android 6.0) to ensure support for runtime permissions. Target API level: 34.

Permissions:

INTERNET: For initial data fetching and daily updates.

POST_NOTIFICATIONS (Android 13+): For displaying notifications.

SCHEDULE_EXACT_ALARM (Android 12+): For precise Adhan scheduling.

Absolutely *no* ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION permissions are required or requested. 7.5 Safety vs. Convenience (for Destructive Actions)
Delete City: When attempting to delete the currently entered city, an alert appears: "This will erase all prayer times and settings, and return you to the Welcome screen." (Confirmation button only).

7.A) Android-Specific Enhancements (No Linux here, but general principles apply)
WorkManager: Utilize `PeriodicWorkRequest` once daily (the minimum allowed interval is 15 minutes, but we use `setMinimumInterval(1, TimeUnit.DAYS)`).

AlarmManager: Utilize `setExactAndAllowWhileIdle()` for alarms to ensure the Adhan triggers reliably, even when the device is in power-saving mode.

Widget: Update via WorkManager every 30 minutes (or immediately upon a prayer time change using `updateAppWidget`).

9) Case Studies — Incident Log (Project-Specific)
Incident Entry Template
Any actual issues encountered during development will be recorded here.

Incident Log
Default Reliance on GPS (CRITICAL)
Date: (Default)

Area: Design / Requirements

Symptom: Initial documentation stated "approximate location for prayer."

Root Cause: The mindset that "all prayer apps use GPS."

Impact: A direct violation of project requirements ("never use GPS").

Preventive Action: The playbook was updated to emphasize: No libraries requiring location permissions are to be imported. `ACCESS_FINE_LOCATION` has been added to the list of permanently disallowed permissions within `AndroidManifest.xml`.

Status: Resolved (by discarding the concept).

Daily Update Failure Due to Doze Mode (Android)
Area: WorkManager / Updates

Symptom: One day after installing the app, the prayer times became outdated (updates failed to occur).

Root Cause: WorkManager utilized `setMinimumInterval(1, TimeUnit.DAYS)`, but the device subsequently entered Doze Mode, causing the scheduled update to be deferred. Fix: Use `setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)` and accept that the update may be slightly delayed. Add logic upon app launch: "If the last fetch occurred more than 25 hours ago, perform a new fetch immediately."

Preventive Measure: Any scheduled work (`Work`) must include fallback logic to be triggered when the user interacts with the UI.

Status: Monitoring

Component ID Mismatch (State Key Mismatch)
Area: User Interface / Widget

Symptom: The Widget was displaying the previous prayer time instead of the current one.

Root Cause: The `ViewModel` uses the enum class `Prayer { FAJR, DHUHR... }`, whereas the Widget reads text strings ("Fajr", "Dhuhr") directly from the database.

Fix: Use the standardized name `Prayer.FAJR.name` in both the database and the Widget.

Preventive Measure: Any enum stored in the database must use `enumClass.name`, rather than `toString()` or a custom string.

Status: Resolved

Daylight Saving Time (DST) Time Zone Disregard
Area: Time Calculation

Symptom: On a specific spring day, a user's app in London displayed the "Dhuhr" prayer time as being one hour late.

Root Cause: We used `System.currentTimeMillis()`, but the time calculation relied on UTC instead of `Europe/London`.

Fix: Pass `ZoneId.systemDefault()` to the time calculation library.

Preventive Measure: Mandate unit tests for the `Europe/London` time zone specifically on the date of the transition to Daylight Saving Time (e.g., March 31st).

Status: Monitoring (Test added).



10) Maintenance Rule for This Playbook
When a new issue arises (e.g., a bug in the Hijri date calculation):

1. Add a new entry to the Incident Log.

2. Add a new rule or update the checklist (e.g., "Test Hijri dates for the entire month of Ramadan").

3. Link the remediation guide.

4. Maintain clear, technical language—either Arabic or English (avoiding marketing jargon).

This document serves as the living engineering memory for the *Prayer Time Widget* application.

8) Reusable Build Sequence (for this project)
1. Define the Vertical Slice: City Input Screen + Prayer Times Display.

2. Define Target Platform: Android (API 23+).

3. Trust Boundaries: UI, Data Layer, Alarm, Widget.

4. Contracts: `CityConfig` data class, `PrayerTimesResult` sealed class.

5. Implement UI: Compose (`TextField`, `LazyColumn` for prayer times).

6. Implement Backend: `PrayerTimesRepository` — adhan-java local calc as default; Aladhan API is optional legacy mode. Room cache 7 days.

7. Error Normalization: `NetworkMapper` converts exceptions into `ErrorType` instances.

8. User-facing errors: Display a `Snackbar` to the user.

9. Tests: `PrayerTimesCalculatorTest` (for time calculations).

10. Truthful Docs: Update the `README` to reflect what has actually been implemented.

11. Freeze Scope: Before adding the Widget, ensure that the core application remains stable and does not crash.

12. Migration: Add the Hijri calendar and special occasions (as an enhancement only, without altering core features).

The game plan has been updated according to your preferences: No GPS usage, no constant internet requirement, no feature bloat, lightweight size, and support for the Hijri calendar and special occasions within the widget.

---

## Critical Agent Verification Directive

- **Thorough Verification Mindset:** As you are an expert in computing, programming, software engineering, and application development, your current mission is to write a report and check the entire existing application to determine whether everything these claims represent is sound, or if there remain any gaps, risks, deficiencies, missing components, voids, spurious data, static data, or mock data - do not overlook any matters simply because you deem them trivial or simple. Do not believe the files until you check whether they are right or not.