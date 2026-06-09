# AGENTS — Hayya (حيا) Project Setup

## Build environment

```sh
export JAVA_HOME=$HOME/jdk21
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew assembleDebug testDebugUnitTest
```

- JDK 21 (Temurin) at `~/jdk21` — system JDK 25 breaks current Gradle/AGP
- Android SDK at `~/Android/Sdk` (platforms 34 + 35, build-tools 34)
- Gradle 8.12, AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.11.00, Hilt 2.52

## Project structure (current)

```
Hayya/  (repo folder may still be Private-Prayer; package com.prayertime)
├── app/src/main/java/com/prayertime/
│   ├── alarm/              # AdhanAlarmReceiver, BootCompletedReceiver, PrayerAlarmScheduler
│   ├── data/
│   │   ├── local/          # Room v4 (city-scoped cache + Hijri columns), AppPreferencesDataSource, CityConfigSerializer
│   │   ├── repository/     # PrayerTimesRepository, OnlinePrayerTimesRepository, LocalPrayerTimesRepository, …
│   │   ├── remote/         # AladhanApi, NetworkMapper, PrayerApi (Retrofit/Gson/OkHttp)
│   │   ├── LocationDataSource.kt, LocationCatalogLoader.kt, LocationCatalog.kt
│   │   └── assets/locations.json
│   ├── domain/
│   │   ├── model/          # Prayer.kt (SHURUQ), FetchError, SaveCityError, HijriModels.kt, …
│   │   ├── repository/     # LocationRepository (interface)
│   │   ├── calculator/     # PrayerTimeCalculator, LocalPrayerTimeCalculator, HijriCalculator, QiblaCalculator
│   │   ├── util/           # LocationNames (AR city/country display + search folding)
│   │   └── usecase/        # SearchLocationsUseCase
│   ├── sensor/             # CompassSensor, CompassHeading (accel+mag, portrait Qibla)
│   ├── locale/             # AppLocale, TextNormalizer (diacritic folding)
│   ├── notification/       # AdhanNotificationHelper
│   ├── permission/         # AdhanPermissions
│   ├── di/                 # Hilt modules (DataModule, DomainModule, AppConfigModule, NetworkModule, RepositoryModule, LocationCatalogInitializer, CompassEntryPoint)
│   ├── ui/
│   │   ├── MainActivity.kt, PrayerTimeRoot.kt
│   │   ├── city/           # CitySetupViewModel, WizardStep
│   │   ├── prayer/         # PrayerTimesViewModel, PrayerTimesUiState
│   │   ├── settings/       # AppSettingsViewModel
│   │   ├── screens/        # CityInputScreen, PrayerTimesScreen, QiblaScreen, AboutScreen (Settings UI), LanguagePickerDialog, HijriCalendarScreen, CalendarColors
│   │   ├── theme/          # AppTheme, ThemePalettes, PrayerTimeTheme, LocalAppTheme, LocalCalendarPalette
│   ├── worker/             # PrayerRefreshWork, PrayerTimeRefreshWorker, WidgetRefreshWork, WidgetUpdateWorker (@HiltWorker)
│   ├── widget/              # PrayerTimeWidgetProvider (medium 5×1), PrayerTimeWidgetProviderLarge, WidgetUpdater, WidgetSnapshotLoader, WidgetRemoteViewsBuilder, WidgetPrayerBoundaryScheduler, WidgetPrayerBoundaryReceiver, WidgetLocaleContext, WidgetDigitFormatter, CountdownFormatter
│   └── PrayerTimeApplication.kt  # @HiltAndroidApp, HiltWorkerFactory
├── app/src/test/java/      # JVM unit tests (single APK; includes network/Aladhan tests)
├── app/schemas/            # Room exported JSON (v4) — commit on version bumps
├── scripts/                # smoke-ci.sh, qa-doze.sh, qa-offline.sh, verify-aladhan-pins.sh
├── dev                     # ./dev → scripts/emu (emulator + installDebug + launch)
├── PHASED_PLAN.md
├── APP_CREATION_PLAYBOOK.md
├── graphity.md
└── local.properties
```

## Phase status

