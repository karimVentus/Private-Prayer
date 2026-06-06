#!/usr/bin/env bash
# Phase 5A — Doze mode & battery QA helper (PHASED_PLAN.md §5A).
#
#   ./scripts/qa-doze.sh audit          Static code/manifest checks (5A.3, no device)
#   ./scripts/qa-doze.sh alarms         Dump AlarmManager entries for PrayerTime
#   ./scripts/qa-doze.sh doze-on        Force device into Doze (emulator-friendly)
#   ./scripts/qa-doze.sh doze-off       Exit forced Doze
#   ./scripts/qa-doze.sh wakelocks      Runtime wakelock / job snapshot (5A.3)
#   ./scripts/qa-doze.sh 5a2            Short Doze alarm smoke (needs city + adhan ON)
#   ./scripts/qa-doze.sh 5a1-guide      Print overnight 8h manual procedure (5A.1)
#   ./scripts/qa-doze.sh all            audit + device checks when adb connected
#
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SDK="${ANDROID_HOME:-$HOME/Android/Sdk}"
ADB="$SDK/platform-tools/adb"
PACKAGE="${PRAYERTIME_PACKAGE:-com.prayertime.offline}"

die() {
  echo "[qa-doze] ERROR: $*" >&2
  exit 1
}

need_adb() {
  [ -x "$ADB" ] || die "adb not found at $ADB (set ANDROID_HOME)"
  "$ADB" get-state >/dev/null 2>&1 || die "no adb device — run ./dev first"
}

app_installed() {
  "$ADB" shell pm path "$PACKAGE" 2>/dev/null | grep -q .
}

section() {
  echo ""
  echo "========== $* =========="
}

cmd_audit() {
  section "5A.3 static audit — wakelocks & background work"
  local issues=0

  if grep -q 'WAKE_LOCK' "$ROOT_DIR/app/src/main/AndroidManifest.xml" 2>/dev/null; then
    echo "FAIL: WAKE_LOCK permission in manifest"
    issues=$((issues + 1))
  else
    echo "OK: no WAKE_LOCK permission in manifest"
  fi

  if grep -rE 'WakeLock|\.acquire\(|PARTIAL_WAKE_LOCK' "$ROOT_DIR/app/src/main/java" 2>/dev/null | grep -v '.kt:.*PowerManager' | grep -q .; then
    echo "FAIL: WakeLock usage in main source"
    grep -rE 'WakeLock|\.acquire\(|PARTIAL_WAKE_LOCK' "$ROOT_DIR/app/src/main/java" || true
    issues=$((issues + 1))
  else
    echo "OK: no WakeLock.acquire in app code"
  fi

  if grep -q 'FOREGROUND_SERVICE' "$ROOT_DIR/app/src/main/AndroidManifest.xml" 2>/dev/null; then
    echo "FAIL: foreground service declared"
    issues=$((issues + 1))
  else
    echo "OK: no foreground service"
  fi

  echo ""
  echo "Background workers (WorkManager periodic):"
  grep -E 'UNIQUE_WORK_NAME|REFRESH_INTERVAL' \
    "$ROOT_DIR/app/src/main/java/com/prayertime/worker/PrayerRefreshWork.kt" \
    "$ROOT_DIR/app/src/main/java/com/prayertime/worker/WidgetRefreshWork.kt" || true

  echo ""
  echo "Adhan alarm API (Doze-safe):"
  echo "  Primary: AlarmManager.setAlarmClock (useReliableAlarms=true, boot reschedule)"
  echo "  Fallback: AlarmManager.setAndAllowWhileIdle (exact-alarm denied / unreliable path)"
  grep -n 'setAlarmClock\|setAndAllowWhileIdle\|setExactAndAllowWhileIdle' \
    "$ROOT_DIR/app/src/main/java/com/prayertime/alarm/PrayerAlarmScheduler.kt" || true

  echo ""
  echo "Day rollover without WorkManager (app open): PrayerTimesViewModel ticker + needsPrayerDayRefresh"
  echo "Day rollover after long Doze (app closed): refresh on next launch via stale check + BootCompletedReceiver"

  if [ "$issues" -gt 0 ]; then
    die "$issues static audit issue(s)"
  fi
  echo "Static audit PASSED"
}

cmd_alarms() {
  need_adb
  app_installed || die "$PACKAGE not installed — run ./dev"
  section "AlarmManager dump ($PACKAGE)"
  "$ADB" shell dumpsys alarm | grep -E "$PACKAGE|RTC_WAKEUP|setAlarmClock|origWhen|Alarm clock" || {
    echo "(no matching alarm lines — adhan may be off or all prayers passed)"
  }
  echo ""
  if "$ADB" shell dumpsys alarm 2>/dev/null | grep -A8 "ACTION_PRAYER_ALARM" | grep -q "window=+1h"; then
    echo "WARN: Prayer alarms use inexact 1h window — Adhan will NOT fire on time in Doze."
    echo "      Reinstall app (USE_EXACT_ALARM) or grant: adb shell appops set $PACKAGE SCHEDULE_EXACT_ALARM allow"
    echo "      Then open the app once to reschedule. Expect 'Alarm clock:' + window=0 in dump."
  fi
}

cmd_doze_on() {
  need_adb
  section "Forcing Doze (deviceidle)"
  "$ADB" shell dumpsys deviceidle enable
  "$ADB" shell dumpsys battery unplug
  "$ADB" shell dumpsys deviceidle force-idle
  echo "Doze forced. State:"
  "$ADB" shell dumpsys deviceidle get deep
  "$ADB" shell dumpsys deviceidle get light
}

