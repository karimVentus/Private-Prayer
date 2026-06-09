#!/usr/bin/env python3
"""Fill knownCityCoords for remaining picker cities (AU, BY, NZ, RU).

Maintainer script: OSM Nominatim lookup with cache in scripts/.catalog-tail-coords-cache.json.
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
CACHE_PATH = Path(__file__).resolve().parent / ".catalog-tail-coords-cache.json"

TAIL_CODES = {"AU", "BY", "NZ", "RU"}

MANUAL_COORDS: dict[tuple[str, str], tuple[float, float]] = {
    ("RU", "Saint Petersburg"): (59.934, 30.335),
    ("NZ", "Palmerston North"): (-40.353, 175.608),
}

# Per-city IANA zones where country default is too coarse (multi-TZ countries).
CITY_TIMEZONE_OVERRIDES: dict[tuple[str, str], str] = {
    ("AU", "Perth"): "Australia/Perth",
    ("AU", "Adelaide"): "Australia/Adelaide",
    ("AU", "Darwin"): "Australia/Darwin",
    ("AU", "Hobart"): "Australia/Hobart",
    ("AU", "Launceston"): "Australia/Hobart",
    ("AU", "Cairns"): "Australia/Brisbane",
    ("AU", "Gold Coast"): "Australia/Brisbane",
    ("AU", "Sunshine Coast"): "Australia/Brisbane",
    ("RU", "Yekaterinburg"): "Asia/Yekaterinburg",
    ("RU", "Chelyabinsk"): "Asia/Yekaterinburg",
    ("RU", "Ufa"): "Asia/Yekaterinburg",
    ("RU", "Perm"): "Asia/Yekaterinburg",
    ("RU", "Novosibirsk"): "Asia/Novosibirsk",
    ("RU", "Krasnoyarsk"): "Asia/Krasnoyarsk",
    ("RU", "Omsk"): "Asia/Omsk",
}

SEARCH_OVERRIDES: dict[tuple[str, str], str] = {
    ("AU", "Sunshine Coast"): "Sunshine Coast Queensland",
    ("AU", "Gold Coast"): "Gold Coast Queensland",
    ("AU", "Newcastle"): "Newcastle New South Wales",
    ("BY", "Mogilev"): "Mahilyow",
    ("BY", "Grodno"): "Hrodna",
    ("BY", "Bobruisk"): "Babruysk",
    ("BY", "Baranovichi"): "Baranavichy",
    ("BY", "Borisov"): "Barysaw",
    ("RU", "Rostov-on-Don"): "Rostov-on-Don",
    ("NZ", "Palmerston North"): "Palmerston North New Zealand",
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


def timezone_for(code: str, city: str, default_tz: str) -> str:
    return CITY_TIMEZONE_OVERRIDES.get((code, city), default_tz)


def missing_tail_cities(data: dict) -> list[tuple[str, str, str]]:
    coords = data["knownCityCoords"]
    defaults = data["countryDefaults"]
    missing = []
    for code in sorted(TAIL_CODES):
        default_tz = defaults.get(code, {}).get("timezone", "UTC")
        for city in data["citiesByCountry"].get(code, []):
            key = f"{code}_{city}"
            if key not in coords:
                missing.append((code, city, timezone_for(code, city, default_tz)))
    return missing


def main() -> int:
    data = json.loads(ASSET_PATH.read_text(encoding="utf-8"))
    names = country_names(data)
    cache = load_cache()
    missing = missing_tail_cities(data)
    print(f"Missing catalog-tail coords: {len(missing)}")

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
    picker = sum(len(cities) for cities in data["citiesByCountry"].values())
    have = sum(
        1
        for code, cities in data["citiesByCountry"].items()
        for city in cities
        if f"{code}_{city}" in coords
    )
    print(f"\nGlobal picker coverage: {have}/{picker}")
    print(f"File size: {len(output.encode('utf-8'))} bytes")
    print(f"Cache: {len(cache)} entries ({fetched} newly fetched this run)")
    print("Done.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
