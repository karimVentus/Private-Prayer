# Phase 6 Release Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship signed, R8-minified release APK/AAB for both flavors with documented size, updated README/playbook, and a git release tag.

**Architecture:** Gradle release builds already enable R8 (`isMinifyEnabled = true`) with `proguard-rules.pro`. Add release signing via gitignored `keystore.properties`, wire `signingConfigs.release` in `app/build.gradle.kts`, extend CI gate with optional release assemble + size check, then docs/tag after emulator smoke on release build.

**Tech Stack:** Gradle 8.12, AGP 8.7.3, R8, JDK 21, Android SDK 35, Hilt/Room/Compose shrinker rules.

**Baseline (2026-06-06):**
| Artifact | Size | Notes |
|----------|------|-------|
| `app-offline-debug.apk` | 27.8 MB | smoke-ci gate: < 25 MB |
| `app-offline-release-unsigned.apk` | 11.8 MB | R8 minified; ~9.3 MB = 8 adhan MP3s |
| `app-online-release-unsigned.apk` | 12.0 MB | + Retrofit/OkHttp/Gson |
| `app-offline-release.aab` | 13.0 MB | bundle builds with debug signing today |

**Open checklist items:** 6.1–6.7, 6.9 (6.8 TLS pinning done).

---

### Task 0: Branch setup

**Files:** none (git only)

- [ ] **Step 1: Create feature branch**

```bash
cd /home/karimodora/Documents/GitHub/PrayerTime-
git checkout -b feat/phase-6-release
```

Expected: new branch off current HEAD (`feat/phase-3-widget` or `main` after merge).

---

### Task 1: 6.1 Version bump

**Files:**
- Modify: `app/build.gradle.kts:32-33`

- [ ] **Step 1: Set release version**

First public release — keep semantic version, ensure monotonic `versionCode`:

```kotlin
versionCode = 1
versionName = "1.0.0"
```

If re-releasing after a pulled tag, bump `versionCode` only.

- [ ] **Step 2: Verify About screen shows version**

Run release on emulator; Settings → About shows `1.0.0` via `BuildConfig.VERSION_NAME`.

- [ ] **Step 3: Commit**

```bash
git add app/build.gradle.kts
git commit -m "chore(release): set version 1.0.0 (versionCode 1)"
```

---

### Task 2: 6.2 R8 / ProGuard hardening

**Files:**
- Modify: `app/proguard-rules.pro`
- Test: `./gradlew assembleOfflineRelease assembleOnlineRelease`

R8 is already enabled. Harden rules for Hilt, Room, Gson (online), WorkManager, and `@Serializable`/DataStore if shrink warnings appear.

- [ ] **Step 1: Add standard keep rules**

Append to `app/proguard-rules.pro`:

```proguard
# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Gson (online flavor — Retrofit converter)
-keepattributes InnerClasses,EnclosingMethod
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# WorkManager workers
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker
-keep class com.prayertime.worker.** { *; }

# Hilt workers
-keep class * extends androidx.hilt.work.HiltWorker
-keepclassmembers class * {
  @dagger.assisted.AssistedInject <init>(...);
}

# Receivers / alarms (manifest-instantiated)
-keep class com.prayertime.alarm.** { *; }
-keep class com.prayertime.notification.** { *; }
```

- [ ] **Step 2: Build both release variants**

```bash
export JAVA_HOME=$HOME/jdk21
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew assembleOfflineRelease assembleOnlineRelease
```

Expected: `BUILD SUCCESSFUL`, no R8 errors.

- [ ] **Step 3: Emulator smoke on release APK**

```bash
./dev --headless   # or ./dev if window OK
adb install -r app/build/outputs/apk/offline/release/app-offline-release-unsigned.apk
adb shell am start -n com.prayertime.offline/com.prayertime.ui.MainActivity
```

Manual checks: wizard/city load, prayer rows, countdown tick, widget add, adhan toggle, Settings theme/language.

- [ ] **Step 4: Commit**

```bash
git add app/proguard-rules.pro
git commit -m "chore(release): harden R8 keep rules for Hilt/Room/workers"
```

---

### Task 3: 6.3 Release signing

**Files:**
- Create: `keystore.properties.example`
- Modify: `.gitignore` (add `keystore.properties`, `*.jks`, `*.keystore`)
- Modify: `app/build.gradle.kts` (signingConfigs + release buildType)

- [ ] **Step 1: Generate upload keystore (one-time, local only)**

```bash
keytool -genkeypair -v \
  -keystore ~/prayertime-upload.jks \
  -alias prayertime \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass '<STORE_PASS>' -keypass '<KEY_PASS>' \
  -dname "CN=PrayerTime, OU=Mobile, O=PrayerTime, L=Unknown, ST=Unknown, C=DE"
```

Never commit the `.jks` file or passwords.

- [ ] **Step 2: Create `keystore.properties` (gitignored)**

```properties
storeFile=/home/USER/prayertime-upload.jks
storePassword=STORE_PASS
keyAlias=prayertime
keyPassword=KEY_PASS
```

- [ ] **Step 3: Wire signing in `app/build.gradle.kts`**

```kotlin
import java.util.Properties

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

android {
    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}
```

- [ ] **Step 4: Build signed artifacts**

```bash
./gradlew assembleOfflineRelease bundleOfflineRelease assembleOnlineRelease bundleOnlineRelease
```

Expected outputs:
- `app/build/outputs/apk/offline/release/app-offline-release.apk` (signed)
- `app/build/outputs/bundle/offlineRelease/app-offline-release.aab` (signed)

- [ ] **Step 5: Commit scaffold only**

