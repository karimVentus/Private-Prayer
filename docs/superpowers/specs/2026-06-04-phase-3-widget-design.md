# Phase 3 — Home Screen Widget Design

**Date:** 2026-06-04  
**Status:** Approved for implementation (user waived 2H manual sign-off to start Phase 3)  
**Branch:** `feat/phase-3-widget` (new, off current `cursor/phase-2-manual-qa-signoff`)

---

## Goal

Add a home-screen App Widget that shows the saved city, next prayer with countdown, and all six daily times. Widget reads from the same Room cache and repository as the main app; works offline; refreshes every 30 minutes and at each prayer boundary.

---

## Approaches Considered

| Approach | Pros | Cons |
|----------|------|------|
| **A — RemoteViews + WorkManager + prayer-boundary alarm (recommended)** | Matches existing Hilt/WorkManager patterns; survives process death; offline via Room; immediate refresh at prayer time without keeping app alive | Two refresh mechanisms (30 min + alarm); RemoteViews layout is XML not Compose |
| **B — Glance AppWidget (Compose)** | Modern API, Compose styling | New dependency; minSdk 23 support needs Glance version audit; team has zero Glance code |
| **C — `updatePeriodMillis` only (no worker)** | Simplest | Platform minimum 30 min; no exact prayer-boundary update; stale countdown between ticks |

**Decision:** Approach A. Reuses `PrayerRefreshWork` / `@HiltWorker` / `@AndroidEntryPoint` receiver patterns already in the codebase.

---

## Architecture

```
PrayerTimeWidgetProvider (@AndroidEntryPoint)
    └── WidgetUpdater (singleton, Hilt)
            ├── WidgetSnapshotLoader → PrayerTimesRepository + PrayerTimeCalculator
            ├── WidgetRemoteViewsBuilder → RemoteViews from snapshot
            └── WidgetRefreshScheduler
                    ├── WidgetRefreshWork → PeriodicWork 30 min (@HiltWorker WidgetUpdateWorker)
                    └── WidgetPrayerBoundaryReceiver → one-shot alarm at next prayer timestamp

Triggers:
  - App start (WidgetRefreshWork init in Application)
  - Widget placed / system onUpdate (Provider)
  - City saved or cleared (WidgetUpdater.requestImmediateUpdate)
  - PrayerTimesViewModel: next prayer identity changes while app open
  - WidgetUpdateWorker success → reschedule boundary alarm
  - BootCompletedReceiver → also refresh widgets (extend existing receiver)
```

No widget configuration activity (3A.3 deferred). Widget uses the app's saved `CityConfig` from DataStore. Empty state: "Open app to choose a city" with tap → `MainActivity`.

---

## Components

### 3A — Widget UI

| File | Responsibility |
|------|----------------|
| `widget/PrayerTimeWidgetProvider.kt` | Base `AppWidgetProvider`; `@AndroidEntryPoint`; delegates to `WidgetUpdater` |
| `widget/PrayerTimeWidgetProviderSmallTall.kt` | Small tall provider (2×3 cells); extends base |
| `widget/PrayerTimeWidgetProviderSmallWide.kt` | Small wide provider (4×1 cells); extends base |
| `widget/PrayerTimeWidgetProviderLarge.kt` | Large provider (6×3 cells); extends base |
| `widget/WidgetUpdater.kt` | `@Singleton`; `updateAll()` + `requestImmediateUpdate()`; `widgetSizeFor` dispatches to layout by provider class |
| `widget/WidgetSnapshot.kt` | Immutable display model (city, times, nextPrayer, countdownMillis, state enum) |
| `widget/WidgetSnapshotLoader.kt` | Load config + times via repository; `PrayerTimeCalculator.buildResult` |
| `widget/WidgetRemoteViewsBuilder.kt` | Build `RemoteViews` from snapshot; size-specific bind methods (bindSmallTall, bindSmallWide, bindColumns); localized strings |
| `widget/WidgetLocaleContext.kt` | Per-app locale resolution for RemoteViews strings (AppCompat application locales, API 23+ fallback) |
| `widget/WidgetDigitFormatter.kt` | Eastern Arabic digit substitution on M/L time + countdown fields when language is Arabic |
| `widget/CountdownFormatter.kt` | Shared countdown text (extracted from `PrayerTimesScreen`) |
| `res/layout/widget_prayer_times_medium.xml` | Medium (4×4) RemoteViews layout — city label + 6-column grid |
| `res/layout/widget_prayer_times_small_tall.xml` | Small tall (2×3) — next prayer + hour/min countdown |
| `res/layout/widget_prayer_times_small_wide.xml` | Small wide (4×1) — next prayer + compact countdown line |
| `res/layout/widget_prayer_times_large.xml` | Large (6×3) — same as medium + clock display |
| `res/xml/widget_info_*.xml` (4 files) | `minWidth/minHeight` + `targetCell*`, `updatePeriodMillis=1800000`, preview layout per size |
| `res/drawable/widget_background.xml`, `widget_row_*.xml` | Rounded rect + row highlight styling |
| `res/drawable/widget_preview_image_*.xml` (3 files) | Solid rounded rectangles for widget picker preview (static, not real times) |

