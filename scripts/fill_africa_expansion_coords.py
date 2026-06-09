#!/usr/bin/env python3
"""Fill knownCityCoords for newly added African picker cities (33-country expansion)."""
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
CACHE_PATH = Path(__file__).resolve().parent / ".africa-expansion-coords-cache.json"

EXPANSION_CODES = {
    "AO",
    "BF",
    "BI",
    "BJ",
    "BW",
    "CD",
    "CF",
    "CG",
    "CI",
    "CM",
    "CV",
    "ER",
    "GA",
    "GM",
    "GN",
    "GQ",
    "GW",
    "LR",
    "LS",
    "MG",
    "ML",
    "MW",
    "MZ",
    "NA",
    "NE",
    "RW",
    "SC",
    "SZ",
    "TD",
    "TG",
    "UG",
    "ZM",
    "ZW",
}

COUNTRY_NAMES = {
    "AO": "Angola",
    "BF": "Burkina Faso",
    "BI": "Burundi",
    "BJ": "Benin",
    "BW": "Botswana",
    "CD": "Democratic Republic of the Congo",
    "CF": "Central African Republic",
    "CG": "Congo",
    "CI": "Cote d'Ivoire",
    "CM": "Cameroon",
    "CV": "Cape Verde",
    "ER": "Eritrea",
    "GA": "Gabon",
    "GM": "Gambia",
    "GN": "Guinea",
    "GQ": "Equatorial Guinea",
    "GW": "Guinea-Bissau",
    "LR": "Liberia",
    "LS": "Lesotho",
    "MG": "Madagascar",
    "ML": "Mali",
    "MW": "Malawi",
    "MZ": "Mozambique",
    "NA": "Namibia",
    "NE": "Niger",
    "RW": "Rwanda",
    "SC": "Seychelles",
    "SZ": "Eswatini",
    "TD": "Chad",
    "TG": "Togo",
    "UG": "Uganda",
    "ZM": "Zambia",
    "ZW": "Zimbabwe",
}

# Per-city timezone when country default is wrong for that city.
TIMEZONE_OVERRIDES: dict[tuple[str, str], str] = {
    ("CD", "Kinshasa"): "Africa/Kinshasa",
    ("CD", "Matadi"): "Africa/Kinshasa",
    ("CD", "Lubumbashi"): "Africa/Lubumbashi",
    ("CD", "Mbuji-Mayi"): "Africa/Lubumbashi",
    ("CD", "Kolwezi"): "Africa/Lubumbashi",
    ("CD", "Kisangani"): "Africa/Lubumbashi",
    ("CD", "Kananga"): "Africa/Lubumbashi",
    ("CD", "Goma"): "Africa/Lubumbashi",
    ("CD", "Bukavu"): "Africa/Lubumbashi",
    ("CD", "Tshikapa"): "Africa/Lubumbashi",
}

MANUAL_COORDS: dict[tuple[str, str], tuple[float, float]] = {
    ("AO", "Uige"): (-7.617, 15.058),
    ("BF", "Fada N'Gourma"): (14.357, -0.753),
    ("CV", "Porto Novo"): (17.021, -25.067),
}

SEARCH_OVERRIDES: dict[tuple[str, str], str] = {
    ("AO", "Uige"): "Uige Angola",
    ("BF", "Fada N'Gourma"): "Fada N'Gourma Burkina Faso",
    ("BF", "Dedougou"): "Dédougou",
    ("BF", "Hounde"): "Houndé",
    ("BW", "Selibe Phikwe"): "Selebi-Phikwe",
    ("CI", "Bouake"): "Bouaké",
    ("CI", "San-Pedro"): "San-Pédro",
    ("CM", "Yaounde"): "Yaoundé",
    ("CM", "Ngaoundere"): "Ngaoundéré",
    ("CV", "Sao Filipe"): "São Filipe",
    ("CV", "Porto Novo"): "Porto Novo Cape Verde",
    ("GA", "Lambarene"): "Lambaréné",
    ("GN", "Nzerekore"): "Nzérékoré",
    ("GN", "Labe"): "Labé",
    ("GN", "Boke"): "Boké",
    ("GN", "Gueckedou"): "Guéckédou",
    ("GW", "Bafata"): "Bafatá",
    ("GW", "Gabu"): "Gabú",
    ("GW", "Bissora"): "Bissorã",
    ("GW", "Catio"): "Catió",
    ("ML", "Segou"): "Ségou",
    ("NE", "Birni N'Konni"): "Birni-N'Konni",
    ("RW", "Huye"): "Butare Rwanda",
    ("RW", "Muhanga"): "Gitarama Rwanda",
    ("RW", "Musanze"): "Ruhengeri Rwanda",
    ("RW", "Rubavu"): "Gisenyi Rwanda",
    ("RW", "Karongi"): "Kibuye Rwanda",
    ("TD", "Abeche"): "Abéché",
    ("TG", "Lome"): "Lomé",
    ("TG", "Kpalime"): "Kpalimé",
    ("TG", "Atakpame"): "Atakpamé",
    ("TG", "Tsevie"): "Tsévié",
    ("TG", "Aneho"): "Aného",
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


def city_timezone(code: str, city: str, defaults: dict) -> str:
    return TIMEZONE_OVERRIDES.get((code, city), defaults.get(code, {}).get("timezone", "UTC"))


def missing_cities(data: dict) -> list[tuple[str, str, str]]:
    coords = data["knownCityCoords"]
    defaults = data["countryDefaults"]
    missing = []
    for code in sorted(EXPANSION_CODES):
        for city in data["citiesByCountry"].get(code, []):
            key = f"{code}_{city}"
            if key not in coords:
                missing.append((code, city, city_timezone(code, city, defaults)))
    return missing


def main() -> int:
    data = json.loads(ASSET_PATH.read_text(encoding="utf-8"))
    cache = load_cache()
    missing = missing_cities(data)
    print(f"Missing expansion coords: {len(missing)}")

    fetched = 0
    failed: list[str] = []
    for index, (code, city, tz) in enumerate(missing, start=1):
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

        if index % 25 == 0:
            save_cache(cache)

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

    picker = sum(len(data["citiesByCountry"].get(c, [])) for c in EXPANSION_CODES)
    have = sum(
        1
        for c in EXPANSION_CODES
        for city in data["citiesByCountry"].get(c, [])
        if f"{c}_{city}" in data["knownCityCoords"]
    )
    print(f"\nExpansion coverage: {have}/{picker} picker cities have coords")
    print(f"File size: {len(output.encode('utf-8'))} bytes")
    print(f"Cache: {len(cache)} entries ({fetched} newly fetched this run)")
    print("Done.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