| Phase | Status | Tests pass | Open issues |
|-------|--------|------------|-------------|
| 0 Scaffold | Done | ✅ | — |
| 1 Vertical slice | Done — wizard, hybrid Aladhan + adhan-java, Room cache | ✅ | — |
| 1 manual | Hameln + Berlin verified | — | — |
| 1F Privacy / offline-only | Done — offline_only flag, privacy UI, fallback rejection, diacritic lookup, tests | ✅ | — |
| 2A Countdown | Done — live 1s ticker, wrap-to-tomorrow, city-TZ day-change refresh | ✅ | — |
| 2B–2D | Adhan alarms, permissions, WorkManager daily refresh | Done | Manual QA signed off (Jun 2026) |
| 2E | Unit tests (midnight, city TZ, DST, alarms, migrations, workers, engine) | Done — **419** `@Test` in `app/src/test/java/` (56 files) | — |
| 2F | Architecture hardening — ViewModel decomposition, city-scoped cache, async init, timezone consistency | Done | — |
| 2G | Hilt DI, worker/engine tests, cache invalidation, About refresh | Done | — |
| 2 manual | Adhan exact/fallback, emulator | Done | — |
| 2H | Pre–Phase 3 polish — LocationRepository, Hilt network, Room schemas, locale EN/AR, RTL (2H-B.5), header/adhan fixes, `./dev` | Done | — |
| 3 | Home screen widget — **two** providers (medium + large), stale cache, locale/digits, provider E2E + real worker tests | Done | — |
| 4 | Hijri calendar — calculator, 10 events, Room v4, main + calendar + M/L widget, 19 tests | Done | — |
| 5G | Audit / architecture hardening — split errors, TextNormalizer, catalog validation, test infra | Done (Jun 2026) | — |
| 5E | UI polish — spacing, RTL, language picker, **three app themes**, Settings screen, portrait lock, Compose smoke | Done (Jun 2026) | — |
| 5 | Manual QA hardening — 5A–5F signed off (Jun 2026, incl. 5C.2, 5D, 5F.3); 5A–5E automated; TLS; USE_EXACT_ALARM | **Done** | — |
| 6 | Release — R8, signed APK/AAB | **Done (`v1.0.0`)** | Tagged Jun 2026 |
| 7A | Qibla compass — city bearing + portrait accel/mag sensor, align feedback | **Done** — PR **#11** + **#12** + **#13** (L-widget) merged Jun 2026 | — |
| **7B** | Compass geographic calibration + UI | **Done** — geographic declination, upright gate, accuracy UI, user fine-offset, 3 tests; merged PR **#42** Jun 2026 | — |
| **8A** | Manual lat/lng coordinates wizard | **Done** — `WizardStep.ManualCoords`, validation, EN/AR strings, 426 JVM tests | `feat/manual-coords-wizard` |
| **8B** | Europe `knownCityCoords` fill | **Planned** — ~465 missing; branch `feat/city-coords-europe` after 8A merge | — |

## Architecture (post-2F hardening)

### Dependency injection (Hilt)
- `@HiltAndroidApp` on `PrayerTimeApplication` — implements `Configuration.Provider` for `HiltWorkerFactory`
- Flavor `RepositoryModule`: `OnlinePrayerTimesRepository` + `NetworkModule`; privacy via Settings **offline-only toggle** (`offline_only` DataStore flag), not a separate APK
- `@HiltViewModel` + `hiltViewModel()` for all three ViewModels; `@AndroidEntryPoint` on `MainActivity`, `BootCompletedReceiver`
- `@HiltWorker` on `PrayerTimeRefreshWorker` — repository/preferences injected, no service-locator cast
- Unit tests construct ViewModels/workers directly (no Hilt test harness)

### ViewModel decomposition
- `MainViewModel` (380 lines) → 3 focused VMs: `CitySetupViewModel` (wizard/search), `PrayerTimesViewModel` (fetch/ticker), `AppSettingsViewModel` (privacy/adhan toggles)
- `SearchLocationsUseCase` → `LocationRepository` (domain interface, `LocalLocationRepository` in data)

