# Prayer Times

**Privacy-first Android prayer times — no GPS, no account.**

Pick a country and city from a bundled catalog, get accurate daily times (Umm al-Qura, Shafi), live countdown to the next prayer, optional adhan notifications, four home-screen widgets, and a Hijri calendar with ten Islamic events. English and Arabic with full RTL support; three themes (light, green, dark).

| | |
|---|---|
| **Version** | 1.0.0 |
| **Package** | `com.prayertime` (online) · `com.prayertime.offline` (privacy-only APK) |
| **Min SDK** | 23 · **Target** 35 |
| **Tests** | 303 shared JVM · 344 online JVM |

---

## Screenshots

### Prayer times

Live countdown, Hijri date, upcoming event banner, per-prayer mute toggles, and offline privacy mode.

| English (light) | Arabic (light) | Arabic (dark) |
|:---:|:---:|:---:|
| ![Prayer times — English, light theme](docs/screenshots/prayer-times-en-light.png) | ![Prayer times — Arabic, light theme](docs/screenshots/prayer-times-ar-light.png) | ![Prayer times — Arabic, dark theme](docs/screenshots/prayer-times-ar-dark.png) |

### Home-screen widgets

Four sizes: small tall, small wide, medium (5×1 schedule), and large (clock + six columns). Next prayer highlighted with a single column border; widgets follow the app theme.

| English (light) | Arabic (light) | Arabic (dark) |
|:---:|:---:|:---:|
| ![Widgets — English, light](docs/screenshots/widgets-en-light.png) | ![Widgets — Arabic, light](docs/screenshots/widgets-ar-light.png) | ![Widgets — Arabic, dark](docs/screenshots/widgets-ar-dark.png) |

| Large widget (EN) | Large widget (AR) | Large widget (AR, dark) |
|:---:|:---:|:---:|
| ![Large widget — English](docs/screenshots/widget-large-en-light.png) | ![Large widget — Arabic](docs/screenshots/widget-large-ar-light.png) | ![Large widget — Arabic, dark](docs/screenshots/widget-large-ar-dark.png) |

### Hijri calendar

Monthly grid with Gregorian pairing, event labels (Arafah, Eid, etc.), and an annual occasions list.

| Monthly calendar | Annual occasions |
|:---:|:---:|
| ![Hijri calendar — monthly view](docs/screenshots/hijri-calendar-monthly-ar.png) | ![Hijri calendar — annual events](docs/screenshots/hijri-events-annual-ar.png) |

### Settings & setup

Offline-only privacy toggle, theme picker, adhan notifications, and country/city wizard (4,000+ cities, no GPS).

| Settings | City wizard |
|:---:|:---:|
| ![Settings — Arabic, dark theme](docs/screenshots/settings-ar-dark.png) | ![City wizard — Arabic, dark theme](docs/screenshots/city-wizard-ar-dark.png) |

---

## Features

| Area | Details |
|------|---------|
| **Prayer calculation** | Umm al-Qura (Makkah), Shafi Asr, twilight angle at \|lat\| ≥ 48°; Shuruq = sunrise |
| **Privacy** | Default **offline-only** — no network calls; optional Aladhan API when user disables offline mode (online flavor) |
| **Adhan** | Eight sounds, per-prayer mute, exact-alarm scheduling, Doze-safe `setAlarmClock` |
| **Widgets** | Four providers; locale + Eastern Arabic digits; theme sync; stale-cache fallback |
| **Hijri** | Calculator + 10 events; main-screen banner; calendar monthly/annual views |
| **i18n** | English / Arabic, RTL layout, in-app language picker |
| **Themes** | Light, green, dark — app + widgets + calendar |
| **Security (online)** | TLS certificate pinning for `aladhan.com` |

---

## Architecture

- **Primary app (`online` flavor):** bundled `locations.json` + local `adhan-java` **or** [Aladhan API](https://api.aladhan.com) — user toggles **Offline-only (no network)** in Settings
- **Optional `offline` flavor:** separate APK with **no** network stack and **no** `INTERNET` permission
- **Stack:** Kotlin · Jetpack Compose · Hilt · Room v4 · DataStore · WorkManager
- **Flavors:** `RepositoryModule` swaps `LocalPrayerTimesRepository` vs `OnlinePrayerTimesRepository`; network code lives only in `src/online/`

See [`PHASED_PLAN.md`](PHASED_PLAN.md) for the full roadmap and Mermaid diagrams.

---

## Install (release)

Requires JDK 21, Android SDK, and a local `keystore.properties` (copy from `keystore.properties.example`; never commit the keystore or passwords).

```sh
export JAVA_HOME=$HOME/jdk21
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew assembleOnlineRelease
adb install -r app/build/outputs/apk/online/release/app-online-release.apk
```

| Artifact | Path | Size |
|----------|------|------|
| Signed APK | `app/build/outputs/apk/online/release/app-online-release.apk` | ~12 MB |
| Signed AAB | `app/build/outputs/bundle/onlineRelease/app-online-release.aab` | Play Store |

**Privacy-only build:** `assembleOfflineRelease` → `com.prayertime.offline` (~12 MB, no Aladhan toggle).

```sh
./scripts/release-gate.sh   # APK size gate (≤ 13 MB)
./scripts/smoke-ci.sh       # full CI before merge/tag
```

---

## Development

```sh
export JAVA_HOME=$HOME/jdk21
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew assembleOnlineDebug testOnlineDebugUnitTest
```

**Emulator shortcut** (boot → install offline debug → launch):

```sh
./dev              # installs com.prayertime.offline
./dev --headless
```

For the **online** flavor on device/emulator: `./gradlew installOnlineDebug`.

After widget size or layout changes, remove and re-add the widget on the home screen (launchers cache dimensions).

---

## Tests

```sh
./gradlew testOfflineDebugUnitTest testOnlineDebugUnitTest
```

303 shared tests + 41 online-only (`app/src/testOnline/java/`). Suites cover Doze (5A), permission denial (5B), offline cache (5C), DST/timezone (5D), Room migrations, widgets, and Hijri calculator.

Requires JDK 21 (`$HOME/jdk21`); system JDK 25 breaks the current Gradle/AGP toolchain.

---

## Documentation

| Doc | Purpose |
|-----|---------|
| [`PHASED_PLAN.md`](PHASED_PLAN.md) | Roadmap, phase gates, Graphify |
| [`APP_CREATION_PLAYBOOK.md`](APP_CREATION_PLAYBOOK.md) | Engineering playbook + feature table |
| [`docs/PRIVACY.md`](docs/PRIVACY.md) | Privacy model |
| [`graphity.md`](graphity.md) | Knowledge-graph maintenance |
| [`AGENTS.md`](AGENTS.md) | Build environment for agents/CI |

---

## License

See repository license file. Prayer calculation uses [`adhan-java`](https://github.com/batoulapps/adhan-java) (Umm al-Qura).
