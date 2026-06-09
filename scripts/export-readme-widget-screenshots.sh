#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=scripts/resolve-java-home.sh
source "$ROOT_DIR/scripts/resolve-java-home.sh"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export PRAYERTIME_EXPORT_SCREENSHOTS=1

cd "$ROOT_DIR"
./gradlew testDebugUnitTest --tests "com.prayertime.widget.WidgetReadmeScreenshotExportTest.exportReadmeWidgetScreenshots"
echo "[export-readme-widget-screenshots] Wrote PNGs under docs/screenshots/"
