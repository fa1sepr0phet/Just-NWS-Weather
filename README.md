# Just NWS Weather

> A clean, privacy-focused, open-source Android weather app powered by the National Weather Service.

**No ads, no tracking, just weather.**

Just NWS Weather exists for one reason: to show accurate weather without turning users into the product.

Download from Google Play: https://play.google.com/store/apps/details?id=com.nwsweather.myapp

---

## Screenshots

<p align="center">
  <img src="images/4681clean.png" width="30%" />
  <img src="images/kcclean.png" width="30%" />
  <img src="images/kc2clean.png" width="30%" />
</p>

## Ads and analytics can fuck right off

- No advertising SDKs
- No analytics SDKs
- No user accounts
- No subscriptions
- No paywalls
- No third-party tracking SDKs
- Weather data from the National Weather Service
- Some location-related data is stored locally on-device
- Android backup/restore may copy local app data depending on OS settings
- Fully open source under GPLv3

Just weather.

---

## Features

- Current weather conditions
- Detailed forecasts from the National Weather Service
- GPS-based weather lookup
- Manual address/location search
- Saved favorite locations
- Home screen widget support
- Dynamic weather-based UI
- Modern Kotlin + Jetpack Compose interface

---

## Privacy

This app does not include advertising, analytics, attribution, or crash-reporting SDKs. It does not require an account and does not send app data to ad tech or profiling services.

Location permission is optional for current-location weather, but as the code stands today the app may request it on startup.

Search suggestions come from a bundled U.S. city database. Full address lookup uses Android's `Geocoder`, whose backend may vary by device, Android version, or OEM.

Saved locations, cached weather metadata, the latest weather snapshot, and app settings are stored locally on-device. Depending on Android backup settings, some of that local data may also be included in system backup/restore.

For more detail, see [`PRIVACY.md`](PRIVACY.md).

---

## Network Requests

Just NWS Weather makes network requests only for weather and location-related functionality.

Primary service:

- `api.weather.gov` - National Weather Service weather forecast data

Possible platform/system services:

- Android `Geocoder` - used for full address lookup; the backend may depend on the device, Android version, or OEM
- Google Play Services location APIs in the Play build

The app does not send data to analytics, advertising, crash-reporting, or tracking services, but remote services may still receive IP address and standard network metadata when requests are made.

---

## Standard Build

```bash
git clone https://github.com/fa1sepr0phet/Just-NWS-Weather.git
cd JustWeather
./gradlew assembleRelease
```

For a debug build:

```bash
./gradlew assembleDebug
```

## FOSS / Privacy Build

Just NWS Weather supports a FOSS-oriented build for users and distributors who want to avoid the Google Play Services location dependency.

```bash
./gradlew assembleFossRelease
```

For a debug build:

```bash
./gradlew assembleFossDebug
```
