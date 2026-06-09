# Security

## Reporting vulnerabilities

**Do not** open a public issue for security bugs.

1. **GitHub Private Vulnerability Reporting** — Security tab → *Report a vulnerability* (enable in repo settings; see below).
2. **Email** — `bashirta95@gmail.com`

Policy: [SECURITY.md](https://github.com/karimVentus/Private-Prayer/blob/main/SECURITY.md)

Supported versions: **1.0.x** and later.

---

## Why Security tab features show “Disabled” / “Needs setup”

These are **off by default** on new repos until you turn them on in Settings. They are not broken — they were never enabled.

| Feature | What you see | Why | How to enable |
|---------|--------------|-----|---------------|
| **Private vulnerability reporting** | Disabled | Repo setting not turned on | **Settings → Security → Private vulnerability reporting → Enable** |
| **Dependabot alerts** | Disabled | Alerts not enabled (`.github/dependabot.yml` exists but alerts are separate) | **Settings → Security → Code security and analysis → Dependabot alerts → Enable** |
| **Code scanning** | Needs setup | No CodeQL (or other) workflow has run yet | Add [CodeQL workflow](https://github.com/github/codeql-action) or **Settings → Security → Code scanning → Set up** |

### Dependabot

The repo already has [`.github/dependabot.yml`](https://github.com/karimVentus/Private-Prayer/blob/main/.github/dependabot.yml) for **weekly Gradle dependency PRs**. That is separate from **Dependabot alerts**, which notify you when a *known CVE* affects a dependency. Enable alerts in Settings (free for public repos).

### Code scanning (CodeQL)

Free for **public** repositories. Minimal setup:

1. **Actions → New workflow → CodeQL Analysis** (or commit `.github/workflows/codeql.yml`).
2. First successful run moves status from “Needs setup” to active.

### Private vulnerability reporting

Free. Once enabled, reporters use the Security tab form instead of public issues. Matches [SECURITY.md](https://github.com/karimVentus/Private-Prayer/blob/main/SECURITY.md).

---

## App security notes

- **Offline-only default** — no network unless user disables in Settings
- **TLS pinning** — `aladhan.com` in `network_security_config.xml`; rotate via `scripts/verify-aladhan-pins.sh`
- **No GPS** — city chosen from catalog or manual coords only
- **Release** — R8 shrinker; signed APK/AAB on GitHub Releases

---

## العربية — لماذا الميزات غير مفعّلة؟

ميزات تبويب **Security** في GitHub **لا تُفعَّل تلقائياً** عند إنشاء المستودع. الحالة "Disabled" تعني أنك لم تفعّلها من الإعدادات بعد:

- **Private vulnerability reporting** — من إعدادات المستودع → Security → تفعيل الإبلاغ الخاص
- **Dependabot alerts** — تفعيل التنبيهات من Code security and analysis (ملف dependabot.yml موجود لطلبات التحديث فقط)
- **Code scanning** — يحتاج إعداد workflow مثل CodeQL وتشغيله مرة واحدة

التطبيق نفسه يدعم الوضع الأوفلاين افتراضياً وتثبيت TLS لـ Aladhan عند تفعيل الشبكة.