### City-scoped Room cache
- `PrayerTimeEntity` includes `cityKey` (e.g. `"DE_Hameln"`) — composite index `(cityKey, dateLabel)`
- Switching cities no longer wipes cache — Hameln→Berlin→Hameln hits cache
- `clearCityConfig()` clears DataStore only; Room cache kept by design. Recovery: Settings → **Refresh today's times** (`invalidateTodayCache`) or `clearAllCaches()`
- **Room schema:** version **4** (current). `exportSchema = true`; KSP writes `app/schemas/com.prayertime.data.local.AppDatabase/<version>.json` (commit on version bumps). Migrations: `MIGRATION_1_2`, `MIGRATION_2_3`, `MIGRATION_3_4` in `PrayerTimeMigrations.kt`. Next schema change → bump `@Database(version)`, add `MIGRATION_4_5`, commit new schema JSON
- **Room migration tests:** **CI gate** — exported schema JSON (v1–v4) + JVM `AppDatabaseMigrationTest` (3 tests, Robolectric, runs in `testDebugUnitTest`). **Optional** — `AppDatabaseMigrationInstrumentedTest` (3 tests, `MigrationTestHelper` validates against exported JSON on device; not in `smoke-ci.sh`)

### Boot-time adhan reschedule
- `BootCompletedReceiver.rescheduleAfterBoot()` runs on `BOOT_COMPLETED` / `LOCKED_BOOT_COMPLETED`
- When `adhanNotificationsEnabled` is false: `cancelAllPrayerAlarms()` then return (clears stale alarms after reboot)
- When enabled: fetch today's times, then `PrayerAlarmScheduler.schedulePrayerAlarms(useReliableAlarms = true)` — requires `USE_EXACT_ALARM` (install grant) or user-granted `SCHEDULE_EXACT_ALARM` on API 31+ for `setAlarmClock`

### Unit test source sets
- `src/test/java/` — all JVM unit tests (network + shared)
- Run: `./gradlew testDebugUnitTest`
- Live HTTP integration tests skip unless `PRAYERTIME_LIVE_HTTP=1` (default off in CI); also skip when api.aladhan.com is unreachable

### Async LocationDataSource
- `LocationCatalogInitializer` (Hilt `@Singleton`) calls `LocationDataSource.initialize()` at app startup — not from `LocalLocationRepository`
- `initialize()` kicks off JSON parsing on `Dispatchers.IO` — returns immediately, no main-thread block
- `suspend fun awaitReady()` for coroutine callers; sync list accessors return empty while loading; `NOT_STARTED` throws
- `locations.json` (187 KB, 9,221 lines) in `assets/` — no hardcoded Kotlin maps; expanded via `scripts/expand_locations.py`

### Timezone consistency
- Staleness check uses `needsPrayerDayRefresh(lastFetch, now, cityTZ)` — not a flat 25-hour epoch threshold
- Countdown loop uses same `needsPrayerDayRefresh` with city TZ for midnight rollover
- No device-clock vs city-clock split

### Online pipeline tests
- `AladhanTimingsMapperTest` (18 tests): API response parsing, time normalization, DST-aware timestamps, edge cases
- `NetworkMapperTest` (10 tests): every error classification branch
- `AladhanTimingsMapper` moved from `src/online/` → `src/main/` (zero Retrofit/Gson deps)
- `OnlinePrayerTimesRepository` shares single `PrayerTimesLocalEngine` instance with composed `LocalPrayerTimesRepository`

### Flavor isolation
- **Single APK** (`com.prayertime`) — Aladhan stack in `src/main/`; user chooses offline-only vs network in Settings
- `PrayerTimesRepository` is an interface — `OnlinePrayerTimesRepository` composes `LocalPrayerTimesRepository` + `PrayerApi`
- **`offline_only` flag** (default true): zero HTTP when enabled; Aladhan when disabled
- **Release shrinker:** `proguard-rules.pro` keeps `domain.model.**`, `data.remote.**`, and `PrayerTimeWidgetProvider` + `PrayerTimeWidgetProviderLarge`; `-dontwarn` for OkHttp/Retrofit
- **Dependabot:** pinned ignores for Kotlin/AGP/Room/Hilt/coroutines in `.github/dependabot.yml` — bump only on coordinated upgrade

## APK sizes (debug builds)

| Build | APK | Size |
|-------|-----|------|
| debug | `app-debug.apk` | ~23 MB |

`smoke-ci.sh` builds + tests single variant; APK size gate limit 25 MB.

## Prayer calculation (current)

