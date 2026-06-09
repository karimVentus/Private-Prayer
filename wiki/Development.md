# Development

## Requirements

| Tool | Version / path |
|------|----------------|
| JDK | **21** (Temurin) at `~/jdk21` — system JDK 25 breaks Gradle/AGP |
| Android SDK | `~/Android/Sdk` — platforms 34 + 35, build-tools 34 |
| Gradle | 8.12 wrapper |
| AGP | 8.7.3 |
| Kotlin | 2.0.21 |

## Build

```sh
export JAVA_HOME=$HOME/jdk21
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew assembleDebug testDebugUnitTest
```

## Emulator shortcut

```sh
./dev              # boot emulator, installDebug, launch
./dev --headless   # no GUI
```

Package: `com.prayertime`

## CI gate

```sh
./scripts/smoke-ci.sh
```

Runs debug build, **414** JVM unit tests, APK size check (~25 MB limit). Live HTTP tests skip unless `PRAYERTIME_LIVE_HTTP=1`.

## Branching

- Feature branches only — no direct push to `main`
- `./scripts/smoke-ci.sh` green before merge
- Update `PHASED_PLAN.md`, `AGENTS.md`, and playbook on phase boundaries

## Key paths

| Area | Path |
|------|------|
| UI | `app/src/main/java/com/prayertime/ui/` |
| Prayer engine | `domain/calculator/` |
| City catalog | `data/assets/locations.json` |
| Widgets | `widget/` |
| Tests | `app/src/test/java/` |

## Instrumented tests (optional)

```sh
./gradlew connectedDebugAndroidTest
```

Requires emulator/device; Room migration tests.

## Docs for agents

See [AGENTS.md](https://github.com/karimVentus/Private-Prayer/blob/main/AGENTS.md) in the repo.
