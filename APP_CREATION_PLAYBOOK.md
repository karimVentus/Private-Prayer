# Hayya (حيا) — Application Creation Playbook

> **This is an engineering guide for Hayya (حيا):** a lightweight, city-based Android prayer-times app with a fixed shipped feature set (Phases 0–7A). Package **`com.prayertime`** unchanged. The incident log contains case studies from real development that illustrate each principle in practice. Update it whenever a new problem appears.

---

## 0) Purpose of this playbook

Build the Hayya application with absolute integrity:

- [x] **Honest Scope:** Shipped feature set complete (Phases **0–7A**). New work requires an explicit phase in `PHASED_PLAN.md`.
- [ ] **Fixed Architectural Boundaries:** Complete separation between UI, time calculation logic, local storage, and notifications.
- [ ] **Deterministic Error Handling:** Defining behavior for scenarios involving lack of internet connectivity, data retrieval failures, or invalid city names.
- [ ] **Test Evidence:** Verifying time calculations — including DST adjustments — while offline.
- [x] **Release Discipline:** Signed R8 release APK/AAB (~12 MB offline); `scripts/release-gate.sh` ≤ 13 MB; see README Release section.

Avoid:

- [ ] Feature creep before a stable vertical slice
- [ ] Battery drain
- [ ] Excessive documentation
- [ ] Untested integration paths

---

## 1) Pre-build phase (before writing features)

### 1.1 Define the first vertical slice

**The Slice:** Selecting a country and city via a two-step wizard, and displaying the six daily prayer times on the main screen (excluding Widget or notifications).

- [x] **Input:** The user selects a country and city from a scrollable list with substring-filtering, or types a custom city name.
- [x] **Core operation:** On save: Aladhan geocode (`meta`) → lat/lon/tz → DataStore. On fetch: Aladhan timings first (Umm al-Qura + Shafi); adhan-java fallback → 6 times → Room cache (replace-by-date).
- [x] **Error surface:** Invalid city name (404 → geocode fails → SaveCityResult.Error → Snackbar, city not persisted). Network error (geocode fails → same path). Transient error on fetch → cached data or local calc. No network + no cache → Error message.
- [x] **Output:** UI with city name header + "Change" button + "Next: X in Yh Zm" + 6 rows (Fajr, **Shuruq**, Dhuhr, Asr, Maghrib, Isha).

Rule:

- [ ] Do not build phase-wide feature breadth first.
- [ ] Build one slice deeply (UI + logic + contracts + tests + docs).

### 1.2 Declare trust boundaries early

For Android, define environments explicitly:

- [ ] **Isolated Context (Sandbox):** UI (Compose), Widget, WorkManager, AlarmManager, Database (Room).
- [ ] **Network Context:** Only when fetching city data for the first time or during the daily update.
- [ ] **Privileged Operations:** AlarmManager for triggering the Adhan (requires `SCHEDULE_EXACT_ALARM` on Android 12+).
- [ ] **Fallback Behavior:** If permission is denied, the app displays a notification indicating it will use an approximate alarm.
- [ ] **User Message:** "To enable precise Adhan timing, go to Settings and grant 'Precise Alarm' permissions."

### 1.3 Create a quality gate before roadmap expansion

Prerequisites before starting the Widget:

    - [x] Green smoke gate for the core application (`./gradlew testDebugUnitTest` BUILD SUCCESSFUL, **414** test methods).
- [x] Time calculation logic runs offline via adhan-java (no network dependency for daily calc).
- [x] "City not found" error: `SaveCityResult.Error` blocks geocode failure from persisting; Snackbar shown; NoCity race suppressed.
- [x] Database correctly saves and restores the entry for "Damascus, SY" (DataStore contract test passes).
- [x] Manual E2E: Hameln + Berlin (DE) — times match reference app.
- [x] ViewModel decomposition: `MainViewModel` (380 lines) → `CitySetupViewModel` + `PrayerTimesViewModel` + `AppSettingsViewModel` + `SearchLocationsUseCase`.
- [x] City-scoped cache: `cityKey` column + composite index; DB migration v1→v2; cache survives city switches.
- [x] Async data init: `LocationDataSource` parses JSON on `Dispatchers.IO`.
- [x] Timezone consistency: all day-boundary checks use `needsPrayerDayRefresh` with city TZ.
- [x] Online pipeline tested: `AladhanTimingsMapperTest` (18 tests) + `NetworkMapperTest` (10 tests).
- [x] Flavor isolation verified: interface + composition pattern; offline APK has no dead UI controls.
- [x] Graphify graph refreshed (`graphity.md` maintenance commands) — 2026-06-04 after Hilt DI.
- [ ] No open regressions in the vertical slice.

---

## 2) Architecture lessons (for Android/Kotlin)

### 2.1 Contracts first, implementation second

Define typed contracts at boundaries:

**City Storage Contract:**
```kotlin
data class CityConfig(
    val cityName: String, // "Damascus"
    val countryCode: String, // "SY"
    val timezone: String, // "Asia/Damascus"
    val latitude: Double, // 33.5 (from API geocode)
    val longitude: Double // 36.3 (from API geocode)
)
```

**Prayer Times Result Contract:**
```kotlin
sealed class PrayerTimesResult {
    data class Success(val times: List<PrayerTime>, val nextPrayer: Prayer, val countdown: Long) : PrayerTimesResult()
    data class Error(val type: FetchError) : PrayerTimesResult()
}
enum class FetchError { NETWORK, CITY_NOT_FOUND, INVALID_RESPONSE, MISSING_COORDINATES, UNKNOWN }

sealed class SaveCityResult {
    data class Success(val config: CityConfig) : SaveCityResult()
    data class Error(val type: SaveCityError) : SaveCityResult()
}
enum class SaveCityError { CITY_NOT_FOUND, INVALID_COUNTRY, NETWORK, UNKNOWN }
```

