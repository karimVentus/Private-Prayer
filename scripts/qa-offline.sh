#!/usr/bin/env bash
# Phase 5C — Offline robustness QA helper (PHASED_PLAN.md §5C).
#
# Primary target: **com.prayertime** — toggle offline-only vs network in Settings.
#
#   ./scripts/qa-offline.sh audit           Static + unit-test pointers (no device)
#   ./scripts/qa-offline.sh network-off     Airplane mode on (best effort)
#   ./scripts/qa-offline.sh network-on      Restore connectivity
#   ./scripts/qa-offline.sh cache-dump      Room prayer_times rows via run-as
#   ./scripts/qa-offline.sh 5c1             5C.1 launch-in-airplane smoke
#   ./scripts/qa-offline.sh 5c2-guide       7-day cache manual / accelerated procedure
#   ./scripts/qa-offline.sh 5c3             5C.3 network restore smoke
#   ./scripts/qa-offline.sh all             audit + device checks when adb connected
#
# Env: PRAYERTIME_PACKAGE (default com.prayertime for online 5C tests)
#
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SDK="${ANDROID_HOME:-$HOME/Android/Sdk}"
ADB="$SDK/platform-tools/adb"
PACKAGE="${PRAYERTIME_PACKAGE:-com.prayertime}"
ACTIVITY="com.prayertime.ui.MainActivity"
DB_NAME="prayer_times.db"
DB_TABLE="prayer_times"

die() {
  echo "[qa-offline] ERROR: $*" >&2
  exit 1
}

need_adb() {
  [ -x "$ADB" ] || die "adb not found at $ADB (set ANDROID_HOME)"
  "$ADB" get-state >/dev/null 2>&1 || die "no adb device — run ./dev or ./gradlew installDebug first"
}

app_installed() {
  "$ADB" shell pm path "$PACKAGE" 2>/dev/null | grep -q .
}

section() {
  echo ""
  echo "========== $* =========="
}

network_off() {
  echo "[qa-offline] Disabling network (best effort)..."
  "$ADB" shell svc wifi disable >/dev/null 2>&1 || true
  "$ADB" shell svc data disable >/dev/null 2>&1 || true
  "$ADB" shell settings put global airplane_mode_on 1 >/dev/null 2>&1 || true
  "$ADB" shell am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true >/dev/null 2>&1 || true
}

network_on() {
  echo "[qa-offline] Restoring network (best effort)..."
  "$ADB" shell settings put global airplane_mode_on 0 >/dev/null 2>&1 || true
  "$ADB" shell am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false >/dev/null 2>&1 || true
  "$ADB" shell svc wifi enable >/dev/null 2>&1 || true
  "$ADB" shell svc data enable >/dev/null 2>&1 || true
}

launch_app() {
  "$ADB" shell am force-stop "$PACKAGE" >/dev/null 2>&1 || true
  "$ADB" shell am start -W -n "$PACKAGE/$ACTIVITY" >/dev/null
}

cmd_audit() {
  section "5C static audit — cache policy & fallback paths"

  echo "Room retention: cleanupOldEntries keeps last 7 city-calendar days per cityKey"
  echo "  Source: PrayerTimesLocalEngine.cleanupOldEntries → PrayerDayLabels.daysAgo(7)"
  echo "  Unit tests: PrayerTimesLocalEngineTest (cleanupOldEntries *)"
  echo ""

  echo "Online fetch pipeline (network mode ON):"
  echo "  1. Today's Room cache hit → return cache (no network call)"
  echo "  2. Cache miss → Aladhan API"
  echo "  3. NETWORK error + valid coords → local adhan-java fallback (Success)"
  echo "  4. NETWORK error + no coords + no cache → FetchError → Snackbar"
  echo "  Source: OnlinePrayerTimesRepository.fetchTodayTimes"
  echo "  Unit tests: OnlinePrayerTimesRepositoryTest"
  echo ""

  echo "Widget airplane path:"
  echo "  fetchTodayTimes error + getCachedTodayTimes hit → STALE snapshot (not Snackbar)"
  echo "  Source: WidgetSnapshotLoader.errorSnapshot"
  echo ""

  echo "Main UI Snackbar (FetchError only):"
  echo "  PrayerTimeRoot LaunchedEffect → error_fetch_network_online / offline variants"
  echo ""

  if [ -x "$ROOT_DIR/gradlew" ]; then
    echo "Running unit tests (cache + online repo)..."
    (
      cd "$ROOT_DIR"
      export JAVA_HOME="${JAVA_HOME:-$HOME/jdk21}"
      export ANDROID_HOME="$SDK"
      ./gradlew testDebugUnitTest \
        --tests com.prayertime.data.repository.PrayerTimesLocalEngineTest \
        --tests com.prayertime.widget.WidgetSnapshotLoaderTest \
        --tests com.prayertime.data.repository.OnlinePrayerTimesRepositoryTest \
        -q && echo "Unit tests: PASSED"
    )
  fi
  echo "Static audit complete."
}

cmd_cache_dump() {
  need_adb
  app_installed || die "$PACKAGE not installed"
  section "Room cache dump ($PACKAGE)"

  local sql="SELECT cityKey, dateLabel, COUNT(*) AS rows FROM $DB_TABLE GROUP BY cityKey, dateLabel ORDER BY dateLabel DESC;"
  if "$ADB" shell run-as "$PACKAGE" ls "databases/$DB_NAME" >/dev/null 2>&1; then
    "$ADB" shell run-as "$PACKAGE" sqlite3 "databases/$DB_NAME" "$sql" 2>/dev/null || \
      die "sqlite3 failed — emulator image may lack sqlite3 CLI"
  else
    die "database not found — open app once with a city saved"
  fi
}

