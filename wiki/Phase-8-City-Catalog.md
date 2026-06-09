# Phase 8 — City catalog coordinates + manual entry

**Status:** **8A** + **8B complete** on `feat/city-coords-europe` (574/574 EU picker cities). **8C** (Africa) next.

---

## Problem

`locations.json` has **2,056** picker cities but only **~1,049** `knownCityCoords` (~51%). In **offline-only mode** (default), cities without exact coords are rejected (`CITY_NOT_FOUND`).

**Europe baseline (Jun 2026):** 599 picker cities / 134 with exact coords. Germany (`DE`) is complete; most other EU countries have 0 exact coords.

---

## Regional data order

Fill coordinates in this order:

1. **Europe**
2. **Africa**
3. **Asia**
4. **America**

Scripts:

- `scripts/expand_locations.py` — add coords + picker lists
- `scripts/generate-cities-ar.py` — regenerate Arabic city names after picker changes

---

## Wizard flow (target UX)

1. Select country → search city.
2. **Path A** — city in picker with bundled coords → save (existing).
3. **Path B** — no match or no coords:
   - **B1** — *Use "&lt;name&gt;"* — custom name; online geocode when offline coords absent (existing).
   - **B2** — **Enter coordinates manually** (new) — city name + latitude + longitude; hint to copy from [latlong.net](https://www.latlong.net/) or Google Maps long-press; timezone from `countryDefaults`; saves `CityConfig` with lat/lng → works **offline** (prayer times + Qibla).

```mermaid
flowchart TD
    Country[Select country] --> Search[Search city]
    Search --> Found{In picker with coords?}
    Found -->|yes| SaveA[Save bundled coords]
    Found -->|no| Options[Show fallback options]
    Options --> B1[Use custom name]
    Options --> B2[Enter lat/lng manually]
    B1 --> Online{Offline-only?}
    Online -->|yes + no coords| Err[CITY_NOT_FOUND]
    Online -->|no| Geocode[Aladhan geocode]
    B2 --> SaveB[Save CityConfig + country TZ]
    Geocode --> SaveA
    SaveA --> Done[Prayer times]
    SaveB --> Done
```

---

## Tasks

### 8A — Manual coordinates UI (complete)

- [x] **8A.1** `WizardStep.ManualCoords` + lat/lng/timezone fields, validation, EN/AR copy
- [x] **8A.2** `saveManualCoords()` — `CityConfig.hasValidCoordinates` bypasses fallback rejection
- [x] **8A.3** `ManualCoordsOption` card on city fallback (“Enter coordinates manually”)
- [x] **8A.4** Unit tests — offline save, invalid coords rejected, country default TZ (7 tests)

### 8B — Europe coord fill (complete)

- [x] **8B.1** `scripts/fill_europe_coords.py` — OSM Nominatim + `.europe-coords-cache.json`
- [x] **8B.2** 440 coords added; EU picker 574/574 covered; MD `countryDefaults` → `Europe/Chisinau`
- [x] **8B.3** `LocationDataSourceTest` — `every_europe_picker_city_resolves_to_found`

### 8C–8E — Regional coord fill

- [ ] **8C** Africa
- [ ] **8D** Asia
- [ ] **8E** America

### 8F — Release

- [ ] Update README catalog note; version bump; APK size gate

---

## Branching

```sh
git checkout main && git pull
git checkout -b feat/city-coords-europe   # or feat/manual-coords-wizard for 8A
./scripts/smoke-ci.sh                     # before merge
```

Implement **8A before 8B** so users have a fallback while the catalog fills.
