#!/usr/bin/env python3
"""Fill knownCityCoords for European picker cities missing from locations.json.

One-time maintainer script: queries OSM Nominatim, caches results in
scripts/.europe-coords-cache.json, then patches app/src/main/assets/locations.json.
Re-run is idempotent (cache hits skip network).
"""
from __future__ import annotations

import json
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSET_PATH = ROOT / "app/src/main/assets/locations.json"
TEST_RESOURCE_PATH = ROOT / "app/src/test/resources/locations.json"
CACHE_PATH = Path(__file__).resolve().parent / ".europe-coords-cache.json"

EU_CODES = {
    "AL", "AD", "AT", "BE", "BA", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR", "DE",
    "GR", "HU", "IS", "IE", "IT", "XK", "LV", "LT", "LU", "MT", "MD", "MC", "ME", "MK",
    "NL", "NO", "PL", "PT", "RO", "RS", "SK", "SI", "ES", "SE", "CH", "UA", "GB", "VA",
}

COUNTRY_NAMES = {
    "AL": "Albania", "AD": "Andorra", "AT": "Austria", "BE": "Belgium", "BA": "Bosnia and Herzegovina",
    "BG": "Bulgaria", "HR": "Croatia", "CY": "Cyprus", "CZ": "Czech Republic", "DK": "Denmark",
    "EE": "Estonia", "FI": "Finland", "FR": "France", "DE": "Germany", "GR": "Greece",
    "HU": "Hungary", "IS": "Iceland", "IE": "Ireland", "IT": "Italy", "XK": "Kosovo",
    "LV": "Latvia", "LT": "Lithuania", "LU": "Luxembourg", "MT": "Malta", "MD": "Moldova",
    "MC": "Monaco", "ME": "Montenegro", "MK": "North Macedonia", "NL": "Netherlands",
    "NO": "Norway", "PL": "Poland", "PT": "Portugal", "RO": "Romania", "RS": "Serbia",
    "SK": "Slovakia", "SI": "Slovenia", "ES": "Spain", "SE": "Sweden", "CH": "Switzerland",
    "UA": "Ukraine", "GB": "United Kingdom", "VA": "Vatican City",
}

# Picker name -> Nominatim search query override (city fragment only).
SEARCH_OVERRIDES: dict[tuple[str, str], str] = {
    ("GB", "Newcastle"): "Newcastle upon Tyne",
    ("ES", "Cordoba"): "Córdoba",
    ("ES", "Malaga"): "Málaga",
    ("NL", "The Hague"): "Den Haag",
    ("PL", "Lodz"): "Łódź",
    ("RO", "Bucharest"): "București",
    ("RO", "Brașov"): "Brașov",
    ("RO", "Brasov"): "Brașov",
    ("RO", "Ploiesti"): "Ploiești",
    ("RO", "Targu Mures"): "Târgu Mureș",
    ("UA", "Kyiv"): "Kyiv",
    ("UA", "Odesa"): "Odesa",
    ("MD", "Chișinău"): "Chișinău",
    ("MD", "Bălți"): "Bălți",
    ("MD", "Hîncești"): "Hîncești",
    ("MD", "Edineț"): "Edineț",
    ("ES", "Las Palmas"): "Las Palmas de Gran Canaria",
    ("LU", "Luxembourg City"): "Luxembourg",
    ("MC", "Monte Carlo"): "Monte Carlo",
    ("MC", "La Condamine"): "La Condamine",
    ("MC", "Fontvieille"): "Fontvieille",
    ("MC", "Moneghetti"): "Moneghetti",
    ("VA", "Vatican City"): "Vatican City",
    ("XK", "Pristina"): "Pristina",
    ("XK", "Peja"): "Peja",
    ("XK", "Ferizaj"): "Ferizaj",
    ("XK", "Gjilan"): "Gjilan",
    ("XK", "Vushtrri"): "Vushtrri",
    ("XK", "Suhareka"): "Suhareka",
    ("XK", "Malisheva"): "Malisheva",
    ("IS", "Reykjavik"): "Reykjavík",
    ("IS", "Kópavogur"): "Kópavogur",
    ("IS", "Reykjanesbær"): "Reykjanesbær",
    ("IS", "Garðabær"): "Garðabær",
    ("IS", "Mosfellsbær"): "Mosfellsbær",
    ("IS", "Hafnarfjörður"): "Hafnarfjörður",
    ("IS", "Keflavík"): "Keflavík",
}