- **Default target mode:** offline-only (`offline_only=true`) → no API calls, local coordinates + adhan-java
- **Optional network mode:** Aladhan `v1/timingsByCity` — `method=4` (Umm al-Qura), `school=0` (Shafi), `latitudeAdjustmentMethod=3`
- **TLS pinning (online):** `network_security_config.xml` pins `aladhan.com` (leaf cert + SPKI backup). Verify/rotate: `./scripts/verify-aladhan-pins.sh`
- **Offline engine:** `com.batoulapps.adhan:adhan:1.2.1` — Umm al-Qura + Shafi + `TWILIGHT_ANGLE` if |lat| ≥ 48°
- **UI:** second row = **Shuruq** (sunrise), not Duha+20

## App themes (5E.16–19)

- **Three themes:** `AppTheme.LIGHT` (default), `GREEN`, `DARK` — persisted in DataStore (`app_theme`)
- **Compose:** `PrayerTimeTheme(theme)` + `ThemePalettes.materialScheme()`; calendar uses `LocalCalendarPalette` / `calendarPalette()`
- **Settings:** user-facing **Settings** screen (`AboutScreen.kt`) — theme picker, privacy, adhan, refresh
- **Widgets (two providers):** `PrayerTimeWidgetProvider` (**medium**, `widget_info_medium` **5×1** horizontal-only) and `PrayerTimeWidgetProviderLarge` (**large**, resizable). Shared stack: `WidgetSnapshot.appTheme`, `ThemePalettes.widget()`, `WidgetRemoteViewsBuilder` (`WidgetSize.MEDIUM` | `LARGE`). Per-theme column highlight drawables: `widget_col_highlight_{light,green,dark}.xml` (8dp radius). **Medium:** three equal bands (Hijri header / short prayer names / times), **14sp**, **time-only** (`timeOnly=true`, no per-column countdown), next-prayer highlight via `widget_highlight_0..5` overlay on the times row. **Large:** city label, Hijri, live clock; prayer grid via `widget_large_prayer_block.xml` (M-aligned 3-band columns) + per-column countdown. (Legacy SmallTall/SmallWide providers removed — consolidated into medium.)
- **Bottom navigation:** 4-tab `NavigationBar` (Prayer 🕌 / Qibla 🧭 / Calendar 📅 / Settings ⚙️) with `NavHost` — replaces boolean-flag if/else. "Change" city `TextButton` kept in header.
- **Custom adhan sounds:** `AdhanAlertDeliverer` + `AdhanSoundResolver` — user-imported audio files (`custom_` key prefix, `custom_adhans/` dir), merged UI picker with play/delete, fallback to default if file missing
- **Per-prayer mute:** `PrayerTimesScreen` toggles → `muted_prayers` DataStore; `PrayerAlarmScheduler` schedules `Prayer.adhanAlarmPrayers` (five fard; **SHURUQ** display-only); `AdhanAlarmReceiver` no-ops when muted

## Graphify

After structural changes or phase completion:

```sh
OPENAI_API_KEY="" graphify update . --no-cluster
```

Details: [`graphity.md`](graphity.md). Diagrams: [`PHASED_PLAN.md`](PHASED_PLAN.md). **Last run:** 2026-06-08 — **5252** nodes, **99405** edges (v1.1.5 bottom nav + inset fixes). Install: `uv tool install graphifyy`.

## Orientation

- **Portrait-only:** `MainActivity` `android:screenOrientation="portrait"`.

## Agent rules

- **Verification:** After changes: `./dev` (boot `PrayerTimeEmulator`, install debug, launch). Package `com.prayertime`. Headless: `./dev --headless`. CI: `./scripts/smoke-ci.sh`. After dependency bumps: full `./scripts/smoke-ci.sh` before merge.
- **Instrumented tests (optional):** `./gradlew connectedDebugAndroidTest` — only `AppDatabaseMigrationInstrumentedTest` (3 tests); requires emulator/device, not part of `smoke-ci.sh`. JVM migration coverage is sufficient for schema bumps; run instrumented when validating `MigrationTestHelper` against a real framework SQLite stack.
- **Branching:** Feature work on branches; no direct push to `main`.
- **Merge:** `./scripts/smoke-ci.sh` green, scope matches `PHASED_PLAN.md`, user sign-off, update `PHASED_PLAN.md` + playbook feature table.
- **Docs language:** English for plans and agent-facing docs.
