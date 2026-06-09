#!/usr/bin/env python3
"""Fill knownCityCoords for remaining catalog countries (Americas, Caribbean, Pacific, KR/TW/BT)."""
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
CACHE_PATH = Path(__file__).resolve().parent / ".remaining-coords-cache.json"
ADD_SCRIPT = Path(__file__).resolve().parent / "add_remaining_cities.py"

TIMEZONE_OVERRIDES: dict[tuple[str, str], str] = {
    ("EC", "Santo Domingo"): "America/Guayaquil",
    ("MX", "Tijuana"): "America/Tijuana",
    ("MX", "Ciudad Juarez"): "America/Oaxaca",
    ("MX", "Merida"): "America/Merida",
}

MANUAL_COORDS: dict[tuple[str, str], tuple[float, float]] = {
    ("BB", "Six Cross Roads"): (13.092, -59.533),
    ("BS", "George Town"): (23.516, -75.779),
    ("BT", "Jakar"): (27.549, 90.752),
    ("BZ", "Benque Viejo del Carmen"): (17.075, -89.139),
    ("DO", "Higuey"): (18.615, -68.708),
    ("EC", "Santo Domingo"): (-0.254, -79.171),
    ("FM", "Nema"): (6.963, 152.575),
    ("FM", "Satowan"): (5.331, 153.738),
    ("FM", "Kuttu"): (5.459, 153.456),
    ("GD", "St. George's"): (12.053, -61.750),
    ("HT", "Gonaives"): (19.452, -72.689),
    ("HT", "Cap-Haitien"): (19.760, -72.203),
    ("JM", "Savanna-la-Mar"): (18.217, -78.133),
    ("KI", "Taburao"): (0.091, 173.680),
    ("KR", "Daegu"): (35.871, 128.601),
    ("KR", "Daejeon"): (36.350, 127.385),
    ("KR", "Gwangju"): (35.160, 126.853),
    ("LC", "Bisee"): (13.967, -60.974),
    ("LC", "Soufriere"): (13.857, -61.057),
    ("NI", "Jinotepe"): (11.849, -86.199),
    ("PA", "Penonome"): (8.509, -80.357),
    ("SR", "Groningen"): (5.800, -55.467),
    ("SV", "Ahuachapan"): (13.921, -89.846),
    ("TO", "Nuku'alofa"): (-21.136, -175.203),
    ("TO", "Mu'a"): (-21.149, -175.183),
    ("TO", "Ha'ateiho"): (-21.158, -175.204),
    ("TV", "Lolua"): (-8.535, 179.121),
    ("TV", "Motufoua"): (-7.470, 178.683),
    ("TV", "Fagatua"): (-8.553, 179.077),
}

