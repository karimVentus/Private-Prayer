# Phase 3 Home Screen Widget Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a home-screen widget showing city, next-prayer countdown, and six daily times with 30-minute periodic refresh and prayer-boundary immediate updates.

**Architecture:** RemoteViews widget driven by `WidgetUpdater` singleton; data from existing `PrayerTimesRepository` + `PrayerTimeCalculator`; periodic `WidgetUpdateWorker` (30 min) plus one-shot boundary alarm receiver; Hilt injection on provider/receivers/workers matching existing alarm/worker patterns.

**Tech Stack:** Kotlin, AppWidgetProvider, RemoteViews, WorkManager (@HiltWorker), AlarmManager, Hilt, Robolectric unit tests.

**Spec:** [`docs/superpowers/specs/2026-06-04-phase-3-widget-design.md`](../specs/2026-06-04-phase-3-widget-design.md)

---

### Task 0: Branch setup

**Files:** none (git only)

- [ ] **Step 1: Create feature branch**

```bash
cd /home/karimodora/Documents/GitHub/PrayerTime-
git checkout -b feat/phase-3-widget
```

Expected: new branch off current HEAD.

---

### Task 1: Shared countdown formatter

**Files:**
- Create: `app/src/main/java/com/prayertime/widget/CountdownFormatter.kt`
- Modify: `app/src/main/java/com/prayertime/ui/screens/PrayerTimesScreen.kt`
- Test: `app/src/test/java/com/prayertime/widget/CountdownFormatterTest.kt`

- [ ] **Step 1: Write failing test**

```kotlin
package com.prayertime.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class CountdownFormatterTest {
    @Test
    fun formatsHoursMinutes_whenOverOneHour() {
        val text =
            CountdownFormatter.format(
                millis = 2 * 3_600_000L + 14 * 60_000L,
                hoursUnit = "h",
                minutesUnit = "m",
                secondsUnit = "s",
            )
        assertEquals("2h 14m", text)
    }

    @Test
    fun formatsMinutesSeconds_whenUnderOneHour() {
        val text =
            CountdownFormatter.format(
                millis = 5 * 60_000L + 9_000L,
                hoursUnit = "m",
                minutesUnit = "s",
                secondsUnit = "x",
            )
        assertEquals("5s 9x", text)
    }
}
```

- [ ] **Step 2: Run test — expect FAIL**

```bash
export JAVA_HOME=$HOME/jdk21 ANDROID_HOME=$HOME/Android/Sdk
./gradlew testOfflineDebugUnitTest --tests com.prayertime.widget.CountdownFormatterTest
```

- [ ] **Step 3: Implement formatter + refactor screen**

Create `CountdownFormatter.kt`:

```kotlin
package com.prayertime.widget

object CountdownFormatter {
    fun format(
        millis: Long,
        hoursUnit: String,
        minutesUnit: String,
        secondsUnit: String,
    ): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return buildString(if (hours > 0) 8 else 6) {
            if (hours > 0) {
                append(hours)
                append(hoursUnit)
                append(' ')
                append(minutes)
                append(minutesUnit)
            } else {
                append(minutes)
                append(minutesUnit)
                append(' ')
                append(seconds)
                append(secondsUnit)
            }
        }
    }
}
```

In `PrayerTimesScreen.kt`: replace private `formatCountdownMillis` body with `CountdownFormatter.format(...)` call; delete the private function.

- [ ] **Step 4: Run test — expect PASS**

```bash
./gradlew testOfflineDebugUnitTest --tests com.prayertime.widget.CountdownFormatterTest
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/prayertime/widget/CountdownFormatter.kt \
  app/src/main/java/com/prayertime/ui/screens/PrayerTimesScreen.kt \
  app/src/test/java/com/prayertime/widget/CountdownFormatterTest.kt
git commit -m "$(cat <<'EOF'
refactor: extract CountdownFormatter for widget reuse

EOF
)"
```

---

### Task 2: Widget snapshot model + loader

