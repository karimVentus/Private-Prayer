# Contributing Guidelines / إرشادات المساهمة

[English](#english) | [العربية](#arabic)

---

<a id="english"></a>

## English

Thank you for your interest in contributing to **Private-Prayer**! We welcome bug fixes, improvements, and documentation updates.

### Build & Run

Ensure you have the correct JDK and Android SDK paths set:

```sh
export JAVA_HOME=$HOME/jdk21
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew assembleOnlineDebug
```

### Running Tests

Before submitting any changes, run the test suites to ensure everything is functional:

```sh
./gradlew testOfflineDebugUnitTest testOnlineDebugUnitTest
```

### Coding Standards

We use Detekt for code linting and style checking. Verify your code meets the standards:

```sh
./gradlew detekt
```

---

<a id="arabic"></a>

## العربية

شكرًا لاهتمامك بالمساهمة في مشروع **Private-Prayer**! نرحب بتقارير إصلاح الأخطاء، والتحسينات، وتحديثات المستندات.

### البناء والتشغيل

تأكد من إعداد مسارات JDK و Android SDK بشكل صحيح على جهازك:

```sh
export JAVA_HOME=$HOME/jdk21
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew assembleOnlineDebug
```

### تشغيل الاختبارات

قبل إرسال أي تغييرات، يرجى تشغيل حزم الاختبارات للتأكد من سلامة التطبيق:

```sh
./gradlew testOfflineDebugUnitTest testOnlineDebugUnitTest
```

### معايير جودة الكود

نحن نستخدم Detekt لفحص وتدقيق جودة الكود وتنسيقه. يرجى التحقق من مطابقة الكود الخاص بك للمعايير:

```sh
./gradlew detekt
```
