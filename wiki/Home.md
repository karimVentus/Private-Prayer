# Hayya (حيا) — Private-Prayer Wiki

**Privacy-first prayer times • Offline-first • No GPS • No ads**

Open-source Android app ([karimVentus/Private-Prayer](https://github.com/karimVentus/Private-Prayer)) for accurate daily prayer times — 4,000+ bundled cities, live countdown, optional adhan (including custom sounds), Qibla compass, home-screen widgets, and a Hijri calendar.

| | |
|---|---|
| **Latest release** | [v1.1.6](https://github.com/karimVentus/Private-Prayer/releases/tag/v1.1.6) |
| **Package** | `com.prayertime` |
| **Min / Target SDK** | 23 / 35 |
| **Languages** | English, Arabic (RTL) |

---

## Wiki pages

| Page | Contents |
|------|----------|
| [Roadmap](Roadmap) | Phase status (0–7A done; Phase 8 planned) |
| [Phase 8 — City catalog](Phase-8-City-Catalog) | Coordinate rollout + manual lat/lng wizard |
| [Development](Development) | Build env, tests, CI, branching |
| [Security](Security) | Enabled Dependabot, CodeQL, secret scanning; how to report |
| [Releases](Releases) | Version history |

---

## Quick links

- [README](https://github.com/karimVentus/Private-Prayer#readme) — screenshots, features, install
- [PHASED_PLAN.md](https://github.com/karimVentus/Private-Prayer/blob/main/PHASED_PLAN.md) — full implementation plan (source of truth)
- [AGENTS.md](https://github.com/karimVentus/Private-Prayer/blob/main/AGENTS.md) — agent / contributor build notes
- [GitHub Releases](https://github.com/karimVentus/Private-Prayer/releases) — signed APKs (`Hayya-v*.apk`)
- [Smoke CI](https://github.com/karimVentus/Private-Prayer/actions/workflows/smoke-ci.yml) — build + unit tests on every push

---

## Current focus

**Phase 8** — fill `knownCityCoords` worldwide (Europe → Africa → Asia → America) and add a **manual latitude/longitude** path in the city wizard for regions without bundled coords. See [Phase 8 — City catalog](Phase-8-City-Catalog).