SEARCH_OVERRIDES: dict[tuple[str, str], str] = {
    ("BB", "Six Cross Roads"): "Six Cross Roads Barbados",
    ("BO", "La Paz"): "La Paz Bolivia",
    ("BO", "Potosi"): "Potosí Bolivia",
    ("BS", "George Town"): "George Town Bahamas",
    ("BT", "Jakar"): "Jakar Bhutan",
    ("BZ", "Benque Viejo del Carmen"): "Benque Viejo Belize",
    ("CR", "San Jose"): "San José Costa Rica",
    ("CU", "Camaguey"): "Camagüey Cuba",
    ("CU", "Holguin"): "Holguín Cuba",
    ("CU", "Guantanamo"): "Guantánamo Cuba",
    ("CU", "Pinar del Rio"): "Pinar del Río Cuba",
    ("DO", "Higuey"): "Higüey Dominican Republic",
    ("EC", "Santo Domingo"): "Santo Domingo de los Colorados Ecuador",
    ("EC", "Portoviejo"): "Portoviejo Ecuador",
    ("GD", "St. George's"): "Saint George's Grenada",
    ("GT", "Mixco"): "Mixco Guatemala",
    ("HT", "Petion-Ville"): "Pétion-Ville Haiti",
    ("HT", "Gonaives"): "Gonaïves Haiti",
    ("HT", "Cap-Haitien"): "Cap-Haïtien Haiti",
    ("HN", "San Pedro Sula"): "San Pedro Sula Honduras",
    ("HN", "Comayagua"): "Comayagua Honduras",
    ("JM", "Savanna-la-Mar"): "Savanna-la-Mar Jamaica",
    ("KR", "Daegu"): "Daegu South Korea",
    ("KR", "Daejeon"): "Daejeon South Korea",
    ("KR", "Gwangju"): "Gwangju South Korea",
    ("LC", "Bisee"): "Bisée Saint Lucia",
    ("LC", "Soufriere"): "Soufrière Saint Lucia",
    ("MX", "Leon"): "León Mexico",
    ("MX", "Merida"): "Mérida Mexico",
    ("MX", "San Luis Potosi"): "San Luis Potosí Mexico",
    ("NI", "Leon"): "León Nicaragua",
    ("NI", "Jinotepe"): "Jinotepe Nicaragua",
    ("PA", "Colon"): "Colón Panama",
    ("PA", "Penonome"): "Penonomé Panama",
    ("PA", "Chitre"): "Chitré Panama",
    ("PY", "Asuncion"): "Asunción Paraguay",
    ("PY", "Encarnacion"): "Encarnación Paraguay",
    ("SM", "San Marino"): "City of San Marino",
    ("SR", "Groningen"): "Groningen Suriname",
    ("SV", "Ahuachapan"): "Ahuachapán El Salvador",
    ("TO", "Nuku'alofa"): "Nukuʻalofa Tonga",
    ("TO", "Mu'a"): "Muʻa Tonga",
    ("TO", "Ha'ateiho"): "Haʻateiho Tonga",
    ("TW", "Kaohsiung"): "Kaohsiung Taiwan",
    ("TW", "Taichung"): "Taichung Taiwan",
    ("TW", "Hsinchu"): "Hsinchu Taiwan",
    ("VU", "Port Vila"): "Port-Vila Vanuatu",
}


def load_expansion_codes() -> set[str]:
    import importlib.util

    spec = importlib.util.spec_from_file_location("add_remaining_cities", ADD_SCRIPT)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return set(module.REMAINING_EXPANSION.keys())


def country_names(data: dict) -> dict[str, str]:
    return {c["code"]: c["name"] for c in data["countries"]}


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


def missing_cities(data: dict, codes: set[str]) -> list[tuple[str, str, str]]:
    coords = data["knownCityCoords"]
    defaults = data["countryDefaults"]
    missing = []
    for code in sorted(codes):
        for city in data["citiesByCountry"].get(code, []):
            key = f"{code}_{city}"
            if key not in coords:
                missing.append((code, city, city_timezone(code, city, defaults)))
    return missing


def main() -> int:
    codes = load_expansion_codes()
    data = json.loads(ASSET_PATH.read_text(encoding="utf-8"))
    names = country_names(data)

    # Ensure Tonga default exists (catalog country without countryDefaults entry).
    if "TO" not in data["countryDefaults"]:
        data["countryDefaults"]["TO"] = {
            "latitude": -21.2,
            "longitude": -175.2,
            "timezone": "Pacific/Tongatapu",
        }

    cache = load_cache()
    missing = missing_cities(data, codes)
    print(f"Missing remaining coords: {len(missing)}")

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

    picker = sum(len(data["citiesByCountry"].get(c, [])) for c in codes)
    have = sum(
        1
        for c in codes
        for city in data["citiesByCountry"].get(c, [])
        if f"{c}_{city}" in data["knownCityCoords"]
    )
    total_picker = sum(len(v) for v in data["citiesByCountry"].values())
    print(f"\nRemaining expansion: {have}/{picker} coords")
    print(f"Global picker cities: {total_picker}")
    print(f"File size: {len(output.encode('utf-8'))} bytes")
    print(f"Cache: {len(cache)} entries ({fetched} newly fetched this run)")
    print("Done.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
