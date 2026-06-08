# Graphify — Knowledge Graph Playbook (Hayya)

> **Repo:** Hayya (حيا) Android app (Kotlin / Compose). Package `com.prayertime`.  
> **Phased plan:** [`PHASED_PLAN.md`](PHASED_PLAN.md) (Mermaid diagrams + Graphify maintenance table).  
> **Agents:** Run Graphify after phase gates and when `data/`, `domain/`, or `ui/` boundaries change.

---

## Quick reference (PrayerTime-)

| Step | Command |
|------|---------|
| First-time / force rebuild | `OPENAI_API_KEY="" graphify update . --no-cluster --force` |
| After feature work | `OPENAI_API_KEY="" graphify update . --no-cluster` |
| Visual cluster report | `GRAPHIFY_VIZ_NODE_LIMIT=15000 graphify cluster-only . --no-label` |
| Cursor rules | `graphify cursor install` |
| Optional git hook | `graphify hook install` |

**Artifacts:** `graphify-out/graph.json`, `graphify-out/GRAPH_REPORT.md`, `graphify-out/graph.html`

---

## Phase 1: Workspace & first vertical slice

Before initializing the graph, baseline code must exist so the AST parser has targets.

1. Initialize the project and implement the **first vertical slice** (see `APP_CREATION_PLAYBOOK.md` §1.1).
2. Run `./gradlew assembleDebug testDebugUnitTest` — must be green (see `scripts/smoke-ci.sh` for full gate).

**PrayerTime- status:** Phases **0–7A** complete on `main` (`v1.0.0` tagged Jun 2026). PR **#11**–**#13** merged. **No active feature phase** — see `PHASED_PLAN.md` §Post-7A. **Last graph:** see footer.

---

## Phase 2: CLI installation

```bash
which graphify
# If missing (official PyPI package — CLI command is still `graphify`):
uv tool install graphifyy
# Alternatives: pipx install graphifyy
```

---

## Phase 3: AI assistant & hooks

```bash
graphify cursor install
# Optional:
graphify hook install
```

Generates `.cursor/rules/graphify.mdc` for agent context.

---

## Phase 4: Initial AST extraction (zero token cost)

```bash
OPENAI_API_KEY="" graphify update . --no-cluster --force
```

- `update .` — scope repo root  
- `--no-cluster` — local tree-sitter only  
- `--force` — write `graphify-out/graph.json`

---

## Phase 5: Clustering & visualization

```bash
GRAPHIFY_VIZ_NODE_LIMIT=15000 graphify cluster-only . --no-label
```

---

## Phase 6: Verification gate

```bash
ls graphify-out
# graph.json, GRAPH_REPORT.md, graph.html (all under graphify-out/)

xdg-open file://$(pwd)/graphify-out/graph.html
```

---

## Post-initialization maintenance

Whenever a phase gate closes or packages move:

```bash
OPENAI_API_KEY="" graphify update . --no-cluster
```

Tick **Graphify** checkbox in `PHASED_PLAN.md` for that phase.

---

## What agents should read from the graph

