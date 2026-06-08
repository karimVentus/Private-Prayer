#!/usr/bin/env bash
# Build signed release APK (+ optional AAB) and publish a GitHub Release.
# Prerequisites: JDK 21+, Android SDK, keystore (scripts/setup-release-signing.sh), gh CLI.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ -z "${JAVA_HOME:-}" ]]; then
    if [[ -d "$HOME/jdk21" ]]; then
        JAVA_HOME="$HOME/jdk21"
    elif [[ -d "$HOME/.local/share/mise/installs/java/temurin-21.0.11+10.0.LTS" ]]; then
        JAVA_HOME="$HOME/.local/share/mise/installs/java/temurin-21.0.11+10.0.LTS"
    else
        echo "[publish-release] ERROR: set JAVA_HOME to JDK 21 (Temurin). JDK 25+ breaks AGP jlink."
        exit 1
    fi
fi
export JAVA_HOME
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
unset ANDROID_SDK_ROOT

TAG="${1:-}"
if [[ -z "$TAG" ]]; then
    TAG="v$(grep -m1 'versionName' app/build.gradle.kts | sed -E 's/.*"([^"]+)".*/\1/')"
fi

if [[ ! -f keystore.properties ]]; then
    echo "[publish-release] keystore.properties missing — run:"
    echo "  PRAYERTIME_KEYSTORE_PASSWORD='…' ./scripts/setup-release-signing.sh"
    exit 1
fi

echo "[publish-release] Building release (tag target: $TAG)..."
./scripts/release-gate.sh

APK=$(ls app/build/outputs/apk/release/app-release*.apk 2>/dev/null | head -1)
if [[ -z "$APK" ]]; then
    echo "[publish-release] ERROR: release APK not found"
    exit 1
fi

DIST="dist/release"
mkdir -p "$DIST"
OUT_APK="$DIST/Hayya-${TAG}.apk"
cp "$APK" "$OUT_APK"
APK_MB=$(du -m "$OUT_APK" | cut -f1)
echo "[publish-release] Packaged $OUT_APK (${APK_MB} MB)"

if [[ "${PUBLISH_AAB:-0}" == "1" ]]; then
    ./gradlew bundleRelease
    AAB=$(ls app/build/outputs/bundle/release/app-release*.aab 2>/dev/null | head -1)
    if [[ -n "$AAB" ]]; then
        OUT_AAB="$DIST/Hayya-${TAG}.aab"
        cp "$AAB" "$OUT_AAB"
        echo "[publish-release] Packaged $OUT_AAB"
    fi
fi

if [[ "${PUBLISH_GITHUB:-1}" != "1" ]]; then
    echo "[publish-release] Skipping GitHub upload (PUBLISH_GITHUB=0). Install locally:"
    echo "  adb install -r $OUT_APK"
    exit 0
fi

if ! command -v gh >/dev/null 2>&1; then
    echo "[publish-release] gh not found — APK at $OUT_APK"
    echo "  adb install -r $OUT_APK"
    exit 0
fi

NOTES_FILE="$DIST/release-notes-${TAG}.md"
cat >"$NOTES_FILE" <<EOF
## Hayya (حيا) ${TAG}

Signed release APK for sideload install (**com.prayertime**).

### Install
1. Download \`Hayya-${TAG}.apk\` below.
2. On Android: enable install from unknown sources for your browser/files app.
3. Open the APK and install.

Or with USB debugging:
\`\`\`sh
adb install -r Hayya-${TAG}.apk
\`\`\`

### Highlights
- Settings **About** section — app description + GitHub repo link
- App name **Hayya** (حيا) — launcher, widgets, Settings
- Qibla compass, custom adhan sounds, M/L widgets

Built from \`main\` with R8 minification (~${APK_MB} MB).
EOF

if gh release view "$TAG" >/dev/null 2>&1; then
    echo "[publish-release] Release $TAG exists — uploading APK"
    gh release upload "$TAG" "$OUT_APK" --clobber
elif git rev-parse "$TAG" >/dev/null 2>&1; then
    echo "[publish-release] Creating GitHub release for existing tag $TAG"
    gh release create "$TAG" "$OUT_APK" --title "Hayya (حيا) ${TAG}" --notes-file "$NOTES_FILE"
else
    echo "[publish-release] Creating release $TAG from main"
    gh release create "$TAG" "$OUT_APK" --title "Hayya (حيا) ${TAG}" --notes-file "$NOTES_FILE" --target main
fi

echo "[publish-release] Done — see: gh release view $TAG"
