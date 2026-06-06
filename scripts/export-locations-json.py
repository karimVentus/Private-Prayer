#!/usr/bin/env python3
"""HISTORICAL — DO NOT RUN.

One-shot exporter that parsed hardcoded maps from LocationDataSource.kt
(countries, citiesByCountry, countryDefaults, knownCityCoords).

LocationDataSource now loads app/src/main/assets/locations.json only.
To change the catalog, edit that JSON (and app/src/test/resources/locations.json
for JVM tests that mirror assets).
"""

from __future__ import annotations

import sys


def main() -> None:
    sys.stderr.write(
        "export-locations-json.py is obsolete (LocationDataSource.kt no longer "
        "has hardcoded maps).\n"
        "Edit app/src/main/assets/locations.json directly.\n",
    )
    raise SystemExit(1)


if __name__ == "__main__":
    main()