cmd_5c1() {
  need_adb
  app_installed || die "$PACKAGE not installed — for 5C use: PRAYERTIME_PACKAGE=com.prayertime ./gradlew installOnlineDebug"

  section "5C.1 — airplane mode at launch"
  cat <<EOF
Preconditions (online flavor):
  1. About → Offline-only OFF (network mode)
  2. City saved with coords (e.g. Hameln) — one successful fetch while online
  3. Note today's six prayer times on screen
  4. Force-stop app: adb shell am force-stop $PACKAGE

This script will: enable airplane mode → cold launch app.
EOF

  read -r -p "Continue? [y/N] " ans
  [ "${ans:-}" = "y" ] || [ "${ans:-}" = "Y" ] || exit 0

  network_off
  sleep 1
  launch_app

  echo ""
  echo "Verify on device/emulator:"
  echo "  PASS A: Prayer list visible with six rows (cached or local fallback times)"
  echo "  PASS B (optional Snackbar): only if full FetchError — see audit notes"
  echo "         Typical cache-hit launch: times shown, NO Snackbar (expected today)"
  echo "  FAIL: Loading spinner forever, empty wizard, or crash"
  echo ""
  echo "Logcat: $ADB logcat -d -t 30 | grep -iE 'prayertime|FetchError|IOException'"
  echo "Restore network: ./scripts/qa-offline.sh network-on"
}

cmd_5c2_guide() {
  section "5C.2 — 7 days airplane → 7 days cache accessible"
  cat <<'EOF'
Goal: After up to 7 city-calendar days offline, cached prayer rows remain queryable.

Code guarantee (unit-tested):
  - cleanupOldEntries deletes rows older than 7 city-calendar days
  - Rows exactly 7 days old are kept
  - PrayerTimesLocalEngineTest covers retention edge cases

Accelerated emulator procedure:
  1. Online or offline APK — save Hameln (Europe/Berlin TZ)
  2. ./scripts/qa-offline.sh network-off
  3. For each of 7 days (city TZ):
       a. Set emulator date to that day (Settings → Date, or adb shell date)
       b. Cold launch app — wait for prayer list
       c. ./scripts/qa-offline.sh cache-dump — expect one new dateLabel per day
  4. On day 8 (optional): oldest day should drop after a fetch triggers cleanup

Overnight alternative:
  - Leave airplane on for 7 real days; open app once per day; run cache-dump daily

Pass criteria:
  - cache-dump shows up to 7 distinct dateLabels for the cityKey
  - App displays times for each day opened (local calc when network mode + offline)
  - Day 8+: dateLabel from day 1 removed (if cleanup ran)

Record in PHASED_PLAN.md §5C.2 when signed off.
EOF
}

cmd_5c3() {
  need_adb
  app_installed || die "$PACKAGE not installed"

  section "5C.3 — re-enable network → fresh fetch on next launch"
  cat <<EOF
Important (current behavior):
  OnlinePrayerTimesRepository returns today's Room cache BEFORE calling the API.
  A fresh Aladhan fetch on launch only happens when today's cache is EMPTY
  (new city day, invalidateTodayCache, or first fetch ever).

Procedure:
  1. Complete 5C.2 or ensure today's cache exists from local fallback while offline
  2. ./scripts/qa-offline.sh network-on
  3. Optional — force API refetch: About → "Refresh today's times" (invalidates cache)
  4. Force-stop + relaunch: adb shell am force-stop $PACKAGE && launch

Pass (cache still valid, same day):
  - Times still shown; may be local-calc cache, not necessarily new API data

Pass (after Refresh today's times OR new city day, network ON):
  - Times reload; online flavor may show API wall times (check logcat for HTTP)
  - Widget updates to READY (not STALE)

Monitor API:
  $ADB logcat -s OkHttp:* | grep -i aladhan

Fail:
  - FetchError Snackbar when network is up and coords valid
  - Empty prayer list with saved city
EOF

  read -r -p "Run network-on + relaunch now? [y/N] " ans
  if [ "${ans:-}" = "y" ] || [ "${ans:-}" = "Y" ]; then
    network_on
    sleep 2
    launch_app
    echo "Relaunched with network restored."
  fi
}

cmd_all() {
  cmd_audit
  if [ -x "$ADB" ] && "$ADB" get-state >/dev/null 2>&1; then
    if app_installed; then
      cmd_cache_dump
    else
      echo "[qa-offline] Device connected but $PACKAGE not installed"
      echo "  Run: ./dev   or   ./gradlew installDebug"
    fi
  else
    echo "[qa-offline] No adb device — static audit only"
  fi
}

usage() {
  sed -n '2,16p' "$0" | sed 's/^# \?//'
  exit 0
}

main="${1:-all}"
case "$main" in
  -h | --help | help) usage ;;
  audit) cmd_audit ;;
  network-off) need_adb && network_off && "$ADB" shell dumpsys connectivity | sed -n '1,40p' ;;
  network-on) need_adb && network_on ;;
  cache-dump) cmd_cache_dump ;;
  5c1) cmd_5c1 ;;
  5c2 | 5c2-guide) cmd_5c2_guide ;;
  5c3) cmd_5c3 ;;
  all) cmd_all ;;
  *)
    die "unknown command: $main (try --help)"
    ;;
esac