- [ ] Never trust raw payloads across process boundaries. Use schema validation at API/network boundaries.

### 2.2 Prefer explicit result objects for operations

Every time calculation or data fetching operation returns a `PrayerTimesResult`. The UI handles only `Success` or `Error` states, rather than unexpected `Exception`s.

Benefits:

- [ ] UI rendering logic remains deterministic
- [ ] No silent failures
- [ ] Easier to test invalid/malformed responses

### 2.3 Error normalization strategy

Convert low-level runtime errors into stable codes:

| System/Library Error | Stable Code | User Message (UI) |
|---|---|---|
| UnknownHostException | NETWORK | "No internet connection. Times will be updated later." |
| 404 from API or Search Failure | CITY_NOT_FOUND | "City not found. Please check the name or try 'Country, City'." |
| JSON parsing error | INVALID_RESPONSE | "An error occurred with the prayer data. Retrying automatically." |
| Any unexpected exception | UNKNOWN | "An unexpected error occurred. Please try again." |

- [ ] Map codes to user-safe messages in UI layer.
- [ ] Do not leak opaque stack traces as UX.

### 2.4 Keep domain logic extractable and testable

- [ ] The logic for calculating the next prayer and countdown resides in `PrayerTimeCalculator`, not within an `Activity` or `ViewModel`.
- [ ] Tests directly against helper modules without booting the full Android runtime.

### 2.5 Avoid Backend Entry-Point Bloat (The "No-Monolith" Rule)

- [ ] Any logic exceeding 50 lines should move to a dedicated module.
- [ ] Repository and ViewModel should remain lean — delegate to domain calculators.
- [ ] This preserves compile times and mental model clarity.

### 2.6 Contract-Driven Development

For every new API/network domain, dedicated schema contract tests must exist:

- [ ] Assert that the internal data class serializes to exactly what the API returns.
- [ ] Assert that the response is validated before it touches the UI.
- [ ] This prevents "Silent Failures" where the UI shows stale data because of a minor JSON key change.

### 2.7 User Data Storage Pattern (Machine-ID Shield – Modified)

Sensitive data consists of "City" and "Adhan Preferences" (audio vs. notification). This does not require complex encryption.

- [ ] **DataStore:** For user preferences (City, Audio, Adhan Time).
- [ ] **Room Database:** For storing prayer times for the last 7 days (enabling display in the Widget even without internet connection).
- [ ] No OS keychain dependency is needed at this project's scale.

### 2.8 Shell Command Construction — Direct Process Spawn Only

N/A for Android — no shell command execution is used in this application.

### 2.9 Prefer Safe Unwrapping

Never call `.unwrap()` on `Option`/nullable values that depend on prior setup configuration.

- [ ] **Example:** `config?.let { ... } ?: return` instead of `config!!`
- [ ] **Why:** A refactor removing a nullable field would silently crash; safe calls propagate cleanly.

### 2.10 Audit Claims Must Be Code-Verified Before Acting

- [ ] Grep + import analysis before deleting files or refactoring based on audit reports.

### 2.11 Architecture visibility (Graphify)

Use **Graphify** for dependency and package-boundary audits. Playbook: [`graphity.md`](graphity.md). Phase diagrams: [`PHASED_PLAN.md`](PHASED_PLAN.md).

- [ ] **Structure:**
  - `ui/` — MainActivity, ViewModels, Screens (Widget in Phase 3)
  - `data/` — Repository, Room, DataStore, AladhanApi, LocationDataSource
  - `domain/` — PrayerTimeCalculator, LocalPrayerTimeCalculator, models
  - `utils/` — TimeUtils, DateUtils
- [ ] After each phase gate: `OPENAI_API_KEY="" graphify update . --no-cluster`
- [ ] Before merge: confirm `graphify-out/graph.json` reflects current repository layout

---

## 3) Scope control lessons (Very strict here)

### 3.1 Freeze scope while stabilizing

When an issue arises (e.g., an error in time calculation within the +3 time zone), all work on the Widget or new interfaces is immediately frozen. Only allow:

- [ ] Bug fixes
- [ ] Contract hardening
- [ ] Tests
- [ ] Docs truthfulness updates

### 3.2 Track what is "implemented" vs "partial" vs "planned"

Public docs must reflect real maturity.