**Files:**
- Create: `app/src/main/java/com/prayertime/widget/WidgetSnapshot.kt`
- Create: `app/src/main/java/com/prayertime/widget/WidgetSnapshotLoader.kt`
- Test: `app/src/test/java/com/prayertime/widget/WidgetSnapshotLoaderTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
package com.prayertime.widget

import com.prayertime.data.repository.PrayerTimesRepository
import com.prayertime.domain.model.CityConfig
import com.prayertime.domain.model.ErrorType
import com.prayertime.domain.model.Prayer
import com.prayertime.domain.model.PrayerTime
import com.prayertime.domain.model.PrayerTimesResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetSnapshotLoaderTest {
    private val repository = mockk<PrayerTimesRepository>()
    private val loader = WidgetSnapshotLoader(repository)

    private val config =
        CityConfig(
            cityName = "Hameln",
            countryCode = "DE",
            timezone = "Europe/Berlin",
            latitude = 52.1,
            longitude = 9.35,
        )

    private val times =
        listOf(
            PrayerTime(Prayer.FAJR, "04:00", 1_700_000_000_000L),
            PrayerTime(Prayer.DUHA, "05:30", 1_700_005_400_000L),
        )

    @Test
    fun noCity_returnsEmptyState() = runTest {
        coEvery { repository.cityConfig } returns flowOf(null)
        val snapshot = loader.load()
        assertEquals(WidgetSnapshot.State.NO_CITY, snapshot.state)
    }

    @Test
    fun success_returnsTimesAndCityLabel() = runTest {
        coEvery { repository.cityConfig } returns flowOf(config)
        coEvery { repository.fetchTodayTimes(config) } returns
            PrayerTimesResult.Success(times, Prayer.FAJR, 60_000L)
        val snapshot = loader.load()
        assertEquals(WidgetSnapshot.State.READY, snapshot.state)
        assertEquals("Hameln, DE", snapshot.cityLabel)
        assertEquals(2, snapshot.times.size)
        assertEquals(Prayer.FAJR, snapshot.nextPrayer)
    }

    @Test
    fun error_withoutCache_returnsErrorState() = runTest {
        coEvery { repository.cityConfig } returns flowOf(config)
        coEvery { repository.fetchTodayTimes(config) } returns
            PrayerTimesResult.Error(ErrorType.NETWORK)
        val snapshot = loader.load()
        assertEquals(WidgetSnapshot.State.ERROR, snapshot.state)
        assertTrue(snapshot.times.isEmpty())
    }
}
```

- [ ] **Step 2: Run test — expect FAIL**

```bash
./gradlew testOfflineDebugUnitTest --tests com.prayertime.widget.WidgetSnapshotLoaderTest
```

- [ ] **Step 3: Implement snapshot + loader**

`WidgetSnapshot.kt`:

```kotlin
package com.prayertime.widget

import com.prayertime.domain.model.Prayer
import com.prayertime.domain.model.PrayerTime

data class WidgetSnapshot(
    val state: State,
    val cityLabel: String = "",
    val times: List<PrayerTime> = emptyList(),
    val nextPrayer: Prayer? = null,
    val countdownMillis: Long = 0L,
) {
    enum class State { NO_CITY, READY, ERROR }
}
```

`WidgetSnapshotLoader.kt`:

```kotlin
package com.prayertime.widget

import com.prayertime.data.repository.PrayerTimesRepository
import com.prayertime.domain.model.PrayerTimesResult
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetSnapshotLoader
    @Inject
    constructor(
        private val repository: PrayerTimesRepository,
    ) {
        suspend fun load(): WidgetSnapshot {
            val config = repository.cityConfig.first() ?: return WidgetSnapshot(WidgetSnapshot.State.NO_CITY)
            return when (val result = repository.fetchTodayTimes(config)) {
                is PrayerTimesResult.Success ->
                    WidgetSnapshot(
                        state = WidgetSnapshot.State.READY,
                        cityLabel = "${config.cityName}, ${config.countryCode}",
                        times = result.times,
                        nextPrayer = result.nextPrayer,
                        countdownMillis = result.countdown,
                    )
                is PrayerTimesResult.Error ->
                    WidgetSnapshot(
                        state = WidgetSnapshot.State.ERROR,
                        cityLabel = "${config.cityName}, ${config.countryCode}",
                    )
            }
        }
    }
```