cmd_doze_off() {
  need_adb
  section "Exiting forced Doze"
  "$ADB" shell dumpsys deviceidle unforce
  "$ADB" shell dumpsys deviceidle disable
  "$ADB" shell dumpsys battery reset
  echo "Doze force cleared, battery reset."
}

cmd_wakelocks() {
  need_adb
  app_installed || die "$PACKAGE not installed"
  section "5A.3 runtime — wakelocks & jobs ($PACKAGE)"

  echo "--- Active partial wakelocks (should be empty for PrayerTime) ---"
  "$ADB" shell dumpsys power | grep -i -A2 "$PACKAGE" || echo "(none for $PACKAGE)"

  echo ""
  echo "--- WorkManager jobs ---"
  "$ADB" shell dumpsys jobscheduler | grep -E "$PACKAGE|prayer_time" || echo "(no matching job lines)"

  echo ""
  echo "--- Batterystats wakelock summary (since last unplug) ---"
  "$ADB" shell dumpsys batterystats --checkin "$PACKAGE" 2>/dev/null \
    | grep -i wake || echo "(no wakelock lines in checkin for $PACKAGE)"
}

cmd_5a2() {
  need_adb
  app_installed || die "$PACKAGE not installed — run ./dev and complete city setup with adhan ON"

  section "5A.2 — AlarmManager fires in Doze"
  echo "Package: $PACKAGE (override with PRAYERTIME_PACKAGE=com.prayertime for online flavor)"
  echo "Precondition: adhan notifications ON, city configured, at least one future prayer today."
  echo ""

  cmd_alarms

  if "$ADB" shell dumpsys alarm 2>/dev/null | grep -A8 "ACTION_PRAYER_ALARM" | grep -q "window=+1h"; then
    echo ""
    echo "Attempting appops grant for SCHEDULE_EXACT_ALARM on $PACKAGE..."
    "$ADB" shell appops set "$PACKAGE" SCHEDULE_EXACT_ALARM allow 2>/dev/null || true
  fi

  echo ""
  echo "Rescheduling via BOOT_COMPLETED (same path as BootCompletedReceiver)..."
  "$ADB" shell am broadcast -a android.intent.action.BOOT_COMPLETED -p "$PACKAGE" >/dev/null 2>&1 || true
  sleep 2
  cmd_alarms

  echo ""
  echo "Forcing Doze before next prayer..."
  cmd_doze_on

  echo ""
  echo "Monitor for adhan notification at prayer time:"
  echo "  $ADB logcat -s AdhanAlarmReceiver:* NotificationManager:* | grep -i prayertime"
  echo ""
  echo "Or advance emulator clock to 1 min before next scheduled alarm (see origWhen above), wait 2 min."
  echo "Pass: notification + adhan sound while device remains in Doze."
  echo ""
  echo "When done: ./scripts/qa-doze.sh doze-off"
}

cmd_5a1_guide() {
  section "5A.1 — 8+ hour Doze manual procedure"
  cat <<'EOF'
Goal: After 8+ hours in Doze, prayer times and countdown are correct on wake.

Setup (physical device recommended; emulator acceptable with caveats):
  1. Complete city setup (e.g. Hameln). Note today's Fajr–Isha times on screen.
  2. Enable adhan notifications + grant POST_NOTIFICATIONS + exact alarms if prompted.
  3. Leave app in background (home screen). Do not force-stop.

Overnight / 8h path:
  4. Plug device in, disable manual time changes.
  5. Optional accelerated emulator path (same calendar day):
       ./scripts/qa-doze.sh doze-on
       # leave 8+ hours OR advance system clock next morning before doze-off
  6. After wake / doze-off: open app WITHOUT changing city.

Pass criteria:
  - All six prayer rows match offline calculator for today's city date.
  - Countdown points to the correct next prayer (not stale yesterday).
  - If a prayer occurred during Doze, adhan notification appeared (5A.2 overlap).

Fail signals:
  - Countdown stuck or showing yesterday's next prayer.
  - Empty list / fetch error when cache should exist (offline flavor).
  - Widget shows stale next prayer after app resume.

Recovery already in app:
  - needsPrayerDayRefresh on ViewModel ticker when app is open.
  - Stale fetch on launch when last cache day != city today.
  - BootCompletedReceiver reschedules alarms after reboot (not after Doze wake).

Record result in PHASED_PLAN.md §5A.1 checkbox when signed off.
EOF
}

cmd_all() {
  cmd_audit
  if [ -x "$ADB" ] && "$ADB" get-state >/dev/null 2>&1; then
    if app_installed; then
      cmd_alarms
      cmd_wakelocks
    else
      echo "[qa-doze] Device connected but $PACKAGE not installed — skipping runtime checks"
    fi
  else
    echo "[qa-doze] No adb device — static audit only"
  fi
}

usage() {
  sed -n '2,12p' "$0" | sed 's/^# \?//'
  exit 0
}

main="${1:-all}"
case "$main" in
  -h | --help | help) usage ;;
  audit) cmd_audit ;;
  alarms) cmd_alarms ;;
  doze-on) cmd_doze_on ;;
  doze-off) cmd_doze_off ;;
  wakelocks) cmd_wakelocks ;;
  5a1 | 5a1-guide) cmd_5a1_guide ;;
  5a2) cmd_5a2 ;;
  all) cmd_all ;;
  *)
    die "unknown command: $main (try --help)"
    ;;
esac