| Feature | Status | Notes |
|---|---|---|
| Displaying Six Daily Prayer Times | **Implemented** | Wizard → local geocode + adhan-java calc → city-scoped Room cache (cityKey). UI: Fajr, Shuruq, Dhuhr, Asr, Maghrib, Isha. Hameln/Berlin verified. |
| Privacy / offline-only (no Aladhan) | **Implemented** | `offline_only` flag (default true), About toggle (hidden on offline APK — static info card), repository guards, tests. Umlaut key mismatch fixed. |
| Countdown to the Next Prayer | **Implemented** | Live 1s ticker managed by `PrayerTimesViewModel` (not Composable); wraps to tomorrow after Isha; city-TZ midnight rollover via `needsPrayerDayRefresh`. |
| Change City | **Implemented** | "Change" clears city, returns to country wizard. City-scoped cache survives switching — Hameln→Berlin→Hameln hits cache. |
| Save-Time City Validation | **Implemented** | `SaveCityResult` blocks persistence on geocode failure. Snackbar error + clear. |
| Full-Audio Adhan Notification | **Implemented** | About toggle; `AdhanAlarmReceiver` + `res/raw/adhan.mp3`; exact/inexact alarms; boot reschedule; five fard slots via `Prayer.adhanAlarmPrayers` (**SHURUQ** display-only). |
| Per-prayer mute toggles | **Implemented (5E.22–23)** | `PrayerTimesScreen` bell icons on every row; `muted_prayers` DataStore; `AdhanAlarmReceiver` skips muted; Material outlined icons + theme tints. |
| Automatic Daylight Saving Time Adjustment | **Implemented** | `PrayerTimeCalculator.needsPrayerDayRefresh` uses `TimeZone.getTimeZone(cityTZ)` for all day-boundary checks. DST-aware mapper tests. |
| Architecture: ViewModel decomposition | **Implemented** | `MainViewModel` (380 lines) → `CitySetupViewModel` + `PrayerTimesViewModel` + `AppSettingsViewModel` + `SearchLocationsUseCase`. |
| Architecture: City-scoped cache | **Implemented** | `PrayerTimeEntity.cityKey` + composite index; Room v4 (`MIGRATION_1_2` … `MIGRATION_3_4`); cache survives city switches; `invalidateTodayCache` for manual refresh. |
| Architecture: Async data init | **Implemented** | `LocationCatalogInitializer` at startup; `LocationDataSource.initialize()` on `Dispatchers.IO`; `loadGeneration` for test isolation; `awaitReady()` for suspend callers. |
| Architecture: Timezone consistency | **Implemented** | All staleness/rollover checks use `needsPrayerDayRefresh` with city timezone — no device-clock splits. |
| Architecture: Online pipeline tests | **Implemented** | `AladhanTimingsMapperTest` (18+ tests) + `NetworkMapperTest` (10 tests); mapper in `src/main/`. |
| Architecture: Flavor isolation | **Implemented** | `PrayerTimesRepository` is interface; `OnlinePrayerTimesRepository` composes `LocalPrayerTimesRepository` + shared `PrayerTimesLocalEngine`; offline APK never shows dead toggle. |
| Architecture: Hilt DI | **Implemented** | `@HiltAndroidApp`, flavor `RepositoryModule`, `@HiltViewModel`, `@HiltWorker`; removed manual `PrayerTimeViewModelFactory` and flavor `PrayerTimeApp`. |
| Architecture: Worker & engine tests | **Implemented** | `PrayerRefreshWorkTest`, `PrayerTimeRefreshWorkerTest`, `PrayerTimesLocalEngineTest`; worker refresh/retry/skip + periodic enqueue KEEP policy. |
| Manual cache refresh | **Implemented** | Settings → **Refresh today's times** uses `fetchTodayTimes(forceRefresh)` + snackbar; `BuildConfig.VERSION_NAME` on About screen. |
| App language (EN / AR / system) | **Done (5E)** | `LanguagePickerDialog`; **Language** button on prayer header (beside **Change**); `values-ar` + AppCompat locales; RTL prayer/Settings/wizard/calendar; portrait-only app |
| Dev workflow (`./dev`) | **Implemented** | `scripts/emu` — boot emulator, `installDebug`, launch; `HEADLESS=1` for CI-style runs. |
| Architecture: LocationRepository | **Implemented** | Domain `LocationRepository`; `LocalLocationRepository`; use case no longer binds to `LocationDataSource`. |
| Architecture: Hilt network stack | **Implemented** | `NetworkModule` — `OkHttpClient`, `Retrofit`, injectable `AladhanApi` (online flavor only). |
| TLS pinning (Aladhan API) | **Done (6.8)** | `network_security_config.xml` — leaf cert + SPKI backup for `aladhan.com`; `./scripts/verify-aladhan-pins.sh` |
| Architecture: Room schema export | **Implemented** | `exportSchema = true`; `app/schemas/.../4.json` in VCS (`MIGRATION_3_4`). |
| The Widget | **Implemented** | Two providers (medium 5×1 + large); theme sync at bind; **M-widget** 5×1 three-band layout, **14sp** names/times, **time-only** (no M countdown); **L-widget** M-aligned grid via `widget_large_prayer_block.xml` + per-column countdown; picker previews; locale + Eastern Arabic digits; provider E2E + real worker stack. |
| Hijri Calendar and Events | **Implemented** | `HijriCalculator`, 10 events, Room v4, main subtitle + banner, monthly calendar (`headerDayText`, `eventNameCellRes`), M/L widget Hijri/event, 19 tests. |
| Phase 5G audit / architecture | **Implemented (Jun 2026)** | Comprehensive audits closed — `TextNormalizer`, `FetchError`/`SaveCityError`, `Prayer.SHURUQ`, Room migrations 1→4, `AdhanAlarmReceiver` goAsync, `LocationCatalogInitializer`. See `PHASED_PLAN.md` §5G + `Audit.md`. |
| Widget picker previews | **Implemented (5E.28)** | Sample prayer-time layouts in picker (API 31+ `previewLayout`); static light chrome — placed widgets follow user theme. |
| Launcher icon | **Implemented (5E.29)** | Adaptive icon: green `#1B5E20` + white crescent vector foreground. |
| Release build (R8 + signed) | **Done (Phase 6)** | offline + online APK/AAB; `keystore.properties.example`; `scripts/release-gate.sh`; README Release section |
| TLS pinning (Phase 6.8) | **Done** | SHA-256 pins for api.aladhan.com (leaf + SPKI backup). Expires 2027-01-01. |
| BootCompletedReceiver stale alarms | **Fixed** | Null-city and notifications-denied boot paths now explicitly cancel stale alarm intents. |
| Phase 5 hardening (Doze, perms, offline, DST, UI) | **Done (Jun 2026)** | 5A–5F signed off (emulator + device), incl. **5C.2**, **5D**, **5F.3** user verification. |
| Qibla compass (city coords + magnetometer) | **Done (7A, Jun 2026)** | `QiblaScreen`, `QiblaCalculator`, `CompassSensor`/`CompassHeading`; portrait hold; dual-layer rotation; align haptic. Merged PR **#11**–**#13**. |
| Compass calibration (7B) | **Active** | Geographic declination (`CompassGeographicField`), upright tilt gate, accuracy chip + auto tips, declination label. |
| City catalog + manual coords (8) | **Planned** | EU→AF→AS→AM data; manual lat/lng wizard before bulk fill. |
| L-widget layout parity | **Done (PR #13, Jun 2026)** | `widget_large_prayer_block.xml`; M-aligned columns + readable fonts; VM ticker test seam. |
| Product name (Hayya / حيا) | **Done (v1.1.1)** | Launcher + widgets + Settings version string; GitHub release `Hayya-v*.apk`; package `com.prayertime` unchanged. |
| Settings About section | **Done (v1.1.2)** | Bottom of Settings: Hayya description, GitHub repo link (`about_repo_url`), version line. |
| Adhan sound picker | **Implemented** | 8 sounds with live preview, persisted to DataStore, EN/AR labels. AdhanAlarmReceiver reads preference at alarm time. |

- [ ] Never market future capabilities as done.

### 3.3 Split "historical walkthrough" from "release sign-off"

- [ ] Canonical status should live in one public source (e.g., README).

---

## 4) Testing strategy lessons

### 4.1 The Minimum Testing Ladder

1. **Contract Tests:** Is the `CityConfig` I save identical to the one I read back? (DataStore test).
2. **Helper Tests:** Does `PrayerTimeCalculator.getNextPrayer(...)` return the correct prayer if the current time is 13:00 and Asr is at 13:08?
3. **Critical Tests:** What happens if the API returns an incomplete schedule (e.g., Maghrib is missing)?
4. **Smoke gate for workspace:** typecheck + tests + lint.

### 4.2 Test what usually breaks

- [x] **Time Zones (device ≠ city):** Day refresh and cache key use **city** `timezone`, not device local midnight — `needsPrayerDayRefresh(..., cityTimeZone)`, `todayDateLabel(config.timezone)`; unit tests (2E.2: London vs UTC, Auckland vs UTC). *Audit finding resolved Phase 2F.*
- [x] **Midnight:** Countdown handles zero seconds gracefully — `maxOf(0L, next.timestamp - now)`. Midnight rollover uses city TZ `needsPrayerDayRefresh`. *Resolved Phase 2F.*
- [x] **DST Transition:** `Europe/London` March 31st — 2E.3 unit tests (`isSameDay`, `needsPrayerDayRefresh`, countdown across spring-forward gap). Additional DST tests: `AladhanTimingsMapperTest` London BST vs UTC. *Resolved Phase 2F.*
- [ ] **Hijri:** Eid (1 Shawwal) timing verified via `HijriCalculatorTest` event cycle; full Ramadan→Eid manual QA optional in Phase 5.
- [ ] **Destructive operation confirmations:** Delete city confirmation behavior.

### 4.3 Fail fast on invalid response payloads

In `PrayerTimesRemoteDataSource`:
```kotlin
if (!json.has("data") || !json.getJSONObject("data").has("timings")) {
    return PrayerTimesResult.Error(FetchError.INVALID_RESPONSE)
}
```

- [ ] Never allow an empty or malformed response to reach the UI.

### 4.4 Known Test Coverage Holes

- [x] **Offline City Validation (1F-E.4):** Covered by `offline_only` toggle + `PrayerTimesRepositoryTest` / geocode rejection — separate offline APK dropped (Jun 2026); one app with Settings privacy toggle.
- [x] **Repository Fallback Rejection (1F-E.5):** `PrayerTimesRepositoryTest` — `offline save rejects fallback city with CITY_NOT_FOUND` + `online save rejects fallback city when geocode fails` (asserts `CityResolutionResult.Fallback` → `SaveCityResult.Error(CITY_NOT_FOUND)`, nothing persisted).
- [x] **Umlaut Parity (1F-E.6):** `LocationDataSourceTest.every_germany_picker_city_resolves_to_found` + ASCII umlaut cases.
- [x] **Phase 2E gaps (audit — time & notifications):** **2E.1–2E.4** in `PrayerTimeCalculatorTest` + `PrayerAlarmSchedulerTest` (midnight, city TZ, London DST, `ShadowAlarmManager`).
- [x] **Countdown midnight rollover (audit — “partially at runtime”):** **Resolved.** City TZ via `needsPrayerDayRefresh`; midnight calls `refreshTimesForNewDay()` (keeps prayer screen visible, no full-screen Loading). **By design:** after Isha until city midnight, countdown wraps to next Fajr while the table still shows today’s rows until refresh. **Requires app in foreground** at city midnight (else 25h stale / WorkManager on reopen).
- [x] **Midnight Timezone Accuracy (2E.2):** Day-change uses city `timezone`, not device default (`needsPrayerDayRefresh` London vs UTC tests).
- [x] **Device TZ vs city TZ midnight refresh (audit):** Risk: user in London, city Makkah — old code refreshed at device midnight; **fixed** — refresh at city calendar midnight (`PrayerTimesScreen` + `PrayerTimeCalculator.needsPrayerDayRefresh`); fetch/cache date label uses `config.timezone` (PR #5).
- [x] **DST Transition (2E.3):** `Europe/London` March 31, 2024 — `isSameDay`, `needsPrayerDayRefresh`, and `getCountdownToNext` across spring-forward gap.
- [x] **Cache Staleness (edge case):** City-scoped cache ensures switching cities doesn't wipe old data — `PrayerTimesRepositoryTest."offline city change clears cache and recalculates without API"` verified. City-switch no longer calls `deleteAll()`. **Resolved Phase 2F.**
- [x] **Spurious `knownCityCoords` keys (audit `DE_Ulrich`):** No `DE_Ulrich` in repo — likely misread of valid **`DE_Ulm`**. Actual orphan was **`DE_Offenbach`** (coords without picker entry); fixed by adding Offenbach to DE picker + `germany_known_coords_keys_match_picker_no_orphans` test.
- [x] **Online response parsing (audit):** `AladhanTimingsMapperTest` (18 tests) covers all parsing paths, DST-aware timestamps, time normalization, edge cases. `NetworkMapperTest` (10 tests) covers every error classification branch.
- [x] **Duplicate engine (audit):** `OnlinePrayerTimesRepository` was creating two `PrayerTimesLocalEngine` instances — fixed to share single instance via DI constructor.
- [x] **Flavor isolation (audit):** Single APK; Settings privacy toggle for offline-only vs Aladhan. `PrayerTimesRepository` is interface; `OnlinePrayerTimesRepository` composes local + API.
- [x] **ViewModel God Object (audit):** `MainViewModel` (380 lines) decomposed into `CitySetupViewModel` + `PrayerTimesViewModel` + `AppSettingsViewModel` + `SearchLocationsUseCase`.

---

## 5) Documentation discipline lessons

### 5.1 Docs are part of product correctness

Treat inaccurate docs as a bug.

Update docs whenever:

- [ ] Behavior changes
- [ ] Quality gate changes
- [ ] Scope freeze starts/ends
- [ ] Known limits discovered

Example: If a document states "The application displays the Hijri date" but it does not — that's a bug.

### 5.2 Keep a stabilization checklist

- [ ] **City:** The name supports both Arabic and English characters.
- [ ] **Prayer Times:** Countdown uses device wall-clock instant; **calendar day rollover** uses saved city timezone (not device local midnight — see 2E.2 / PR #5).
- [ ] **Adhan:** Does it function when the device is in "Do Not Disturb" mode? (It *must* function).
- [ ] **Events:** Does it correctly display Eid (1st of Shawwal) exactly 30 days after the start of Ramadan? (Simulated test).
- [ ] Evidence links for each item.

### 5.3 Verification Instructions Requirement

- [ ] **Verification Instructions:** After making any changes or completing a phase, the agent must provide explicit instructions to the user on how to verify those changes (in English).

---

## 6) Commit and PR discipline lessons

### 6.1 One reviewable change per commit

Each commit should represent one coherent intent.

- `feat: add initial City input screen with basic validation` (Good)
- `fix: adjust DST transition for Cairo timezone` (Good)
- `cleanup: move hardcoded strings to resources, fix widget layout, update deps` (Bad — three distinct changes)

### 6.2 Commit message quality

A good message states:

- [ ] What changed
- [ ] Why
- [ ] Scope

### 6.3 PR hygiene

Before merge:

- [ ] Smoke gate green
- [ ] Scope aligned to active checklist/gate
- [ ] No unrelated refactors in the same PR

### 6.4 Bundle Your Pushes

Do not push every tiny commit individually (e.g., "fix typo", "fix typo2"). Instead, group them into a logical bundle: use `git rebase -i`, then push.

### 6.5 Branching Strategy

- [ ] After the initial project setup push, all subsequent code modifications and features must be developed on a new git branch. Direct pushes to the `main` branch are forbidden.

### 6.6 Merge Rules

- [ ] The branch must pass the full verification suite `./scripts/smoke-ci.sh` successfully with zero errors.
- [ ] The branch scope must match the active phase in `PHASED_PLAN.md`.
- [ ] The agent is forbidden from merging their own PR/branch without explicit approval and manual sign-off from the user.
- [ ] The Phased Plan (PHASED_PLAN.md) and Feature Table (APP_CREATION_PLAYBOOK.md) must be updated to reflect the completed state before merging.
- [ ] Any resolved bugs must be added to the Incident Log before the branch is merged.

---

## 7) Platform-Specific Hardening (Android Only)

### 7.1 Host Constraints (Android)

- [ ] **Minimum API level:** 23 (Android 6.0) — ensures support for runtime permissions.
- [ ] **Target API level:** 35 (`compileSdk = 35`).
- [ ] **Absolutely no** `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION` permissions.

**Permissions:**

| Permission | API Level | Purpose | Fallback if Denied |
|---|---|---|---|
| INTERNET | All | Initial data fetching and daily updates | Show cached data; prompt when offline |
| POST_NOTIFICATIONS | 13+ | Displaying Adhan notifications | Adhan audio may not play; show in-app notice |
| SCHEDULE_EXACT_ALARM | 12+ | Precise Adhan scheduling | Use approximate alarm; guide user to Settings |

For every sensitive operation, document:

- [ ] Where it runs (UI / WorkManager / AlarmManager)
- [ ] Required OS-specific permissions
- [ ] Fallback behavior when denied
- [ ] User-facing message on denial (Android-specific)

### 7.5 Safety Over Convenience for Destructive Actions

**Delete City:** When attempting to delete the currently entered city, an alert appears: "This will erase all prayer times and settings, and return you to the Welcome screen." (Confirmation button only).

- [ ] Force confirmation with risk context.
- [ ] Handle conflict paths safely — never silently delete active configuration.

### 7.6 Security & Dependencies

- [ ] **Permissions:** `INTERNET` is always declared in the manifest. This is expected until a dedicated flavor split (e.g., `offline` vs `network`) is implemented.
- [x] **Dependencies:** Retrofit, Gson, and OkHttp are scoped to `onlineImplementation` only — the offline APK classpath is clean (verified via `offlineDebugRuntimeClasspath`).
- [ ] **Mock Data:** Absolutely no mock or fake data is allowed in production builds. Fakes and mocks (like `FakeAladhanApi`, `VmFakeApi`) are strictly reserved for the `test/` or `androidTest/` directories.
- [ ] **Data Sources:** `LocationDataSource` contains embedded maps of real city coordinates (not mocks). This large static surface is an intentional choice to support offline mode.

### 7.A) Android-Specific Enhancements

**WorkManager:**
- [ ] Use `PeriodicWorkRequest` once daily — `setMinimumInterval(1, TimeUnit.DAYS)`.
- [ ] Use `setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)` to mitigate Doze mode delays.
- [ ] Add fallback: on app launch, if the last fetch was >25 hours ago, perform a new fetch immediately.

**AlarmManager:**
- [ ] Use `setExactAndAllowWhileIdle()` to ensure Adhan triggers reliably even in power-saving mode.
- [ ] If `SCHEDULE_EXACT_ALARM` is denied, fall back to `setAndAllowWhileIdle()`.

**Widget:**
- [x] Update via WorkManager every 30 minutes (or immediately upon a prayer time change using `updateAppWidget`).
- [x] Four sizes; locale + stale cache fallback; boundary alarm refresh.

---

## 8) Reusable build sequence (for this project)

- [x] 1. Define Vertical Slice: City Input Screen + Prayer Times Display.
- [x] 2. Define Target Platform: Android (API 23+).
- [x] 3. Trust Boundaries: UI, Data Layer, Alarm, Widget.
- [x] 4. Contracts: `CityConfig` data class + lat/lon, `PrayerTimesResult` sealed class, `SaveCityResult`.
- [x] 5. Implement UI: Compose (`TextField`, `LazyColumn` for 6 prayer times, Change button).
- [x] 6. Implement Backend: `PrayerTimesRepository` — adhan-java local calc for daily times; Aladhan API used for geocode only on save. Room cache + offline fallback.
- [x] 7. Error Normalization: `NetworkMapper` → `FetchError`; save path → `SaveCityError`.
- [x] 8. User-facing errors: Display `Snackbar`; invalid city not persisted (SaveCityResult).
- [x] 9. Tests: **414** unit methods — `app/src/test/java/` (56 files; single APK). Includes `FakePrayerTimesRepository`, `ComposeScreenSmokeTest`, `PrayerTimesViewModelIntegrationTest`, `AladhanApiMockWebServerTest`, `PrayerTimeWidgetProviderTest`, `QiblaCalculatorTest`, `LocationNamesTest`, `LiveAladhanTestSupportTest`.
- [ ] 10. Truthful Docs: Update README with accurate feature list — *in progress*.
- [x] 11. Freeze Scope: Core app stable before widget/Hijri phases (Phases 3–4 complete).
- [x] 12. Migration: Hijri calendar and events added without altering core prayer-time pipeline (Phase 4).

---

## 8.1) Vertical Slice Definition Template (copy/paste)

### A) Slice identity

- **Slice name:**
- **User value (one sentence):**
- **In-scope capabilities:**
- **Out-of-scope capabilities (explicitly deferred):**

### B) Boundary and safety map

- **Execution context:** UI (Compose) | WorkManager | AlarmManager | Widget
- **Target platform:** Android (API 23+, target 34)
- **Platform-specific privilege needs:** INTERNET | POST_NOTIFICATIONS | SCHEDULE_EXACT_ALARM
- **Sensitive operations:** Delete city
- **User confirmations required:** Yes — confirmation dialog for city deletion
- **Failure classes expected:** `FetchError` / `SaveCityError` (compile-time split Jun 2026)

### C) Contract definition

- **Request schema(s):** CityConfig (cityName, countryCode, timezone, latitude, longitude)
- **Response schema(s):** PrayerTimesResult.Success (times, nextPrayer, countdown) or PrayerTimesResult.Error (type)
- **Deterministic result shape:** Success | Error — never raw exceptions
- **Invalid payload behavior:** Reject at boundary with stable error code

### D) UX definition

- **Success state shown to user:** Table of 6 prayer times + city name header + next prayer countdown
- **Failure message strategy:** `FetchError` / `SaveCityError` → humanized message via string resources (`PrayerTimesErrorMapper`)
- **Fallback behavior:** Cached data when offline; approximate alarm when exact alarm denied
- **Help text/docs link shown in UI:** Settings navigation hint for exact alarm permission

### E) Test plan (minimum)

- **Contract tests:** CityConfig save/read parity (DataStore)
- **Calculator tests:** getNextPrayer(...) correctness at various times
- **Edge case tests:** DST transition (Europe/London), midnight countdown, missing prayer in API response
- **Flow test target:** Enter city → display times → survive process death
- **Smoke gate target:** typecheck + tests + lint green

### F) Evidence required to mark slice "done"

- [ ] All tests above added and passing
- [ ] Smoke gate green
- [ ] Docs updated with truthful status (`Implemented / Partial / Planned`)
- [ ] Known limits documented
- [ ] No net-new scope outside this slice in same PR

### G) Post-implementation review

- [ ] **What regressed?**
- [ ] **What surprised us technically?**
- [ ] **What should become a permanent rule in this playbook?**
- [ ] **Incident log entry added?**

---

## 9) Case Studies — Incident Log (Planned / Hypothetical)

### Incident Entry Template

- **Date:**
- [ ] **Area:**
- **Symptom:**
- **Root cause:**
- **Impact:**
- **Fix implemented:**
- [ ] **Preventive action:**
- **Status:** open | monitoring | resolved

### Incident Log (Hypothetical — to be filled during development)

#### Default Reliance on GPS (CRITICAL)

- [ ] **Area:** Design / Requirements
- **Symptom:** (To be avoided) Initial design might suggest location.
- **Root cause:** The mindset that "all prayer apps use GPS."
- **Impact:** Direct violation of project requirements.
- **Preventive action:** Playbook permanently disallows location permissions.
- **Status:** monitoring (preventive rule in place)

#### Daily Update Failure Due to Doze Mode

- [ ] **Area:** WorkManager / Updates
- **Symptom:** (Hypothetical) Times might become outdated.
- **Root cause:** Doze mode deferring WorkManager.
- **Preventive action:** Use expedited work + fallback on app launch.
- **Status:** monitoring (preventive design in place)

#### Cache Staleness on Mode/Coords Change (Edge Case)

- [ ] **Area:** Data / Repository
- **Symptom:** Prayer times may reflect previous settings if the user toggles offline mode or changes coordinates but the cache for the current day still exists.
- **Root cause:** `fetchLocalFallback` returns today's cached times immediately without verifying if the underlying configuration (mode/coords) has changed since the cache was written.
- **Impact:** Times might be slightly off (stale) for the rest of the current day.
- **Fix implemented:** None yet (Fine for performance currently).
- [ ] **Preventive action:** Ensure any explicit user action (e.g., toggling offline mode or changing city) completely clears the Room cache for all days.
- **Status:** monitoring

#### Picker Label vs Coord Key Mismatch — Umlaut Cities (DEFECT)

- [x] **Area:** Data / LocationDataSource
- **Symptom:** Two German cities shown in the city picker (`Osnabruck`, `Saarbrucken`) failed offline save with `CITY_NOT_FOUND` despite coordinates existing.
- **Root cause:** ASCII picker labels (`Osnabruck`) vs umlaut `knownCityCoords` keys (`DE_Osnabrück`); direct key lookup failed; `foldForLookup` fallback only masked the inconsistency.
- **Fix (Phase 1F):** `LocationDataSource.foldForLookup()` NFD-diacritic-stripping fallback so `Osnabruck` → `osnabruck` matches `Osnabrück` → `osnabruck`.
- **Fix (current session):** Picker names changed to proper German Umlauts (`Osnabrück`, `Saarbrücken`) so direct key lookup succeeds. `filteredCities()` also uses `foldForLookup` so typing without Umlauts still matches.
- **Impact:** 2 of 127 DE cities silently fail to save in offline mode. No crash, but user sees "City not found" for a city they picked from the list.
- **Fix implemented:** Both foldForLookup fallback and picker-name alignment (picker now uses `Osnabrück`/`Saarbrücken` matching coords keys exactly).
- [x] **Preventive action:** Added tests: `every_picker_city_key_name_matches_known_coords_exactly_or_is_truly_unknown`, `german umlaut city name directly resolves to Found`, `typo of known city returns Fallback not Found`, `offline save rejects typo city with CITY_NOT_FOUND`.
- **Status:** resolved

#### Comprehensive codebase audit — architecture & runtime (Jun 2026)

- [x] **Area:** Architecture / Data / Widget / Tests
- **Symptom:** Audit report (2026-06-04) flagged non-atomic Room cache writes, silent Aladhan parse failures, shared `ErrorType` across fetch/save, `Prayer.DUHA` semantic mismatch, duplicated test fakes, widget path mocked.
- **Root cause:** Phase 3–4 velocity; incremental hardening deferred to Phase 5.
- **Impact:** Potential wrong prayer times on malformed API input; compile-time error conflation; test gaps on widget and HTTP wire path.
- **Fix implemented:** Phase **5G** + **5E** + **7A** — see `PHASED_PLAN.md` §5G/§5E/§7A (atomic `cacheToRoom`, audit v2 Jun 2026, Qibla compass, **414** tests green).
- [x] **Preventive action:** Graphify + AGENTS.md + `Audit.md` updated; architectural audit items closed; manual QA (5A–5F) tracked separately.

#### Comprehensive codebase audit — reconciliation (Jun 2026)

- **Scope:** Full verification against AGENTS.md; `Audit.md` score **96/100**.
- [x] Room schemas 1–4 exported; instrumented migration tests 1→3 (+ JVM smoke).
- [x] Widget theme bind cache; `AdhanAlarmReceiver` async mute path.
- [x] Graphify **3321** nodes, **50433** edges.
- **Status:** resolved (code); monitoring (manual QA)

#### Offline APK Flavor — "Offline-only" Toggle Is Misleading (UX)

- [ ] **Area:** UI / Product Flavor
- **Symptom:** On the `offline` APK variant, the "offline-only" toggle in the About screen appears functional, but disabling it cannot reach Aladhan because `api` is always `null`. The save path silently falls back to bundled geocode regardless of the toggle state.
- **Root cause:** The `offline` flavor does not inject a real `PrayerApi` instance. The UI toggle has no meaningful effect on the offline variant.
- **Impact:** Harmless but confusing — user may believe they have enabled network mode when they have not.
- **Fix implemented:** None yet. Options: hide the toggle on the `offline` flavor, or add a flavor-specific label explaining the limitation.
- [ ] **Preventive action:** Add flavor-conditional UI to disable or re-label the network toggle on the `offline` variant.
- **Status:** monitoring

#### Network Geocode Failure Does Not Fall Back to Bundled Coords (Online Mode)

- [x] **Area:** Data / Repository / Online Flavor
- **Symptom:** In `online` flavor with `offline_only=false`, if the Aladhan geocode API call fails (network error or `null` response), the city is not saved — no local bundled coords are tried.
- **Root cause (historical):** Early `resolveCoordinates()` returned on any non-null online result, including `GeocodeResult.Error`.
- **Fix implemented:** Only `GeocodeResult.Success` returns early; errors fall through to `LocationDataSource.resolveCityCoordinates()`.
- [x] **Preventive action:** `PrayerTimesRepositoryTest` — `network error on geocode falls back to local coords for known city`.
- **Status:** resolved (2026-06-03)

#### Per-app Arabic locale had no effect (DEFECT)

- [x] **Area:** UI / i18n
- **Symptom:** Language picker showed العربية selected; UI stayed English after recreate.
- **Root cause:** `AppCompatDelegate.setApplicationLocales()` called from `ComponentActivity` + platform Material theme — AppCompat locale pipeline not wired.
- **Fix implemented:** `MainActivity` → `AppCompatActivity`; `Theme.AppCompat.Light.NoActionBar`; `res/xml/locales_config.xml`; startup apply in `PrayerTimeApplication`.
- [x] **Preventive action:** Manual check: Language → ar → recreate → Arabic strings on prayer + About screens.
- **Status:** resolved (2026-06-04, pending emulator sign-off 2H-B.4)

#### Adhan toggle crashed app (DEFECT)

- [x] **Area:** Permissions / About UI
- **Symptom:** Enabling Adhan notifications on About screen crashed the app (emulator / some devices).
- **Root cause:** Toggle synchronously started battery-optimization and exact-alarm system activities; missing activity handlers → uncaught `ActivityNotFoundException`.
- **Fix implemented:** Toggle only requests `POST_NOTIFICATIONS` when needed; optional settings via clickable notices; `AdhanPermissions.safeStartActivity()`.
- [x] **Preventive action:** Do not chain multiple `startActivity` calls from a Switch handler.
- **Status:** resolved (2026-06-04)

#### Prayer screen header truncated city name (UX)

- [x] **Area:** UI / PrayerTimesScreen
- **Symptom:** City label showed as broken lines (“Ha / m…”) beside crowded action buttons.
- **Root cause:** City title and three actions shared one `Row` with `weight(1f)` on title.
- **Fix implemented:** City on full-width line; Change / Language / About on second row.
- **Status:** resolved (2026-06-04)

---

## 10) Maintenance rule for this playbook

Whenever a new issue arises (e.g., a bug in the Hijri date calculation):

- [ ] 1. Add a new Incident Log entry.
- [ ] 2. Add or update a preventive rule or checklist item (e.g., "Test Hijri dates for the entire month of Ramadan").
- [ ] 3. Link evidence of verification.
- [ ] 4. Keep language factual and technical — either Arabic or English (no marketing jargon).
- [ ] 5. If the project exceeds 60 files, run an architecture graph tool after code changes.

This document serves as the living engineering memory for the *Prayer Time Widget* application.

---

## 11) Quality Pipeline Phases (reference template)

### Phase A — Immediate Gate

- [ ] Build passes with zero errors
- [ ] Lint configured and passing (detekt / ktlint)
- [ ] Dependency updates reviewed regularly

### Phase B — Behavior Stability

- [ ] Unit tests for contract + error mapping paths
- [ ] Critical scenario tests: failure UX, permission guidance, fallback behavior
- [ ] Coverage baseline for `domain/` and `data/` modules

### Phase C — Product Hardening

- [ ] Performance measurement (startup latency, memory footprint, widget update speed)
- [ ] Doze mode and battery optimization behavior verified
- [ ] Adhan audio playback reliability (DND mode, ringer modes)

### Phase D — Environment Hardening

- [ ] Multi-device / multi-API-level testing
- [ ] Permission denial flow tested (each permission denied individually)
- [ ] Offline mode behavior verified (airplane mode)
- [ ] Widget tested across launchers

### Documentation Discipline

- [ ] Every hardening step must update the playbook in the same change set.
- [ ] Treat missing playbook updates as a process bug.

### UI Hardening (batch approach)

- [ ] Apply UI polish in controlled batches:
  1. layout and spacing rhythm
  2. visual hierarchy (titles, accents, emphasis)
  3. button states and feedback clarity
  4. responsive behavior and overflow sanity
  5. interaction states (hover, focus, active, disabled)
  6. contrast and typography for readability
  7. i18n string extraction for all hardcoded text (e.g. `prayerName()`)
- [ ] Each batch must preserve existing behavior and pass typecheck/tests before the next batch.

---

## 12) Architecture Migration Pattern

When migrating between major architectural changes (e.g., switching from Aladhan API to local calculation library, or migrating from ViewModel to Compose state):

- [ ] Freeze net-new features during migration
- [ ] Only migration, parity fixes, tests, CI, packaging, and docs evidence updates
- [ ] Maintain a stable API surface across the migration (repository abstraction pattern)
- [ ] Each migrated channel must include test or smoke evidence in the same batch
- [ ] Verify all existing call sites against the new abstraction — no missing coverage
- [ ] CI workflow must include the new build dependencies

### Renderer Parity Audit

When completing a migration:

- [ ] Audit all screen call sites against the new repository/API — verify no missing methods
- [ ] Check for regressions in existing UI behavior
- [ ] Verify all CI jobs include the correct toolchains for the new target
- [ ] Run full typecheck and smoke gate as evidence

---

## 13) Critical Agent Verification Directive

- **Thorough Verification Mindset:** As you are an expert in computing, programming, software engineering, and application development, your current mission is to write a report and check the entire existing application to determine whether everything these claims represent is sound, or if there remain any gaps, risks, deficiencies, missing components, voids, spurious data, static data, or mock data - do not overlook any matters simply because you deem them trivial or simple. Do not believe the files until you check whether they are right or not.