# Fix incorrect/missing countryDefaults while patching.
COUNTRY_DEFAULT_FIXES: dict[str, dict] = {
    "MD": {"latitude": 47.0, "longitude": 28.8, "timezone": "Europe/Chisinau"},
}


def load_cache() -> dict[str, dict]:
    if CACHE_PATH.exists():
        return json.loads(CACHE_PATH.read_text(encoding="utf-8"))
    return {}


def save_cache(cache: dict[str, dict]) -> None:
    CACHE_PATH.write_text(json.dumps(cache, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def nominatim_search(city: str, country: str) -> tuple[float, float] | None:
    query = f"{city}, {country}"
    params = urllib.parse.urlencode({
        "q": query,
        "format": "json",
        "limit": 1,
    })
    url = f"https://nominatim.openstreetmap.org/search?{params}"
    req = urllib.request.Request(
        url,
        headers={"User-Agent": "Hayya-PrayerTimes/1.2 (maintainer script; karimVentus/Private-Prayer)"},
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        results = json.loads(resp.read().decode("utf-8"))
    if not results:
        return None
    lat = float(results[0]["lat"])
    lon = float(results[0]["lon"])
    return lat, lon


def missing_eu_cities(data: dict) -> list[tuple[str, str, str]]:
    coords = data["knownCityCoords"]
    defaults = data["countryDefaults"]
    missing = []
    for code in sorted(EU_CODES):
        if code == "DE":
            continue
        tz = defaults.get(code, {}).get("timezone", "UTC")
        for city in data["citiesByCountry"].get(code, []):
            key = f"{code}_{city}"
            if key not in coords:
                missing.append((code, city, tz))
    return missing


def main() -> int:
    data = json.loads(ASSET_PATH.read_text(encoding="utf-8"))
    cache = load_cache()
    missing = missing_eu_cities(data)
    print(f"Missing EU coords (excl. DE): {len(missing)}")

    fetched = 0
    failed: list[str] = []
    for code, city, tz in missing:
        key = f"{code}_{city}"
        if key in cache:
            lat, lon = cache[key]["latitude"], cache[key]["longitude"]
        else:
            search_city = SEARCH_OVERRIDES.get((code, city), city)
            country = COUNTRY_NAMES[code]
            try:
                result = nominatim_search(search_city, country)
            except Exception as exc:  # noqa: BLE001 — maintainer script
                print(f"  ERROR {key}: {exc}", file=sys.stderr)
                failed.append(key)
                time.sleep(1.1)
                continue
            if result is None:
                print(f"  MISS {key}", file=sys.stderr)
                failed.append(key)
                time.sleep(1.1)
                continue
            lat, lon = result
            cache[key] = {"latitude": round(lat, 3), "longitude": round(lon, 3), "timezone": tz}
            fetched += 1
            print(f"  OK {key} -> {lat:.3f}, {lon:.3f}")
            time.sleep(1.1)

        data["knownCityCoords"][key] = {
            "latitude": cache[key]["latitude"],
            "longitude": cache[key]["longitude"],
            "timezone": tz,
        }

    for code, defaults in COUNTRY_DEFAULT_FIXES.items():
        data["countryDefaults"][code] = defaults

    save_cache(cache)

    if failed:
        print(f"\nFailed to resolve {len(failed)} cities:", ", ".join(failed), file=sys.stderr)
        return 1

    output = json.dumps(data, indent=2, ensure_ascii=False) + "\n"
    ASSET_PATH.write_text(output, encoding="utf-8")
    TEST_RESOURCE_PATH.write_text(output, encoding="utf-8")

    # Report
    coords = data["knownCityCoords"]
    eu_picker = sum(len(data["citiesByCountry"].get(c, [])) for c in EU_CODES)
    eu_have = sum(
        1 for c in EU_CODES for city in data["citiesByCountry"].get(c, [])
        if f"{c}_{city}" in coords
    )
    print(f"\nEU coverage: {eu_have}/{eu_picker} picker cities have coords")
    print(f"File size: {len(output.encode('utf-8'))} bytes")
    print(f"Cache: {len(cache)} entries ({fetched} newly fetched this run)")
    print("Done.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
