# Graphify — Knowledge Graph Playbook (PrayerTime-)

> **Repo:** PrayerTime- Android (Kotlin / Compose).  
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

**PrayerTime- status:** Phases **0–6** complete (`v1.0.0` tagged Jun 2026). **Phase 7A** Qibla compass signed off Jun 2026 (`feat/qibla-compass`). **Last graph:** see footer.

---

## Phase 2: CLI installation

```bash
which graphify
# If missing:
pnpm install -g graphify-cli
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
| Widget (main) | `PrayerTimeWidgetProvider` (+SmallTall, +SmallWide, +Large), `WidgetUpdater` (`applyThemeChrome`, `readAppThemeSync`), `WidgetSnapshotLoader`, `WidgetRemoteViewsBuilder` (`unifiedColumnHighlight`, `widget_highlight_*`), `widget_prayer_times_medium.xml` (3-band layout), `widget_preview_*` / `widget_initial_*` layouts, `res/xml-v31/` `previewLayout`, `WidgetPrayerBoundaryScheduler`, `WidgetPrayerBoundaryReceiver`, `WidgetLocaleContext`, `WidgetDigitFormatter`, `widget_colors.xml`, `widget_col_highlight_{light,green,dark}.xml`, `WidgetRefreshWork`, `WidgetUpdateWorker` |
| Hijri (main) | `HijriCalculator`, `HijriModels`, `HijriDateFormatter` (`eventNameCellRes`), `HijriCalendarScreen` (`headerDayText`), `CalendarColors` |
| Data (main) | `PrayerTimesRepository`, `LocalPrayerTimesRepository`, `LocalLocationRepository`, `LocationCatalogInitializer`, `PrayerTimesLocalEngine`, `AladhanTimingsMapper`, `LocationDataSource`, `LocationCatalogLoader`, `CityConfigSerializer`, `AppDatabase` (v4), `PrayerTimeMigrations` (`MIGRATION_1_2`…`3_4`), `app/schemas/` (1–4.json) |
| Data (online only) | `OnlinePrayerTimesRepository` (composes local + `PrayerApi`), `AladhanApi`, `AladhanResponse`, `NetworkMapper`, `NetworkModule` (+ debug `HttpLoggingInterceptor`); `network_security_config.xml` TLS pins for `aladhan.com`; `./scripts/verify-aladhan-pins.sh` |
| App | `@HiltAndroidApp` `PrayerTimeApplication`; flavor `RepositoryModule` in `src/offline/` / `src/online/` |
| Domain (main) | `LocalPrayerTimeCalculator`, `PrayerTimeCalculator`, `HijriCalculator`, `QiblaCalculator`, `LocationRepository`, `SearchLocationsUseCase`, `FetchError`, `SaveCityError`, `Prayer` (`SHURUQ` slot) |
| Sensor (main) | `CompassSensor` (accel + magnetometer), `CompassHeading` (azimuth + declination), `CompassEntryPoint` |
| DI (main + flavor) | `DataModule`, `DomainModule`, `AppConfigModule`, `RepositoryModule`, `NetworkModule` (online), `WidgetEntryPoint`, `CompassEntryPoint` |
| Worker (main) | `PrayerRefreshWork`, `PrayerTimeRefreshWorker`, `WidgetRefreshWork`, `WidgetUpdateWorker` (`@HiltWorker`) |
| Tests (shared) | `FakePrayerTimesRepository`, `PrayerTimesViewModelIntegrationTest`, `PrayerTimesLocalEngineTest`, `LocationCatalogLoaderTest`, `LocalLocationRepositoryTest`, `WidgetSnapshotLoaderIntegrationTest`, `PrayerTimeWidgetProviderTest`, `WidgetRefreshWorkTest`, `TextNormalizerTest`, `HijriCalculatorTest`, `QiblaCalculatorTest`, `CompassHeadingTest`, `AppDatabaseMigrationTest` (v1→v4), `AppDatabaseMigrationInstrumentedTest`, `com.prayertime.ui.screens.ComposeScreenSmokeTest` |
| Tests (online) | `testOnline/` — `OnlinePrayerTimesRepositoryTest`, `AladhanApiMockWebServerTest`, `AladhanResponseParsingTest`, `NetworkMapperTest`, `ScenarioPrayerApi` |

Use the graph to avoid stale imports when refactoring repository ↔ flavor-specific API ↔ calculator paths. **Do not** expect `PrayerApi` under `src/main/` after the flavor split.

**Test counts (Jun 2026):** **372** `@Test` in `app/src/test/java/` (+ online-only set if present). Recount: `rg -c '@Test' app/src/test/java app/src/testOnline/java --glob '*.kt'`.

**Last Graphify run:** 2026-06-07 — **3561** nodes, **63159** edges (post-**7A** Qibla compass: `sensor/`, `QiblaScreen`, `QiblaCalculator`).
