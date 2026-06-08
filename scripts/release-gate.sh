#!/usr/bin/env bash
set -e

if [[ -z "${JAVA_HOME:-}" ]]; then
    if [[ -d "$HOME/jdk21" ]]; then
        JAVA_HOME="$HOME/jdk21"
    elif [[ -d "$HOME/.local/share/mise/installs/java/temurin-21.0.11+10.0.LTS" ]]; then
        JAVA_HOME="$HOME/.local/share/mise/installs/java/temurin-21.0.11+10.0.LTS"
    else
        echo "[release-gate] ERROR: set JAVA_HOME to JDK 21 (Temurin). JDK 25+ breaks AGP jlink."
        exit 1
    fi
fi
export JAVA_HOME
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
unset ANDROID_SDK_ROOT

echo "=============================================="
echo "[release-gate] Building release APK..."
echo "=============================================="

./gradlew assembleRelease

RELEASE_APK=$(ls app/build/outputs/apk/release/app-release*.apk 2>/dev/null | head -1)

if [ -z "$RELEASE_APK" ]; then
    echo "[release-gate] ERROR: release APK not found"
    exit 1
fi

RELEASE_KB=$(du -k "$RELEASE_APK" | cut -f1)
RELEASE_MB=$(awk "BEGIN {print $RELEASE_KB/1024}")

echo "[release-gate] release: ${RELEASE_MB} MB ($RELEASE_APK)"

# 13 MB ceiling — 8 adhan MP3s dominate size; plan target was 10 MB pre-audio-picker
LIMIT_KB=13312
if [ "$RELEASE_KB" -gt "$LIMIT_KB" ]; then
    echo "[release-gate] FAIL: release (${RELEASE_MB} MB) exceeds 13 MB limit"
    exit 1
fi

echo "=============================================="
echo "[release-gate] PASS — release under 13 MB"
echo "=============================================="
