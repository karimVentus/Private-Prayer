# Privacy Policy — Prayer Times

**Last updated:** 2026-06-03

Prayer Times is a privacy-respecting Android application for viewing Islamic prayer times. This policy explains what data the app collects, sends, and stores.

## Network usage

The app supports two modes:

### Offline-only mode (default recommendation)

- **Zero network requests.** All prayer times are calculated locally on your device using the `adhan-java` library (Umm al-Qura method + Shafi school).
- City coordinates are bundled with the app for 128 German cities and ~50 major cities worldwide — no geocoding API is called. Entering a custom (unlisted) city will result in a "City not found" error and will not be saved.
- No data is sent over the network.

### Network mode (legacy, opt-in)

When network mode is enabled, the app sends the following to `api.aladhan.com`:

| Event | Data sent | Destination | Can disable? |
|-------|-----------|-------------|--------------|
| Save city (network mode) | City name + country code | `api.aladhan.com` | Yes (use offline-only mode) |
| Fetch timings (network mode) | City name + country code + method/school/latAdj parameters | `api.aladhan.com` | Yes |
| Local calculation | Nothing | — | Default in offline-only |
| GPS | **Never** | — | — |

## No telemetry or tracking

- **No analytics.** The app does not use Google Analytics, Firebase Analytics, or any tracking SDK.
- **No ads.** The app contains no advertising.
- **No crash reporting.** No Firebase Crashlytics, Sentry, or similar.
- **No GPS.** The app never requests `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION`.

## Data storage

All app data is stored locally on your device:

- **City configuration** (city name, country, timezone, coordinates) — stored in Jetpack DataStore.
- **Prayer times cache** (up to 7 days) — stored in a local Room database.
- Neither is synced to any server or cloud.

## Permissions

| Permission | Purpose | Required? |
|------------|---------|-----------|
| `INTERNET` | Legacy network mode only; not used in offline-only mode | Optional |

## Calculation method

Prayer times are calculated using the **Umm al-Qura** method (Fajr angle 18.5°, Isha angle 90 min after Maghrib) with the **Shafi** school for Asr calculation. For locations at latitude ≥ 48°, the high-latitude twilight angle method is applied.

## Changes to this policy

If this policy changes, the app will be updated and the "Last updated" date will be revised. No separate notification will be sent.

## Contact

For questions about this policy, open an issue at:
https://github.com/anomalyco/PrayerTime-/issues
