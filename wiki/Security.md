# Security

**Repository security status (Jun 2026):** all GitHub Advanced Security features below are **enabled** on [karimVentus/Private-Prayer](https://github.com/karimVentus/Private-Prayer).

| Feature | Status | Link |
|---------|--------|------|
| Security policy | Enabled | [SECURITY.md](https://github.com/karimVentus/Private-Prayer/blob/main/SECURITY.md) |
| Security advisories | Enabled | [Security advisories](https://github.com/karimVentus/Private-Prayer/security/advisories) |
| Private vulnerability reporting | Enabled | [Report a vulnerability](https://github.com/karimVentus/Private-Prayer/security/advisories/new) |
| Dependabot alerts | Enabled | [Dependabot alerts](https://github.com/karimVentus/Private-Prayer/security/dependabot) |
| Code scanning (CodeQL) | Enabled — initial full scan may take a while | [Code scanning alerts](https://github.com/karimVentus/Private-Prayer/security/code-scanning) |
| Secret scanning | Enabled (public repo) | [Secret scanning alerts](https://github.com/karimVentus/Private-Prayer/security/secret-scanning) |

---

## Reporting vulnerabilities

**Do not** open a public issue for security bugs.

1. **Private Vulnerability Reporting** — [Security → Report a vulnerability](https://github.com/karimVentus/Private-Prayer/security/advisories/new) (preferred).
2. **Email** — `bashirta95@gmail.com`

Policy: [SECURITY.md](https://github.com/karimVentus/Private-Prayer/blob/main/SECURITY.md) · Supported versions: **1.0.x** and later.

---

## GitHub security configuration

### Dependabot

| Setting | Status |
|---------|--------|
| Dependabot alerts | On |
| Dependabot security updates | On — auto PRs for CVE patches |
| Dependabot version updates | On — weekly Gradle PRs via [`.github/dependabot.yml`](https://github.com/karimVentus/Private-Prayer/blob/main/.github/dependabot.yml) |
| Grouped security updates | On |
| Dependabot malware alerts | On |
| Dependabot rules | 1 rule enabled |
| Automatic dependency submission | On |

Pinned dependencies in `dependabot.yml` (Kotlin, AGP, Room, Hilt, etc.) are intentionally held until a coordinated upgrade — see ignore comments in the file.

### Code scanning (CodeQL)

**Default CodeQL setup** enabled from **Settings → Security → Code scanning**. GitHub runs read-only analysis on the repository; the first full scan can take time after initial setup.

- View findings: [Code scanning alerts](https://github.com/karimVentus/Private-Prayer/security/code-scanning)
- **Copilot Autofix:** On — suggests fixes for CodeQL alerts (requires CodeQL enabled)

### Secret protection

Public repositories receive secret scanning alerts automatically. **Push protection** can block commits containing supported secrets — enable in **Settings → Security → Secret Protection** if not already on.

---

## App security notes

- **Offline-only default** — no network unless user disables in Settings
- **TLS pinning** — `aladhan.com` in `network_security_config.xml`; rotate via `scripts/verify-aladhan-pins.sh`
- **No GPS** — city chosen from catalog or manual coords only
- **Release** — R8 shrinker; signed APK/AAB on GitHub Releases

---

## العربية — حالة الأمان

كل ميزات تبويب **Security** مفعّلة على المستودع:

| الميزة | الحالة |
|--------|--------|
| سياسة الأمان | مفعّلة — `SECURITY.md` |
| الإبلاغ الخاص عن الثغرات | مفعّل — [بلّغ هنا](https://github.com/karimVentus/Private-Prayer/security/advisories/new) |
| تنبيهات Dependabot | مفعّلة |
| فحص الكود CodeQL | مفعّل — أول فحص كامل قد يستغرق وقتاً |
| فحص الأسرار | مفعّل (مستودع عام) |
| Copilot Autofix | مفعّل |

**لا تفتح Issue عام** للثغرات — استخدم الإبلاغ الخاص أو البريد `bashirta95@gmail.com`.

التطبيق: أوفلاين افتراضياً، تثبيت TLS لـ Aladhan عند تفعيل الشبكة، بدون GPS.
