#!/usr/bin/env python3
"""Generate app/src/main/assets/cities_ar.json from locations.json (EN -> AR)."""

from __future__ import annotations

import json
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LOCATIONS = ROOT / "app/src/main/assets/locations.json"
CACHE = Path(__file__).resolve().parent / ".city-ar-cache.json"
OUT = ROOT / "app/src/main/assets/cities_ar.json"


def ensure_translator():
    try:
        from deep_translator import GoogleTranslator  # noqa: PLC0415
    except ImportError:
        subprocess.check_call(
            [sys.executable, "-m", "pip", "install", "deep-translator", "-q"],
        )
        from deep_translator import GoogleTranslator  # noqa: PLC0415
    return GoogleTranslator(source="en", target="ar")


def main() -> int:
    with LOCATIONS.open(encoding="utf-8") as handle:
        data = json.load(handle)

    cache: dict[str, str] = {}
    if CACHE.exists():
        cache = json.loads(CACHE.read_text(encoding="utf-8"))

    translator = ensure_translator()
    unique = sorted({city for cities in data["citiesByCountry"].values() for city in cities})
    total = len(unique)
    print(f"Translating {total} unique city names…")

    for index, city in enumerate(unique, start=1):
        if city in cache and cache[city].strip():
            continue
        try:
            cache[city] = translator.translate(city)
        except Exception as error:  # noqa: BLE001
            print(f"[{index}/{total}] failed {city!r}: {error}", file=sys.stderr)
            cache[city] = city
        if index % 25 == 0:
            CACHE.write_text(json.dumps(cache, ensure_ascii=False, indent=2), encoding="utf-8")
            print(f"[{index}/{total}] cached")
        time.sleep(0.15)

    cities_ar: dict[str, dict[str, str]] = {}
    for country, cities in data["citiesByCountry"].items():
        cities_ar[country] = {city: cache.get(city, city) for city in cities}

    OUT.write_text(json.dumps(cities_ar, ensure_ascii=False, indent=2), encoding="utf-8")
    CACHE.write_text(json.dumps(cache, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Wrote {OUT} ({len(cities_ar)} countries)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