**Layouts (four sizes):**

1. **Medium (4×4)** — City name + 6-column grid (prayer name above time); accent highlight on next prayer row
2. **Small tall (2×3)** — Next prayer name + hours/minutes countdown in two stacked TextViews
3. **Small wide (4×1)** — Next prayer name + single-line countdown (e.g. "2h 14m")
4. **Large (6×3)** — Same as medium + clock display

Empty/error states for all sizes; tap anywhere → `MainActivity`.

### 3B — Updates

| File | Responsibility |
|------|----------------|
| `worker/WidgetUpdateWorker.kt` | `@HiltWorker`; load snapshot; `WidgetUpdater.updateAll`; on success reschedule boundary alarm |
| `worker/WidgetRefreshWork.kt` | `@Singleton`; enqueue unique periodic work every **30 minutes** (`ExistingPeriodicWorkPolicy.KEEP`) |
| `widget/WidgetPrayerBoundaryScheduler.kt` | Schedule/cancel `AlarmManager` one-shot at next prayer timestamp |
| `widget/WidgetPrayerBoundaryReceiver.kt` | `@AndroidEntryPoint`; on alarm → `WidgetUpdater.requestImmediateUpdate` + reschedule |

**Integration hooks:**

- `PrayerTimeApplication`: inject `WidgetRefreshWork` (same pattern as `prayerRefreshWork`)
- `BootCompletedReceiver.rescheduleAfterBoot`: call `WidgetUpdater.updateAll` after adhan reschedule
- `PrayerTimesViewModel.startCountdownTicker`: when `getNextPrayer` result differs from previous tick, call `WidgetUpdater.requestImmediateUpdate`
- After `saveCityConfig` success in `CitySetupViewModel`: `requestImmediateUpdate`
- After `clearCityConfig`: `requestImmediateUpdate`

**Data source:** `repository.fetchTodayTimes(config)` — already serves Room cache before network; satisfies 3B.3 and offline gate.

### 3C — Tests

| Test | Coverage |
|------|----------|
| `WidgetSnapshotLoaderTest` | Mock repository → Success/NoCity/Error snapshots |
| `CountdownFormatterTest` | Hours/minutes/seconds formatting parity with UI |
| `WidgetRemoteViewsBuilderTest` | Robolectric: apply snapshot → assert key `TextView` texts via `RemoteViews.apply` |
| `WidgetRefreshWorkTest` | Unique work enqueued, KEEP policy, 30 min interval tag |
| `WidgetUpdateWorkerTest` | Success path updates widgets; no city → success skip |
| `WidgetPrayerBoundarySchedulerTest` | Next alarm timestamp matches next prayer |

---

## Manifest & DI

- Register `PrayerTimeWidgetProvider` with `<receiver>` + `@xml/prayer_time_widget_info`
- Register `WidgetPrayerBoundaryReceiver` (exported=false)
- `PrayerTimeApplication`: add `WidgetRefreshWork` injection
- No new permissions (alarms reuse existing `SCHEDULE_EXACT_ALARM` / inexact fallback pattern from `PrayerAlarmScheduler`)

---

## Error Handling

| State | Widget display |
|-------|----------------|
| No city configured | Empty state message |
| Room cache hit | Full times + countdown |
| `fetchTodayTimes` Error | Last cached times if any; else "Unable to load times" |
| Airplane mode | Cached times (same as app) |

---

## Out of Scope (Phase 3)

- Widget configuration activity (choose city per widget instance)
- Hijri date (Phase 4)
- Live 1-second countdown on widget (platform refreshes at 30 min + prayer boundary; countdown is point-in-time at last update)

> **Scope note (2026-06-04):** Multiple widget sizes were originally listed as out of scope but were implemented during the phase (four providers: small tall, small wide, medium, large; four layouts; per-app locale + Eastern Arabic digits on M/L). This is scope growth, not a gap.

---

## Quality Gate Mapping

| Gate | How verified |
|------|----------------|
| Displays on home screen | Manual `./dev` → add widget |
| Refreshes every 30 min | `WidgetRefreshWork` + manual log |
| Correct next prayer + countdown | Unit tests + manual |
| Updates at prayer time | `WidgetPrayerBoundaryReceiver` + manual clock advance |
| Offline cached data | Airplane mode manual + loader test |
| No Phase 1/2 regressions | `./scripts/smoke-ci.sh` |

---

## Docs Updates (on gate pass)

- `PHASED_PLAN.md` — tick 3A–3C + quality gate
- `APP_CREATION_PLAYBOOK.md` — Widget row → Implemented
- `AGENTS.md` — add `widget/` package
- Graphify update after merge
