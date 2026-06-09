#!/usr/bin/env python3
"""Fill knownCityCoords for Asian picker cities missing from locations.json.

Maintainer script: OSM Nominatim lookup with cache in scripts/.asia-coords-cache.json.
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
CACHE_PATH = Path(__file__).resolve().parent / ".asia-coords-cache.json"

# Catalog countries with missing coords at Phase 8D baseline (Jun 2026).
ASIA_FILL_CODES = {
    "AF", "AM", "AZ", "BD", "BN", "CN", "GE", "ID", "IL", "IN", "IR", "JP", "KG", "KH",
    "KZ", "LA", "LK", "MM", "MN", "MV", "MY", "NP", "PH", "PK", "SG", "TH", "TJ", "TM",
    "UZ", "VN",
}

MANUAL_COORDS: dict[tuple[str, str], tuple[float, float]] = {
    ("LA", "Phonsavan"): (19.450, 103.192),
    ("LA", "Xam Neua"): (20.415, 104.045),
    ("MN", "Ulgii"): (48.968, 89.968),
    ("MN", "Ulaangom"): (49.983, 92.067),
    ("PH", "General Santos"): (6.113, 125.172),
    ("TJ", "Bokhtar"): (37.838, 68.781),
    ("TM", "Abadan"): (38.033, 58.317),
    ("UZ", "Khiva"): (41.378, 60.364),
}

SEARCH_OVERRIDES: dict[tuple[str, str], str] = {
    ("AF", "Mazar-i-Sharif"): "Mazar-e Sharif",
    ("AF", "Lashkar Gah"): "Lashkargah",
    ("AM", "Vagharshapat"): "Etchmiadzin",
    ("AZ", "Ganja"): "Gəncə",
    ("AZ", "Shirvan"): "Şirvan",
    ("BD", "Barisal"): "Barishal",
    ("BD", "Bogra"): "Bogura",
    ("BD", "Comilla"): "Cumilla",
    ("BD", "Cox's Bazar"): "Cox's Bazar Bangladesh",
    ("CN", "Xi'an"): "Xi'an",
    ("ID", "Bandar Lampung"): "Bandar Lampung",
    ("IL", "Petah Tikva"): "Petah Tikva Israel",
    ("IL", "Beersheba"): "Be'er Sheva",
    ("IN", "Bengaluru"): "Bengaluru",
    ("IR", "Mashhad"): "Mashhad",
    ("IR", "Isfahan"): "Esfahan",
    ("IR", "Urmia"): "Urmia",
    ("JP", "Kyoto"): "Kyoto Japan",
    ("KG", "Jalal-Abad"): "Jalal-Abad Kyrgyzstan",
    ("KG", "Kara-Balta"): "Kara-Balta",
    ("KH", "Sihanoukville"): "Sihanoukville",
    ("KZ", "Nur-Sultan"): "Astana",
    ("KZ", "Karagandy"): "Karaganda",
    ("KZ", "Oskemen"): "Oskemen Kazakhstan",
    ("LA", "Muang Xay"): "Muang Xai",
    ("LA", "Phonsavan"): "Phonsavan Laos",
    ("LA", "Xam Neua"): "Xam Neua Laos",
    ("MN", "Mörön"): "Moron Mongolia",
    ("MN", "Ulgii"): "Olgii Mongolia",
    ("MN", "Ulaangom"): "Ulaangom Mongolia",
    ("MV", "Malé"): "Male Maldives",
    ("MY", "Malacca"): "Melaka",
    ("MY", "Penang"): "George Town Penang",
    ("MY", "Kuala Terengganu"): "Kuala Terengganu Malaysia",
    ("PK", "Hyderabad"): "Hyderabad Pakistan",
    ("PK", "Quetta"): "Quetta Pakistan",
    ("PH", "Quezon City"): "Quezon City Philippines",
    ("PH", "General Santos"): "General Santos Philippines",
    ("TJ", "Bokhtar"): "Bokhtar Tajikistan",
    ("TJ", "Konibodom"): "Konibodom",
    ("TM", "Konye-Urgench"): "Kunya-Urgench",
    ("TM", "Turkmenbashi"): "Turkmenbashi Turkmenistan",
    ("TM", "Abadan"): "Abadan Turkmenistan",
    ("UZ", "Khiva"): "Khiva Uzbekistan",
    ("VN", "Ho Chi Minh City"): "Ho Chi Minh City Vietnam",
    ("VN", "Da Nang"): "Da Nang Vietnam",
    ("VN", "Da Lat"): "Da Lat Vietnam",
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


def missing_asia_cities(data: dict) -> list[tuple[str, str, str]]:
    coords = data["knownCityCoords"]
    defaults = data["countryDefaults"]
    missing = []
    for code in sorted(ASIA_FILL_CODES):
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
    missing = missing_asia_cities(data)
    print(f"Missing Asia coords: {len(missing)}")

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
    asia_picker = sum(len(data["citiesByCountry"].get(c, [])) for c in ASIA_FILL_CODES)
    asia_have = sum(
        1 for c in ASIA_FILL_CODES for city in data["citiesByCountry"].get(c, [])
        if f"{c}_{city}" in coords
    )
    print(f"\nAsia target coverage: {asia_have}/{asia_picker} picker cities have coords")
    print(f"File size: {len(output.encode('utf-8'))} bytes")
    print(f"Cache: {len(cache)} entries ({fetched} newly fetched this run)")
    print("Done.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