```bash
git add keystore.properties.example .gitignore app/build.gradle.kts
git commit -m "chore(release): add release signing config (keystore.properties)"
```

---

### Task 4: 6.4 APK size verification

**Files:**
- Modify: `scripts/smoke-ci.sh` or create `scripts/release-gate.sh`
- Modify: `PHASED_PLAN.md` (size target note if adjusted)

- [ ] **Step 1: Measure signed release sizes**

```bash
du -k app/build/outputs/apk/offline/release/app-offline-release.apk | awk '{printf "offline release: %.1f MB\n", $1/1024}'
du -k app/build/outputs/apk/online/release/app-online-release.apk | awk '{printf "online release: %.1f MB\n", $1/1024}'
```

- [ ] **Step 2: Size decision**

Plan target was < 10 MB. Current baseline ~11.8 MB offline — dominated by 8 adhan MP3s (~9.3 MB). Options (pick one):
1. **Accept ~12 MB** — update PHASED_PLAN 6.4 target to "≤ 13 MB release" with rationale (8 full adhan tracks).
2. **Compress MP3s** — re-encode at 64–96 kbps mono (~4–5 MB total savings).
3. **Play Asset Delivery** — defer to post-1.0.

Recommended for v1.0.0: option 1 (document honest target) unless user wants compression.

- [ ] **Step 3: Add release size gate script**

Create `scripts/release-gate.sh`:

```bash
#!/usr/bin/env bash
set -e
export JAVA_HOME=$HOME/jdk21
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew assembleOfflineRelease assembleOnlineRelease
OFFLINE_KB=$(du -k app/build/outputs/apk/offline/release/app-offline-release*.apk | head -1 | cut -f1)
LIMIT_KB=13312  # 13 MB
if [ "$OFFLINE_KB" -gt "$LIMIT_KB" ]; then
  echo "FAIL: offline release ${OFFLINE_KB}KB > ${LIMIT_KB}KB"
  exit 1
fi
echo "PASS: offline release size OK"
```

- [ ] **Step 4: Commit**

```bash
git add scripts/release-gate.sh PHASED_PLAN.md
git commit -m "chore(release): add release size gate (~13 MB ceiling)"
```

---

### Task 5: 6.5 Playbook audit

**Files:**
- Modify: `APP_CREATION_PLAYBOOK.md` (feature table + release row)
- Modify: `PHASED_PLAN.md` (check off 6.1–6.4 as done)
- Modify: `AGENTS.md` (phase status table)

- [ ] **Step 1: Add release row to playbook feature table**

| Release build (R8 + signed) | **Done** | offline + online APK/AAB; see README release section |

- [ ] **Step 2: Mark completed Phase 6 items in PHASED_PLAN.md**

- [ ] **Step 3: Commit**

```bash
git add APP_CREATION_PLAYBOOK.md PHASED_PLAN.md AGENTS.md
git commit -m "docs(release): mark Phase 6 build items complete"
```

---

### Task 6: 6.6 README update

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Add Release section**

```markdown
## Release

Requires JDK 21, Android SDK, and `keystore.properties` (copy from `keystore.properties.example`).

```sh
export JAVA_HOME=$HOME/jdk21
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew assembleOfflineRelease bundleOfflineRelease   # Play Store: offline flavor
./scripts/release-gate.sh                               # size check
./scripts/smoke-ci.sh                                 # full CI gate before tag
```

Signed APK: `app/build/outputs/apk/offline/release/app-offline-release.apk` (~12 MB).
Signed AAB: `app/build/outputs/bundle/offlineRelease/app-offline-release.aab`.
```

- [ ] **Step 2: Update feature list** — themes (light/green/dark), per-prayer mute, adhan sound picker, widgets, Hijri calendar.

- [ ] **Step 3: Commit**

```bash
git add README.md
git commit -m "docs: add release build instructions and feature list"
```

---

### Task 7: 6.9 Pre-tag smoke gate

**Files:** none (run only)

- [ ] **Step 1: Full CI**

```bash
./scripts/smoke-ci.sh
```

Expected: BUILD SUCCESSFUL, APK size < 25 MB (debug).

- [ ] **Step 2: Release gate**

```bash
./scripts/release-gate.sh
```

Expected: PASS.

- [ ] **Step 3: Optional instrumented migration test**

```bash
./dev --headless
./gradlew connectedOfflineDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.prayertime.data.local.AppDatabaseMigrationInstrumentedTest
```

---

### Task 8: 6.7 Git release tag

**Prerequisites:** User sign-off on emulator smoke + CI green.

- [ ] **Step 1: Merge `feat/phase-6-release` → `main` via PR** (branching rule — no direct push to main)

- [ ] **Step 2: Tag**

```bash
git checkout main
git pull
git tag -a v1.0.0 -m "PrayerTime 1.0.0 — first public release"
git push origin v1.0.0
```

- [ ] **Step 3: Mark 6.7 done in PHASED_PLAN.md**

---

## Self-review (plan vs spec)

| Spec item | Task |
|-----------|------|
| 6.1 version bump | Task 1 |
| 6.2 R8 minification | Task 2 (mostly done; harden + smoke) |
| 6.3 signed APK/AAB | Task 3 |
| 6.4 APK size | Task 4 |
| 6.5 playbook audit | Task 5 |
| 6.6 README | Task 6 |
| 6.7 git tag | Task 8 |
| 6.8 TLS pinning | Already done |
| 6.9 smoke-ci | Task 7 |

No placeholders remain. Size target explicitly calls for a product decision (accept ~12 MB vs compress audio).
