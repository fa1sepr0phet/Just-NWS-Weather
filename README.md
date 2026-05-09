# Just NWS Weather

> A clean, privacy-focused, open-source Android weather app powered by the National Weather Service.

**No ads. No analytics. No tracking. No subscriptions.**

Just NWS Weather exists for one reason: to show accurate weather without turning users into the product.

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
- No third-party tracking
- Weather data from the National Weather Service
- Saved locations stored locally on-device
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

The app does not include advertising, analytics, or tracking SDKs. It does not require an account and does not sell, share, or monetize user data.

Location access is optional and used only to retrieve weather for the user’s selected area.

Location autocomplete is performed locally on-device using a bundled U.S. city database. Partial search text is not sent to any third-party service.

For more detail, see [`PRIVACY.md`](PRIVACY.md).

---

## Network Requests

Just NWS Weather makes network requests only for weather and location functionality.

Primary service:

- `api.weather.gov` — National Weather Service weather forecast data

Possible system-level service:

- Android `Geocoder` — used for address lookup; the backend may depend on the device, Android version, or OEM.

Just NWS Weather does not send data to analytics, advertising, crash-reporting, or tracking services.

---

## Standard Build
git clone https://github.com/fa1sepr0phet/Just-NWS-Weather.git
```cd JustWeather
./gradlew assembleRelease
```

For a debug buld:
```
./gradlew assembleDebug

## FOSS / Privacy Build

Just NWS Weather supports a FOSS-oriented build for users and distributors who want to avoid Google Play Services dependencies.

```bash
./gradlew assembleFossRelease
```
For a debug build:
```bash
./gradlew assembleFossDebug
```
