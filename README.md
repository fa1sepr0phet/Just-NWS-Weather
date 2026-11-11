# Just NWS Weather

> A clean, privacy-focused, open-source Android weather app powered by the National Weather Service.

**No ads, no tracking, just weather.**

Just NWS Weather exists for one reason: to show accurate weather without turning users into the product.

Download from Google Play: https://play.google.com/store/apps/details?id=com.nwsweather.myapp

---

## Screenshots

| Current conditions | Forecast | More forecast details |
| --- | --- | --- |
| ![Current conditions screen](images/4681clean.png) | ![Kansas City forecast](images/kcclean.png) | ![Additional Kansas City forecast details](images/kc2clean.png) |

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
- Hourly UV forecasts from EPA, with location and forecast time; hidden when no valid hour is available
- GPS-based weather lookup
- Manual address/location search
- Saved favorite locations
- Home screen widgets with daily high/low temperatures; the larger widget includes scrollable day/night forecasts, precipitation chance, and wind
- Separate resizable NWS Forecast Graph widget: the selected location, temperature, sky cover, precipitation chance, and six-hour probability of 12 inches of snow. Uses the shared graph preset (48 hours starting 59 hours ahead); tap the graph to open the NWS page. Refreshes about every 30 minutes when Android permits.
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
- `forecast.weather.gov` - graphical forecast page and image, fetched when the graph widget is installed
- `data.epa.gov` - UV forecasts only, requested using the NWS-provided city and state

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