| Layer | Typical nodes |
|-------|----------------|
| UI (main) | `MainActivity` (portrait-only), `PrayerTimeRoot`, `CityInputScreen`, `PrayerTimesScreen` (per-prayer mute toggles, Qibla entry), `QiblaScreen` (dual-layer compass dial + arrow, align haptic), `AboutScreen` (Settings UI), `HijriCalendarScreen`, `AnnualEventsView`, `LanguagePickerDialog` (Compose `Dialog`), `AppSpacing`, `PrayerTimeTheme`, `AppTheme`, `ThemePalettes`, `LocalAppTheme`, `LocalCalendarPalette`, `CitySetupViewModel`, `PrayerTimesViewModel`, `AppSettingsViewModel` |
| Locale (main) | `AppLocale`, `TextNormalizer` — per-app language + diacritic folding |
| Widget (main) | `PrayerTimeWidgetProvider` (medium 5×1) + `PrayerTimeWidgetProviderLarge`, `WidgetUpdater`, `WidgetSnapshotLoader`, `WidgetRemoteViewsBuilder` (`timeOnly` medium, `widget_prayer_block` empty GONE), `widget_prayer_times_medium.xml` (3-band), `widget_large_prayer_block.xml` (L-widget M-aligned grid), `widget_prayer_times_large.xml`, `widget_preview_*` / `widget_initial_*`, `WidgetPrayerBoundaryScheduler`, `WidgetPrayerBoundaryReceiver`, `WidgetLocaleContext`, `WidgetDigitFormatter`, `widget_col_highlight_{light,green,dark}.xml`, `WidgetRefreshWork`, `WidgetUpdateWorker` |
| Hijri (main) | `HijriCalculator`, `HijriModels`, `HijriDateFormatter` (`eventNameCellRes`), `HijriCalendarScreen` (`headerDayText`), `CalendarColors` |
| Data (main) | `PrayerTimesRepository`, `LocalPrayerTimesRepository`, `LocalLocationRepository`, `LocationCatalogInitializer`, `PrayerTimesLocalEngine`, `AladhanTimingsMapper`, `LocationDataSource`, `LocationCatalogLoader`, `CityConfigSerializer`, `AppDatabase` (v4), `PrayerTimeMigrations` (`MIGRATION_1_2`…`3_4`), `app/schemas/` (1–4.json) |
| Data (online only) | `OnlinePrayerTimesRepository` (composes local + `PrayerApi`), `AladhanApi`, `AladhanResponse`, `NetworkMapper`, `NetworkModule` (+ debug `HttpLoggingInterceptor`); `network_security_config.xml` TLS pins for `aladhan.com`; `./scripts/verify-aladhan-pins.sh` |
| App | `@HiltAndroidApp` `PrayerTimeApplication`; flavor `RepositoryModule` in `src/offline/` / `src/online/` |
| Domain (main) | `LocalPrayerTimeCalculator`, `PrayerTimeCalculator`, `HijriCalculator`, `QiblaCalculator`, `LocationNames` (`domain/util`), `LocationRepository`, `SearchLocationsUseCase`, `FetchError`, `SaveCityError`, `Prayer` (`SHURUQ` display-only; `adhanAlarmPrayers`), `PrayerWindow` |
| Sensor (main) | `CompassSensor` (accel + magnetometer), `CompassHeading` (azimuth + declination), `CompassEntryPoint` |
| DI (main + flavor) | `DataModule`, `DomainModule`, `AppConfigModule`, `RepositoryModule`, `NetworkModule` (online), `WidgetEntryPoint`, `CompassEntryPoint` |
| Worker (main) | `PrayerRefreshWork`, `PrayerTimeRefreshWorker`, `WidgetRefreshWork`, `WidgetUpdateWorker` (`@HiltWorker`) |
| Tests (shared) | `FakePrayerTimesRepository`, `PrayerTimesViewModelIntegrationTest`, `PrayerTimesLocalEngineTest`, `LocationCatalogLoaderTest`, `LocalLocationRepositoryTest`, `LocationNamesTest`, `WidgetSnapshotLoaderIntegrationTest`, `PrayerTimeWidgetProviderTest`, `WidgetRefreshWorkTest`, `TextNormalizerTest`, `HijriCalculatorTest`, `AdhanAlertDeliverer` (custom + built-in playback, fallback), `QiblaCalculatorTest`, `CompassHeadingTest`, `AdhanSoundResolverTest`, `HijriDateFormatterTest`, `PrayerTimesErrorMapperTest`, `LiveAladhanTestSupportTest`, `AppDatabaseMigrationTest` (v1→v4), `AppDatabaseMigrationInstrumentedTest`, `ComposeScreenSmokeTest`, `OnlinePrayerTimesRepositoryTest`, `AladhanApiMockWebServerTest` |

Use the graph to avoid stale imports when refactoring repository ↔ flavor-specific API ↔ calculator paths. **Do not** expect `PrayerApi` under `src/main/` after the flavor split.

**Test counts (Jun 2026):** **414** JVM `@Test` in `app/src/test/java/` (56 files) + **3** instrumented (`androidTest`). Recount: `rg -c '@Test' app/src/test --glob '*.kt' | awk -F: '{s+=$2} END {print s}'`.

**Last Graphify run:** 2026-06-08 — **5252** nodes, **99405** edges (v1.1.5 bottom nav + inset fixes).
