# Privacy Policy

**Last updated: June 29, 2026**

## Purpose

Just NWS Weather is built to avoid ads, analytics, and user tracking.

This document is meant to describe what the app actually does today, including where location-related data is stored locally and when data leaves the device to make weather, geocoding, widgets, and notifications work.

## Short version

- No advertising SDKs
- No analytics or tracking SDKs
- No user accounts
- No developer-run backend for user profiles or monetization
- Weather data comes from the U.S. National Weather Service
- Some location-related data is stored locally on your device
- Some features rely on Android system services or platform providers
- Android backup and restore may copy local app data, depending on OS settings

## What this app does not do

This app does not include:

- advertising SDKs
- analytics SDKs
- attribution or marketing trackers
- crash-reporting SDKs
- user accounts or sign-in

The app's code does not send data to a developer-controlled server for profiling, ad targeting, or sale of user data.

## Data this app can handle

Depending on which features you use, the app can handle:

- current device location
- manually entered address search text
- saved locations, labels, and coordinates
- cached forecast and alert data tied to a location
- app settings and notification preferences

Some of this information may be personal or sensitive depending on how you use the app. For example, a saved label such as `Home` or a saved street address can be meaningful personal information even if it never leaves your device except through system-managed services or backup flows.

## Permissions

- Location permission is optional for current-location weather lookup. You can deny it and use manual search instead.
- As the code stands today, the app may request location permission when the app starts.
- On Android 13 and newer, the app may request notification permission on first launch.
- Notification permission is used for weather alerts and status-bar temperature features.

## Network and system-service requests

The app currently makes network or system-service requests for:

- weather forecasts, point metadata, and alerts from `https://api.weather.gov/`
- address geocoding through Android's `Geocoder` when you run a full address search
- external browser or app links only when you tap them, such as NWS forecast pages, radar, email, or the Play Store

The app does not add advertising IDs, analytics IDs, or account tokens to these requests.

As with any network request, the remote service or system backend may still see your IP address and standard network metadata.

## Build variants and platform services

- The Play build uses Google Play Services for current-location lookup.
- The FOSS build uses Android's platform `LocationManager` for current-location lookup.
- Manual search suggestions are loaded locally from a bundled U.S. city database.
- Final address resolution still uses Android's `Geocoder`, and its backend may vary by device, Android version, or OEM.

Where the app relies on Android platform services or Google Play Services, those platform providers may have their own privacy behavior and policies outside the direct control of this app.

## Local storage

The app stores data locally in Room and SharedPreferences, including:

- saved locations: label, address, city/state, latitude, longitude, and display order
- point cache: NWS grid and forecast metadata for previously used coordinates
- latest weather snapshot: location name, latitude, longitude, current forecast details, and update timestamp
- settings: theme, temperature unit, notification toggles, tutorial/help state, rating prompt state, and last notified alert ID
- a recent movement timestamp used by the movement tracker

This data is stored in normal app-private Android storage.

The app does not currently add its own encryption layer on top of Android's app sandbox.

## Background behavior

- The app schedules periodic background work through Android WorkManager after app startup.
- Those background jobs may refresh weather data for the last stored location or weather snapshot.
- Widget actions can trigger immediate refreshes.
- If a widget is present, the app may also queue a refresh on some device events, such as the user unlocking the phone.
- If weather alerts or status-bar weather are enabled, refreshed data may be used to update those features.

## Notifications and lock screen visibility

- If severe weather alerts are enabled, the app may show an alert notification when a new alert is seen for the last refreshed location.
- If status-bar temperature is enabled, the app shows an ongoing notification containing the location name, current temperature, and short forecast.
- That status-bar notification is currently marked public and can appear on the lock screen.

## Retention and deletion

- Saved locations stay on the device until you delete them or use the app's `Clear App Data` action.
- Cached point data and the latest weather snapshot stay on the device until replaced or cleared.
- The movement timestamp is deleted once the app decides the device has been stationary long enough.
- The app does not run a developer-controlled remote database for this data.

## Android backup and restore

The app currently has Android backup enabled in the manifest.

That means some locally stored app data may be included in Android backup and restore flows, depending on the device, Android version, and the user's OS settings.

This behavior is controlled by the Android platform, not by a server operated by this app.

## Security notes

- Weather API requests use HTTPS.
- The app relies on standard Android and OkHttp TLS validation.
- The app does not currently use certificate pinning or a custom network security configuration.
- The app does not ship an advertising, analytics, or crash-reporting SDK.

## Third-party and platform services

The app's own code does not include third-party data collection SDKs, but the following external or platform services may still be involved:

- National Weather Service (`api.weather.gov`)
- Android `Geocoder` backend
- Google Play Services location APIs in the Play build

## Changes

This document describes the code as it stands today.

If the app's data flows, permissions, or storage behavior change, this policy should be updated to match.

## Contact

If you have questions or want to report a privacy concern, please open an issue on the GitHub repository.
