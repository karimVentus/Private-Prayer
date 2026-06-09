#!/usr/bin/env bash
set -e

# Setup JDK and Android SDK Paths
# shellcheck source=scripts/resolve-java-home.sh
source "$(cd "$(dirname "$0")/.." && pwd)/scripts/resolve-java-home.sh"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
unset ANDROID_SDK_ROOT

echo "=============================================="
echo "[smoke-ci] Starting project verification..."
echo "=============================================="

# 1. Build + unit tests first (lint runs separately so Robolectric is not starved on CI)
echo "[smoke-ci] Running build and unit tests..."
./gradlew clean assembleDebug testDebugUnitTest

echo "[smoke-ci] Running lint and static analysis..."
./gradlew lintDebug ktlintCheck detekt

# 2. Verify APK Size
echo "[smoke-ci] Verifying APK size..."
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    APK_SIZE_KB=$(du -k "$APK_PATH" | cut -f1)
    APK_SIZE_MB=$(awk "BEGIN {print $APK_SIZE_KB/1024}")
    echo "[smoke-ci] APK path: $APK_PATH"
    echo "[smoke-ci] APK size: ${APK_SIZE_MB} MB"

    # 30MB = 30720 KB (debug APK: adhan audio + full city catalog ~2766 cities + widgets + Hijri)
    if [ "$APK_SIZE_KB" -gt 30720 ]; then
        echo "=============================================="
        echo "[smoke-ci] WARNING: APK size (${APK_SIZE_MB} MB) exceeds the 30MB target!"
        echo "=============================================="
    else
        echo "[smoke-ci] APK size check passed! (${APK_SIZE_MB} MB < 30 MB)"
    fi
else
    echo "[smoke-ci] ERROR: APK file not found!"
    exit 1
fi

echo "=============================================="
echo "[smoke-ci] Verification SUCCESSFUL! 🎉"
echo "=============================================="
echo "[smoke-ci] Room exported-schema check (AppDatabaseMigrationInstrumentedTest) is device-only — not in this gate."
echo "[smoke-ci] Before release: ./dev && ./gradlew connectedDebugAndroidTest \\"
echo "  -Pandroid.testInstrumentationRunnerArguments.class=com.prayertime.data.local.AppDatabaseMigrationInstrumentedTest"
