#!/usr/bin/env python3
"""Fill knownCityCoords for African picker cities missing from locations.json.

Maintainer script: OSM Nominatim lookup with cache in scripts/.africa-coords-cache.json.
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
CACHE_PATH = Path(__file__).resolve().parent / ".africa-coords-cache.json"

# Picker countries in catalog that still need coords (Jun 2026 baseline).
AFRICA_CODES = {"ET", "GH", "KE", "KM", "NG", "SL", "SN", "TZ", "ZA"}

COUNTRY_NAMES = {
    "ET": "Ethiopia",
    "GH": "Ghana",
    "KE": "Kenya",
    "KM": "Comoros",
    "NG": "Nigeria",
    "SL": "Sierra Leone",
    "SN": "Senegal",
    "TZ": "Tanzania",
    "ZA": "South Africa",
}

# Curated coords when Nominatim has no match (city, lat, lng).
MANUAL_COORDS: dict[tuple[str, str], tuple[float, float]] = {
    ("KM", "Mitsamiouli"): (-11.385, 43.384),
}

SEARCH_OVERRIDES: dict[tuple[str, str], str] = {
    ("ET", "Gonder"): "Gondar",
    ("ET", "Awasa"): "Hawassa",
    ("GH", "Wa"): "Wa Ghana",
    ("SL", "Bo"): "Bo Sierra Leone",
    ("SN", "Thiès"): "Thies Senegal",
    ("SN", "Saint-Louis"): "Saint-Louis Senegal",
    ("TZ", "Zanzibar City"): "Zanzibar",
    ("ZA", "Port Elizabeth"): "Gqeberha",
    ("ZA", "Nelspruit"): "Mbombela",
    ("ZA", "Pretoria"): "Pretoria South Africa",
}


def load_cache() -> dict[str, dict]:
    if CACHE_PATH.exists():
        return json.loads(CACHE_PATH.read_text(encoding="utf-8"))
    return {}


def save_cache(cache: dict[str, dict]) -> None:
    CACHE_PATH.write_text(json.dumps(cache, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def nominatim_search(city: str, country: str) -> tuple[float, float] | None:
    query = f"{city}, {country}"
    params = urllib.parse.urlencode({"q": query, "format": "json", "limit": 1})
    url = f"https://nominatim.openstreetmap.org/search?{params}"
    req = urllib.request.Request(
        url,
        headers={"User-Agent": "Hayya-PrayerTimes/1.2 (maintainer script; karimVentus/Private-Prayer)"},
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        results = json.loads(resp.read().decode("utf-8"))
    if not results:
        return None
    return float(results[0]["lat"]), float(results[0]["lon"])


def missing_africa_cities(data: dict) -> list[tuple[str, str, str]]:
    coords = data["knownCityCoords"]
    defaults = data["countryDefaults"]
    missing = []
    for code in sorted(AFRICA_CODES):
        tz = defaults.get(code, {}).get("timezone", "UTC")
        for city in data["citiesByCountry"].get(code, []):
            key = f"{code}_{city}"
            if key not in coords:
                missing.append((code, city, tz))
    return missing


def main() -> int:
    data = json.loads(ASSET_PATH.read_text(encoding="utf-8"))
    cache = load_cache()
    missing = missing_africa_cities(data)
    print(f"Missing Africa coords: {len(missing)}")

    fetched = 0
    failed: list[str] = []
    for code, city, tz in missing:
        key = f"{code}_{city}"
        if key not in cache:
            manual = MANUAL_COORDS.get((code, city))
            if manual is not None:
                lat, lon = manual
                cache[key] = {"latitude": round(lat, 3), "longitude": round(lon, 3), "timezone": tz}
                fetched += 1
                print(f"  OK {key} -> {lat:.3f}, {lon:.3f} (manual)")
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

    save_cache(cache)

    if failed:
        print(f"\nFailed to resolve {len(failed)} cities:", ", ".join(failed), file=sys.stderr)
        return 1

    output = json.dumps(data, indent=2, ensure_ascii=False) + "\n"
    ASSET_PATH.write_text(output, encoding="utf-8")
    TEST_RESOURCE_PATH.write_text(output, encoding="utf-8")

    coords = data["knownCityCoords"]
    africa_picker = sum(len(data["citiesByCountry"].get(c, [])) for c in AFRICA_CODES)
    africa_have = sum(
        1 for c in AFRICA_CODES for city in data["citiesByCountry"].get(c, [])
        if f"{c}_{city}" in coords
    )
    print(f"\nAfrica target coverage: {africa_have}/{africa_picker} picker cities have coords")
    print(f"File size: {len(output.encode('utf-8'))} bytes")
    print(f"Cache: {len(cache)} entries ({fetched} newly fetched this run)")
    print("Done.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