- [ ] **Step 4: Run test — expect PASS**

- [ ] **Step 5: Commit**

```bash
git commit -m "$(cat <<'EOF'
feat(widget): add WidgetSnapshotLoader

EOF
)"
```

---

### Task 3: RemoteViews layout + builder

**Files:**
- Create: `app/src/main/res/layout/widget_prayer_times_medium.xml`
- Create: `app/src/main/res/layout/widget_prayer_times_small_tall.xml`
- Create: `app/src/main/res/layout/widget_prayer_times_small_wide.xml`
- Create: `app/src/main/res/layout/widget_prayer_times_large.xml`
- Create: `app/src/main/res/drawable/widget_background.xml`, `widget_row_default.xml`, `widget_row_highlight.xml`
- Create: `app/src/main/res/xml/widget_info_medium.xml`, `widget_info_small_tall.xml`, `widget_info_small_wide.xml`, `widget_info_large.xml`
- Create: `app/src/main/res/drawable/widget_preview_image_medium.xml`, `widget_preview_image_tall.xml`, `widget_preview_image_wide.xml`
- Create: `app/src/main/java/com/prayertime/widget/WidgetRemoteViewsBuilder.kt`
- Create: `app/src/main/java/com/prayertime/widget/WidgetLocaleContext.kt`, `WidgetDigitFormatter.kt`
- Create: `app/src/main/java/com/prayertime/widget/PrayerTimeWidgetProviderSmallTall.kt`, `ProviderSmallWide.kt`, `ProviderLarge.kt`
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-ar/strings.xml`

> **Scope note:** Original plan called for a single 4×4 layout. Implementation expanded to four sizes (small tall 2×3, small wide 4×1, medium 4×4, large 6×3) with per-app locale and Eastern Arabic digit support.

- [ ] **Step 1: Add strings**

`values/strings.xml`:

```xml
<string name="widget_description">Today\'s prayer times and next prayer countdown</string>
<string name="widget_no_city">Open the app to choose a city</string>
<string name="widget_error">Unable to load prayer times</string>
```

Arabic equivalents in `values-ar/strings.xml`.

- [ ] **Step 2: Create layout XML**

`widget_prayer_times.xml` — vertical `LinearLayout` with `@drawable/widget_background` padding 12dp:

- `@+id/widget_city` TextView 16sp bold
- `@+id/widget_next` TextView 14sp primary color
- `@+id/widget_rows` include 6 row pairs: `@+id/widget_prayer_0` / `@+id/widget_time_0` … through index 5 (FAJR..ISHA order matching app)
- `@+id/widget_empty` TextView centered, visibility gone by default

`widget_background.xml` — `#FF1E1E2E` rounded 16dp (match dark surface).

`prayer_time_widget_info.xml`:

```xml
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="250dp"
    android:minHeight="180dp"
    android:updatePeriodMillis="1800000"
    android:initialLayout="@layout/widget_prayer_times"
    android:previewLayout="@layout/widget_prayer_times"
    android:description="@string/widget_description"
    android:resizeMode="horizontal|vertical"
    android:widgetCategory="home_screen" />
```

- [ ] **Step 3: Write failing builder test**

Robolectric test applying snapshot and reading TextView text via shadow or `RemoteViews.apply` + findViewById on inflated layout.

- [ ] **Step 4: Implement `WidgetRemoteViewsBuilder`**

Pure class taking `Context`; method `build(snapshot: WidgetSnapshot): RemoteViews`:

- NO_CITY → hide rows, show `widget_empty` with `widget_no_city`
- ERROR → show city + empty message `widget_error`
- READY → populate city, `next_label` via `CountdownFormatter`, 6 rows with `prayerNameRes` helper (duplicate small when-block or share via new `PrayerNames` util in domain — keep widget-local private function to minimize scope)
- Highlight next prayer row background `@drawable/widget_row_highlight`
- `setOnClickPendingIntent(R.id.widget_root, pendingIntent MainActivity)`

- [ ] **Step 5: Run builder test — expect PASS**

- [ ] **Step 6: Commit**

---

### Task 4: WidgetUpdater + Provider

**Files:**
- Create: `app/src/main/java/com/prayertime/widget/WidgetUpdater.kt`
- Create: `app/src/main/java/com/prayertime/widget/PrayerTimeWidgetProvider.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Implement WidgetUpdater**

```kotlin
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val loader: WidgetSnapshotLoader,
    private val viewsBuilder: WidgetRemoteViewsBuilder,
) {
    suspend fun updateAll() {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, PrayerTimeWidgetProvider::class.java))
        if (ids.isEmpty()) return
        val snapshot = loader.load()
        val views = viewsBuilder.build(snapshot)
        ids.forEach { id -> manager.updateAppWidget(id, views) }
    }

    fun requestImmediateUpdate() {
        val request = OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
```

- [ ] **Step 2: Implement Provider**

```kotlin
@AndroidEntryPoint
class PrayerTimeWidgetProvider : AppWidgetProvider() {
    @Inject lateinit var widgetUpdater: WidgetUpdater

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { widgetUpdater.updateAll() } finally { pending.finish() }
        }
    }
}
```

Note: `AppWidgetProvider` injection requires `@AndroidEntryPoint` on provider class (supported in Hilt 2.52).

- [ ] **Step 3: Register in manifest**

Inside `<application>`:

```xml
<receiver
    android:name=".widget.PrayerTimeWidgetProvider"
    android:exported="true"
    android:label="@string/app_name">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/prayer_time_widget_info" />
