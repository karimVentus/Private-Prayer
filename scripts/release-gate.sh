#!/usr/bin/env bash
set -e

export JAVA_HOME="${JAVA_HOME:-$HOME/jdk21}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
unset ANDROID_SDK_ROOT

echo "=============================================="
echo "[release-gate] Building release APKs..."
echo "=============================================="

./gradlew assembleOfflineRelease assembleOnlineRelease

OFFLINE_APK=$(ls app/build/outputs/apk/offline/release/app-offline-release*.apk 2>/dev/null | head -1)
ONLINE_APK=$(ls app/build/outputs/apk/online/release/app-online-release*.apk 2>/dev/null | head -1)

if [ -z "$OFFLINE_APK" ] || [ -z "$ONLINE_APK" ]; then
    echo "[release-gate] ERROR: release APK not found"
    exit 1
fi

OFFLINE_KB=$(du -k "$OFFLINE_APK" | cut -f1)
ONLINE_KB=$(du -k "$ONLINE_APK" | cut -f1)
OFFLINE_MB=$(awk "BEGIN {print $OFFLINE_KB/1024}")
ONLINE_MB=$(awk "BEGIN {print $ONLINE_KB/1024}")

echo "[release-gate] offline release: ${OFFLINE_MB} MB ($OFFLINE_APK)"
echo "[release-gate] online release:  ${ONLINE_MB} MB ($ONLINE_APK)"

# 13 MB ceiling — 8 adhan MP3s dominate size; plan target was 10 MB pre-audio-picker
LIMIT_KB=13312
if [ "$OFFLINE_KB" -gt "$LIMIT_KB" ]; then
    echo "[release-gate] FAIL: offline release (${OFFLINE_MB} MB) exceeds 13 MB limit"
    exit 1
fi
if [ "$ONLINE_KB" -gt "$LIMIT_KB" ]; then
    echo "[release-gate] FAIL: online release (${ONLINE_MB} MB) exceeds 13 MB limit"
    exit 1
fi

echo "=============================================="
echo "[release-gate] PASS — both flavors under 13 MB"
echo "=============================================="
