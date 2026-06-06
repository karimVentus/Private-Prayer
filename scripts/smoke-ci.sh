#!/usr/bin/env bash
set -e

# Setup JDK and Android SDK Paths
export JAVA_HOME=$HOME/jdk21
export ANDROID_HOME=$HOME/Android/Sdk
unset ANDROID_SDK_ROOT

echo "=============================================="
echo "[smoke-ci] Starting project verification..."
echo "=============================================="

# 1. Run all checks in a single Gradle invocation
# This is faster and avoids KSP incremental cache/file locking issues
echo "[smoke-ci] Running build, test, lint, and static analysis tasks..."
./gradlew clean assembleOfflineDebug testOfflineDebugUnitTest lintOfflineDebug assembleOnlineDebug testOnlineDebugUnitTest ktlintCheck detekt

# 2. Verify APK Size
echo "[smoke-ci] Verifying APK size..."
APK_PATH="app/build/outputs/apk/offline/debug/app-offline-debug.apk"
if [ -f "$APK_PATH" ]; then
    APK_SIZE_KB=$(du -k "$APK_PATH" | cut -f1)
    APK_SIZE_MB=$(awk "BEGIN {print $APK_SIZE_KB/1024}")
    echo "[smoke-ci] APK path: $APK_PATH"
    echo "[smoke-ci] APK size: ${APK_SIZE_MB} MB"

    # 25MB = 25600 KB (raised from 12MB; debug APKs now ~21.3MB with audio files + widgets + Hijri)
    if [ "$APK_SIZE_KB" -gt 25600 ]; then
        echo "=============================================="
        echo "[smoke-ci] WARNING: APK size (${APK_SIZE_MB} MB) exceeds the 25MB target!"
        echo "=============================================="
    else
        echo "[smoke-ci] APK size check passed! (${APK_SIZE_MB} MB < 25 MB)"
    fi
else
    echo "[smoke-ci] ERROR: APK file not found!"
    exit 1
fi

echo "=============================================="
echo "[smoke-ci] Verification SUCCESSFUL! 🎉"
echo "=============================================="
echo "[smoke-ci] Room exported-schema check (AppDatabaseMigrationInstrumentedTest) is device-only — not in this gate."
echo "[smoke-ci] Before release: ./dev && ./gradlew connectedOfflineDebugAndroidTest \\"
echo "  -Pandroid.testInstrumentationRunnerArguments.class=com.prayertime.data.local.AppDatabaseMigrationInstrumentedTest"
