#!/usr/bin/env python3
"""Fill knownCityCoords for Americas picker cities missing from locations.json.

Maintainer script: OSM Nominatim lookup with cache in scripts/.america-coords-cache.json.
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
CACHE_PATH = Path(__file__).resolve().parent / ".america-coords-cache.json"

AMERICA_FILL_CODES = {"AR", "BR", "CA", "CL", "CO", "PE", "US", "UY", "VE"}

MANUAL_COORDS: dict[tuple[str, str], tuple[float, float]] = {}

SEARCH_OVERRIDES: dict[tuple[str, str], str] = {
    ("AR", "San Miguel de Tucumán"): "San Miguel de Tucuman",
    ("BR", "Brasília"): "Brasilia",
    ("BR", "São Paulo"): "Sao Paulo",
    ("BR", "Belém"): "Belem",
    ("BR", "Goiânia"): "Goiania",
    ("BR", "São Luís"): "Sao Luis",
    ("CA", "Quebec City"): "Quebec City Canada",
    ("CA", "Kitchener"): "Kitchener Ontario",
    ("CA", "London"): "London Ontario",
    ("CL", "Concepción"): "Concepcion Chile",
    ("CL", "Valparaíso"): "Valparaiso",
    ("CO", "Bogotá"): "Bogota",
    ("CO", "Medellín"): "Medellin",
    ("CO", "Cúcuta"): "Cucuta",
    ("PE", "Cusco"): "Cusco Peru",
    ("UY", "Paysandú"): "Paysandu",
    ("UY", "Ciudad de la Costa"): "Ciudad de la Costa Uruguay",
    ("VE", "Ciudad Guayana"): "Ciudad Guayana Venezuela",
    ("VE", "Mérida"): "Merida Venezuela",
    ("VE", "Barcelona"): "Barcelona Venezuela",
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


def country_names(data: dict) -> dict[str, str]:
    return {entry["code"]: entry["name"] for entry in data["countries"]}


def missing_america_cities(data: dict) -> list[tuple[str, str, str]]:
    coords = data["knownCityCoords"]
    defaults = data["countryDefaults"]
    missing = []
    for code in sorted(AMERICA_FILL_CODES):
        tz = defaults.get(code, {}).get("timezone", "UTC")
        for city in data["citiesByCountry"].get(code, []):
            key = f"{code}_{city}"
            if key not in coords:
                missing.append((code, city, tz))
    return missing


def main() -> int:
    data = json.loads(ASSET_PATH.read_text(encoding="utf-8"))
    names = country_names(data)
    cache = load_cache()
    missing = missing_america_cities(data)
    print(f"Missing Americas coords: {len(missing)}")

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
                save_cache(cache)
            else:
                search_city = SEARCH_OVERRIDES.get((code, city), city)
                country = names[code]
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
                save_cache(cache)
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
    america_picker = sum(len(data["citiesByCountry"].get(c, [])) for c in AMERICA_FILL_CODES)
    america_have = sum(
        1 for c in AMERICA_FILL_CODES for city in data["citiesByCountry"].get(c, [])
        if f"{c}_{city}" in coords
    )
    print(f"\nAmericas target coverage: {america_have}/{america_picker} picker cities have coords")
    print(f"File size: {len(output.encode('utf-8'))} bytes")
    print(f"Cache: {len(cache)} entries ({fetched} newly fetched this run)")
    print("Done.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