</receiver>
```

- [ ] **Step 4: Manual smoke — add widget on emulator**

```bash
./dev --headless
# Long-press home → Widgets → Prayer Time
```

- [ ] **Step 5: Commit**

---

### Task 5: WidgetUpdateWorker + WidgetRefreshWork

**Files:**
- Create: `app/src/main/java/com/prayertime/worker/WidgetUpdateWorker.kt`
- Create: `app/src/main/java/com/prayertime/worker/WidgetRefreshWork.kt`
- Modify: `app/src/main/java/com/prayertime/PrayerTimeApplication.kt`
- Test: `app/src/test/java/com/prayertime/worker/WidgetRefreshWorkTest.kt`
- Test: `app/src/test/java/com/prayertime/worker/WidgetUpdateWorkerTest.kt`

- [ ] **Step 1: Write failing WidgetRefreshWorkTest** (mirror `PrayerRefreshWorkTest` — UNIQUE_WORK_NAME, KEEP, 30 min interval constant `REFRESH_INTERVAL_MINUTES = 30L`)

- [ ] **Step 2: Implement WidgetRefreshWork**

```kotlin
@Singleton
class WidgetRefreshWork @Inject constructor(@ApplicationContext context: Context) {
    companion object {
        internal const val UNIQUE_WORK_NAME = "prayer_time_widget_refresh"
        internal const val REFRESH_INTERVAL_MINUTES = 30L
    }
    init { enqueue(context) }
    fun enqueue(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(REFRESH_INTERVAL_MINUTES, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
```

- [ ] **Step 3: Implement WidgetUpdateWorker**

```kotlin
@HiltWorker
class WidgetUpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val widgetUpdater: WidgetUpdater,
    private val boundaryScheduler: WidgetPrayerBoundaryScheduler,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        widgetUpdater.updateAll()
        boundaryScheduler.scheduleNextBoundaryUpdate()
        return Result.success()
    }
}
```

- [ ] **Step 4: Inject in Application**

```kotlin
@Inject lateinit var widgetRefreshWork: WidgetRefreshWork
```

- [ ] **Step 5: Write + run worker tests — expect PASS**

- [ ] **Step 6: Commit**

---

### Task 6: Prayer-boundary immediate refresh

**Files:**
- Create: `app/src/main/java/com/prayertime/widget/WidgetPrayerBoundaryScheduler.kt`
- Create: `app/src/main/java/com/prayertime/widget/WidgetPrayerBoundaryReceiver.kt`
- Modify: `app/src/main/java/com/prayertime/alarm/BootCompletedReceiver.kt`
- Modify: `app/src/main/java/com/prayertime/ui/prayer/PrayerTimesViewModel.kt`
- Modify: `app/src/main/java/com/prayertime/ui/city/CitySetupViewModel.kt`
- Test: `app/src/test/java/com/prayertime/widget/WidgetPrayerBoundarySchedulerTest.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Scheduler** — compute next future prayer timestamp from snapshot times; use `AlarmManager.setExactAndAllowWhileIdle` when exact alarm granted else `setAndAllowWhileIdle` (reuse permission check pattern from `PrayerAlarmScheduler` or inject shared helper)

- [ ] **Step 2: Receiver** — `@AndroidEntryPoint`; inject `WidgetUpdater`; on receive → `requestImmediateUpdate()`

- [ ] **Step 3: Manifest receiver** (exported=false)

- [ ] **Step 4: BootCompletedReceiver** — after adhan block, call `widgetUpdater.updateAll()` and `boundaryScheduler.scheduleNextBoundaryUpdate()`

- [ ] **Step 5: PrayerTimesViewModel** — track `lastWidgetPrayer: Prayer?`; in countdown loop when `getNextPrayer` changes, call `widgetUpdater.requestImmediateUpdate()`

- [ ] **Step 6: CitySetupViewModel** — after successful save, `widgetUpdater.requestImmediateUpdate()`

- [ ] **Step 7: Scheduler unit test** — mock times, assert trigger millis

- [ ] **Step 8: Commit**

---

### Task 7: ProGuard + full CI

**Files:**
- Modify: `app/proguard-rules.pro`

- [ ] **Step 1: Keep widget provider + receivers**

```
-keep class com.prayertime.widget.** { *; }
```

- [ ] **Step 2: Run smoke CI**

```bash
export JAVA_HOME=$HOME/jdk21 ANDROID_HOME=$HOME/Android/Sdk
./scripts/smoke-ci.sh
```

Expected: detekt, ktlint, lint, both flavor builds, ~135+ offline tests green.

- [ ] **Step 3: Commit if proguard changed**

---

### Task 8: Documentation + Graphify

**Files:**
- Modify: `PHASED_PLAN.md`, `APP_CREATION_PLAYBOOK.md`, `AGENTS.md`

- [ ] **Step 1: Tick Phase 3 checkboxes in PHASED_PLAN.md**

- [ ] **Step 2: Playbook Widget row → Implemented**

- [ ] **Step 3: AGENTS.md — add `widget/` to structure tree**

- [ ] **Step 4: Graphify**

```bash
OPENAI_API_KEY="" graphify update . --no-cluster
```

- [ ] **Step 5: Commit docs**

```bash
git commit -m "$(cat <<'EOF'
docs: mark Phase 3 widget complete

EOF
)"
```

---

## Verification (manual)

1. `./dev` — install offline debug, set city Hameln
2. Add widget to home screen — city, 6 times, next prayer line visible
3. Airplane mode — widget still shows cached times
4. Advance emulator clock to next prayer — widget updates within ~1 min of boundary alarm
5. Change city in app — widget updates on return to home screen

## Self-Review Checklist

- [x] Spec 3A.1–3A.2 → Tasks 3–4
- [x] Spec 3A.3 deferred (no config activity)
- [x] Spec 3B.1–3B.3 → Tasks 5–6
- [x] Spec 3C.1–3C.3 → Tasks 1–6 tests
- [x] No GPS, no Glance dependency, no config activity
- [x] Follows Hilt + WorkManager patterns from Phase 2G
