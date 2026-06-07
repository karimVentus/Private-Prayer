#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
export JAVA_HOME="${JAVA_HOME:-$HOME/jdk21}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export PRAYERTIME_EXPORT_SCREENSHOTS=1

cd "$ROOT_DIR"
./gradlew testDebugUnitTest --tests "com.prayertime.widget.WidgetReadmeScreenshotExportTest.exportReadmeWidgetScreenshots"
echo "[export-readme-widget-screenshots] Wrote PNGs under docs/screenshots/"
