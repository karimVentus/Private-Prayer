# Prayer Times (أوقات الصلاة)

[English](#english) | [العربية](#arabic)

[![Smoke CI](https://github.com/karimVentus/Private-Prayer/actions/workflows/smoke-ci.yml/badge.svg)](https://github.com/karimVentus/Private-Prayer/actions/workflows/smoke-ci.yml)

---

<a id="english"></a>

# English

**Privacy-first Android prayer times — no GPS, no account.**

Pick a country and city from a bundled catalog, get accurate daily times (Umm al-Qura, Shafi), live countdown to the next prayer, optional adhan notifications, four home-screen widgets, and a Hijri calendar with ten Islamic events. English and Arabic with full RTL support; three themes (light, green, dark).

| | |
|---|---|
| **Version** | 1.0.0 |
| **Package** | `com.prayertime` |
| **Min SDK** | 23 · **Target** 35 |
| **Tests** | JVM unit tests via `./gradlew testDebugUnitTest` |

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
| **Privacy** | Default **offline-only** — no network calls; optional Aladhan API when user disables offline mode in Settings |
| **Adhan** | Eight sounds, per-prayer mute, exact-alarm scheduling, Doze-safe `setAlarmClock` |
| **Widgets** | Four providers; locale + Eastern Arabic digits; theme sync; stale-cache fallback |
| **Hijri** | Calculator + 10 events; main-screen banner; calendar monthly/annual views |
| **i18n** | English / Arabic, RTL layout, in-app language picker |
| **Themes** | Light, green, dark — app + widgets + calendar |
| **Security** | TLS certificate pinning for `aladhan.com` (when network mode enabled) |

---

## Architecture

- **Single APK (`com.prayertime`):** bundled `locations.json` + local `adhan-java` **or** [Aladhan API](https://api.aladhan.com) — user toggles **Offline-only (no network)** in Settings
- **Stack:** Kotlin · Jetpack Compose · Hilt · Room v4 · DataStore · WorkManager · Retrofit/OkHttp (Aladhan only when network mode on)
- **Repository:** `OnlinePrayerTimesRepository` composes local engine + optional API

See [`PHASED_PLAN.md`](PHASED_PLAN.md) for the full roadmap and Mermaid diagrams.

---

## Install (release)

Requires JDK 21, Android SDK, and a local `keystore.properties` (copy from `keystore.properties.example`; never commit the keystore or passwords).

```sh
export JAVA_HOME=$HOME/jdk21
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

| Artifact | Path | Size |
|----------|------|------|
| Signed APK | `app/build/outputs/apk/release/app-release.apk` | ~12 MB |
| Signed AAB | `app/build/outputs/bundle/release/app-release.aab` | Play Store |

```sh
./scripts/release-gate.sh   # APK size gate (≤ 13 MB)
./scripts/smoke-ci.sh       # full CI before merge/tag
```

---

## Development

```sh
export JAVA_HOME=$HOME/jdk21
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew assembleDebug testDebugUnitTest
```

**Emulator shortcut** (boot → install debug → launch):

```sh
./dev              # installs com.prayertime
./dev --headless
```

Manual install on a running emulator:

```sh
./scripts/emulator-start
./gradlew installDebug
adb shell am start -n com.prayertime/com.prayertime.ui.MainActivity
```

After widget size or layout changes, remove and re-add the widget on the home screen (launchers cache dimensions).

---

## Tests

```sh
./gradlew testDebugUnitTest
```

Full gate: `./scripts/smoke-ci.sh` (build, lint, detekt, APK size).

Requires JDK 21 (`$HOME/jdk21`); system JDK 25 breaks the current Gradle/AGP toolchain.

---

## Documentation

| Doc | Purpose |
|-----|---------|
| [`PHASED_PLAN.md`](PHASED_PLAN.md) | Roadmap, phase gates, Graphify |
| [`APP_CREATION_PLAYBOOK.md`](APP_CREATION_PLAYBOOK.md) | Engineering playbook + feature table |
| [`docs/PRIVACY.md`](docs/PRIVACY.md) | Privacy model |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | Contribution guidelines for developers |
| [`SECURITY.md`](SECURITY.md) | Security policy and vulnerability reporting |
| [`graphity.md`](graphity.md) | Knowledge-graph maintenance |
| [`AGENTS.md`](AGENTS.md) | Build environment for agents/CI |

---

## License

See repository license file. Prayer calculation uses [`adhan-java`](https://github.com/batoulapps/adhan-java) (Umm al-Qura).

---

<a id="arabic"></a>

# العربية

**تطبيق أوقات الصلاة يركز على الخصوصية — بدون نظام تحديد المواقع (GPS)، وبدون حساب شخصي.**

اختر الدولة والمدينة من دليل مدمج داخل التطبيق، واحصل على أوقات صلاة يومية دقيقة (تقويم أم القرى، والمذهب الشافعي)، وعد تنازلي مباشر للصلاة القادمة، وتنبيهات أذان اختيارية، وأربع أدوات للشاشة الرئيسية (Widgets)، وتقويم هجري مع عشرة مناسبات إسلامية. التطبيق متوفر باللغتين العربية والإنجليزية مع دعم كامل للاتجاه من اليمين إلى اليسار (RTL)، وثلاثة سمات (مظهر فاتح، أخضر، داكن).

| | |
|---|---|
| **الإصدار** | 1.0.0 |
| **حزمة التطبيق** | `com.prayertime` |
| **الحد الأدنى لـ SDK** | 23 · **المستهدف** 35 |
| **الاختبارات** | `./gradlew testDebugUnitTest` |

---

## لقطات الشاشة (Screenshots)

### أوقات الصلاة

عد تنازلي مباشر، التاريخ الهجري، شريط المناسبات الإسلامية القادمة، مفاتيح كتم الصوت لكل صلاة، ووضع الخصوصية دون اتصال بالشبكة.

| English (light) | Arabic (light) | Arabic (dark) |
|:---:|:---:|:---:|
| ![Prayer times — English, light theme](docs/screenshots/prayer-times-en-light.png) | ![Prayer times — Arabic, light theme](docs/screenshots/prayer-times-ar-light.png) | ![Prayer times — Arabic, dark theme](docs/screenshots/prayer-times-ar-dark.png) |

### أدوات الشاشة الرئيسية (Widgets)

أربعة أحجام: أداة طولية صغيرة، أداة عريضة صغيرة، أداة متوسطة (جدول 5×1)، وأداة كبيرة (ساعة + ستة أعمدة). يتم تمييز الصلاة القادمة بإطار عمودي فريد؛ تتبع الأدوات مظهر التطبيق المختار.

| English (light) | Arabic (light) | Arabic (dark) |
|:---:|:---:|:---:|
| ![Widgets — English, light](docs/screenshots/widgets-en-light.png) | ![Widgets — Arabic, light](docs/screenshots/widgets-ar-light.png) | ![Widgets — Arabic, dark](docs/screenshots/widgets-ar-dark.png) |

| Large widget (EN) | Large widget (AR) | Large widget (AR, dark) |
|:---:|:---:|:---:|
| ![Large widget — English](docs/screenshots/widget-large-en-light.png) | ![Large widget — Arabic](docs/screenshots/widget-large-ar-light.png) | ![Large widget — Arabic, dark](docs/screenshots/widget-large-ar-dark.png) |

### التقويم الهجري

شبكة تقويم شهرية مقترنة بالتقويم الميلادي، وتسميات المناسبات الإسلامية (عرفة، الأعياد، إلخ)، وقائمة بالمناسبات السنوية.

| Monthly calendar | Annual occasions |
|:---:|:---:|
| ![Hijri calendar — monthly view](docs/screenshots/hijri-calendar-monthly-ar.png) | ![Hijri calendar — annual events](docs/screenshots/hijri-events-annual-ar.png) |

### الإعدادات وإعدادات التشغيل الأول

مفتاح الخصوصية للعمل دون اتصال بالشبكة بالكامل، مغير السمة، تنبيهات الأذان، ومعالج إعداد الدولة والمدينة (أكثر من 4000 مدينة، دون الحاجة لـ GPS).

| Settings | City wizard |
|:---:|:---:|
| ![Settings — Arabic, dark theme](docs/screenshots/settings-ar-dark.png) | ![City wizard — Arabic, dark theme](docs/screenshots/city-wizard-ar-dark.png) |

---

## الميزات (Features)

| القسم | التفاصيل |
|------|---------|
| **حساب أوقات الصلاة** | تقويم أم القرى (مكة المكرمة)، مذهب الشافعي للعصر، تعديل زاوية الشفق لخطوط العرض |lat| ≥ 48°؛ وقت الشروق = شروق الشمس |
| **الخصوصية** | افتراضياً **دون اتصال بالشبكة** — لا اتصالات شبكة؛ Aladhan اختياري من الإعدادات |
| **الأذان** | ثمانية أصوات للأذان، إمكانية كتم الصوت لكل صلاة على حدة، جدولة التنبيهات باستخدام ميزة الإنذار الدقيق، متوافق مع وضع Doze للحفاظ على البطارية عبر `setAlarmClock` |
| **الأدوات (Widgets)** | أربعة مزودين للأدوات؛ دعم اللغة والأرقام العربية الشرقية؛ مزامنة السمة؛ استخدام الذاكرة المؤقتة كاحتياطي عند عدم وجود تحديثات |
| **التقويم الهجري** | حاسبة هجرية مدمجة مع 10 مناسبات إسلامية؛ شريط إعلاني للمناسبات في الشاشة الرئيسية؛ عرض شهري وسنوي للتقويم |
| **تعدد اللغات** | دعم كامل للغتين العربية والإنجليزية، اتجاه كامل من اليمين إلى اليسار (RTL)، ومحدد لغة مدمج في التطبيق |
| **السمات** | ثلاثة سمات (فاتح، أخضر، داكن) تشمل التطبيق والأدوات والتقويم |
| **الأمان** | تثبيت شهادة TLS لـ `aladhan.com` عند تفعيل وضع الشبكة |

---

## البنية البرمجية (Architecture)

- **APK واحد (`com.prayertime`):** `locations.json` + `adhan-java` **أو** [Aladhan API](https://api.aladhan.com) — toggle **دون اتصال** من الإعدادات
- **التقنيات:** Kotlin · Jetpack Compose · Hilt · Room v4 · DataStore · WorkManager

راجع ملف [`PHASED_PLAN.md`](PHASED_PLAN.md) للاطلاع على خارطة الطريق الكاملة ومخططات Mermaid.

---

## التثبيت (النسخة النهائية - Release)

يتطلب التثبيت وجود JDK 21، و Android SDK، وملف `keystore.properties` محلي (انسخه من `keystore.properties.example`؛ لا تقم أبداً برفع ملف keystore أو كلمات المرور إلى المستودع).

```sh
export JAVA_HOME=$HOME/jdk21
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

| الملف الناتج | المسار | الحجم |
|----------|------|------|
| APK موقع | `app/build/outputs/apk/release/app-release.apk` | ~12 ميجابايت |
| AAB موقع | `app/build/outputs/bundle/release/app-release.aab` | متجر Google Play |

```sh
./scripts/release-gate.sh   # التحقق من حجم الـ APK (يجب أن يكون ≤ 13 ميجابايت)
./scripts/smoke-ci.sh       # اختبار الـ CI الكامل قبل الدمج أو التوسيم
```

---

## التطوير (Development)

```sh
export JAVA_HOME=$HOME/jdk21
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew assembleDebug testDebugUnitTest
```

**اختصار المحاكي:**

```sh
./dev              # com.prayertime
./dev --headless
```

تثبيت يدوي:

```sh
./scripts/emulator-start
./gradlew installDebug
adb shell am start -n com.prayertime/com.prayertime.ui.MainActivity
```

بعد تغيير حجم أو تخطيط الأدوات (Widgets)، قم بإزالتها وإعادة إضافتها إلى الشاشة الرئيسية (تقوم واجهات التشغيل بحفظ الأبعاد مؤقتاً).

---

## الاختبارات (Tests)

```sh
./gradlew testDebugUnitTest
```

البوابة الكاملة: `./scripts/smoke-ci.sh`.

يتطلب التثبيت إصدار JDK 21 (`$HOME/jdk21`)؛ حيث أن إصدار نظام JDK 25 يسبب مشاكلاً مع بيئة Gradle/AGP الحالية.

---

## المستندات (Documentation)

| المستند | الغرض |
|-----|---------|
| [`PHASED_PLAN.md`](PHASED_PLAN.md) | خارطة الطريق، بوابات المراحل، Graphify |
| [`APP_CREATION_PLAYBOOK.md`](APP_CREATION_PLAYBOOK.md) | دليل الهندسة البرمجية + جدول الميزات |
| [`docs/PRIVACY.md`](docs/PRIVACY.md) | نموذج سياسة الخصوصية |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | إرشادات المساهمة للمطورين |
| [`SECURITY.md`](SECURITY.md) | سياسة الأمان والإبلاغ عن الثغرات |
| [`graphity.md`](graphity.md) | صيانة شجرة المعرفة (Knowledge-Graph) |
| [`AGENTS.md`](AGENTS.md) | بيئة البناء والتشغيل للوكلاء والـ CI |

---

## الترخيص (License)

راجع ملف الترخيص المرفق في المستودع. يعتمد حساب أوقات الصلاة على مكتبة [`adhan-java`](https://github.com/batoulapps/adhan-java) (تقويم أم القرى).
