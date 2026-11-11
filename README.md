# Just NWS Weather

**No ads, no tracking, just weather.**

Just NWS Weather is an open-source Android weather app that gets its forecasts straight from the U.S. National Weather Service. I built it because checking the weather shouldn't mean handing your location to an ad network. There's no account to create, nothing to subscribe to, and nobody on the other end building a profile of you.

[Get it on Google Play](https://play.google.com/store/apps/details?id=com.nwsweather.myapp), or [build it yourself](#building-it-yourself).

## Ads and analytics can fuck right off

Most weather apps are free because your location is the product. This one is free because the forecast data is already yours: the National Weather Service is taxpayer-funded and publishes everything openly.

So here is what you won't find in this app:

- Ads, or any advertising SDK
- Analytics, attribution, or crash-reporting SDKs
- Accounts or sign-ins
- Subscriptions or paywalls
- A server of mine sitting between you and the weather

The whole thing is GPLv3, so you don't have to take my word for any of it. Read the code.

## What it does

**The forecast**

- Current temperature and conditions for wherever you are, or anywhere in the U.S. you search for
- Wind, humidity, and chance of precipitation for the current hour
- A day-by-day forecast with highs, lows, chance of precipitation, and wind
- The current hour's UV index, from the EPA
- NWS weather alerts for the location you're looking at, with the full text one tap away
- One-tap links to NWS radar and the full weather.gov forecast page

**Getting around**

- Use your device's location, or search by city, ZIP code, or street address
- Save the places you check often and drag them into the order you want
- Fahrenheit or Celsius
- Four themes: System, Light, Dark, and a true-black Midnight
- A background that changes with the weather

**Without opening the app**

- Home screen widgets in four sizes (2x1, 4x1, 2x2, 4x2). The largest one adds a scrollable day and night forecast with precipitation chance and wind.
- An NWS Forecast Graph widget that shows the same hourly chart you'd see on weather.gov: temperature, sky cover, chance of precipitation, and snowfall probability for the next 48 hours. Tap it to open the full NWS page.
- Optional notifications for new weather alerts
- An optional status bar notification that keeps the current temperature in view

## How it works

There is no backend. Your phone talks to the government's weather services directly, and the answer goes straight onto your screen.

1. **Finding the forecast.** The app sends your coordinates to the NWS, which replies with the forecast office and grid square that cover that spot. Coordinates are rounded to three decimal places first (roughly a city block), so the NWS never gets your exact position.
2. **The current temperature.** This comes from real observation stations, not the forecast. The app checks the three nearest stations and uses the newest reading, as long as it's less than 30 minutes old. If none of them has a recent reading, it falls back to the hourly forecast and labels it as a forecast. It will not pass off a daily high as the current temperature.
3. **The UV index.** The NWS API doesn't provide UV, so that one number comes from the EPA's hourly UV forecast instead. The app asks the EPA using the city and state that the NWS reported for your location, never your coordinates or address. If the EPA has nothing for the current hour (which happens for some smaller towns), the app hides the UV reading instead of guessing.
4. **Alerts.** Active NWS alerts for the location are fetched along with the forecast. If the alert request fails, the refresh fails too, so you're never shown "no alerts" when the app simply couldn't check.
5. **Widgets and the status bar.** A background job refreshes the last location you viewed about every 15 minutes and saves the result on your phone. The widgets and the status bar temperature read from that saved copy.

## A few things worth knowing

**It only works in the United States.** The National Weather Service covers the U.S. and its territories, and so does this app.

**Background updates are up to Android.** Widgets and the status bar temperature refresh roughly every 15 to 30 minutes, but Android decides when background work actually runs. Battery saver and Doze can delay it. Every reading carries a timestamp so you can tell how old it is.

**Widgets and alert notifications follow the last location you looked at.** If you checked the weather in another city and left it there, that's the city your widget and alert notifications are watching.

**Don't rely on it as your only source of severe weather warnings.** Alert notifications depend on the same background updates, so they can arrive late.

## Privacy

The short version: the app talks to government weather services and to Android itself, and that's all.

- Your saved locations, settings, and the most recent forecast are stored on your device. If Android backup is turned on, the system may include that data in your backups.
- Location permission is optional. Right now the app asks for it when it starts, but you can say no and search by city or address instead.
- City suggestions come from a list of U.S. cities bundled with the app, so typing in the search box doesn't send anything anywhere. Looking up a full street address uses Android's built-in geocoder, and which service sits behind that depends on your device.

The full details are in [PRIVACY.md](PRIVACY.md).

### Where the app connects

| Host | What for |
| --- | --- |
| `api.weather.gov` | Forecasts, current observations, and alerts |
| `forecast.weather.gov` | The forecast graph image, only if you use the graph widget |
| `data.epa.gov` | Hourly UV forecasts, requested by city and state |

Two system services can also be involved: Android's geocoder for full address searches, and Google Play Services for location in the Play Store build. The FOSS build doesn't use Play Services.

The radar and "full forecast" links open weather.gov pages in your browser, outside the app.

None of these requests carry an advertising ID, an account token, or anything else that identifies you. As with any internet request, the service on the other end can still see your IP address.

## Building it yourself

You'll need a recent Android Studio, or the Android SDK and JDK 17. The app runs on Android 8.1 and newer.

```bash
git clone https://github.com/fa1sepr0phet/Just-NWS-Weather.git
cd Just-NWS-Weather
```

There are two flavors of the app:

| Flavor | Location provider | Build command |
| --- | --- | --- |
| `play` | Google Play Services | `./gradlew assemblePlayDebug` |
| `foss` | Android's built-in location manager, no Google dependency | `./gradlew assembleFossDebug` |

Pick `foss` if you want to avoid Google Play Services entirely. Everything else about the two builds is the same.

To run the unit tests:

```bash
./gradlew testPlayDebugUnitTest testFossDebugUnitTest
```

Release builds (`assemblePlayRelease`, `assembleFossRelease`) are signed with a keystore that isn't in this repository. To make your own, put a keystore at `my-release-key.jks` in the project root and set `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD` in `local.properties`.

### What it's built with

Kotlin and Jetpack Compose with Material 3 for the app, Glance for the home screen widgets, Room for on-device storage, Retrofit and OkHttp for the network calls, and WorkManager for background refreshes.

If you want to find your way around the code, everything lives under `app/src/main/java/com/nwsweather`:

| Folder | What's in it |
| --- | --- |
| `data/` | The NWS and EPA API clients, the Room database, and the repository that ties them together |
| `presentation/` | The view model and UI state |
| `ui/` | The Compose screen, its components, and the weather-based theming |
| `widget/` | The home screen widgets, including the forecast graph |
| `worker/` | Background refresh for the widgets and the status bar temperature |

The only code that differs between the two flavors is the location client, in `app/src/play` and `app/src/foss`.

## Feedback and contributing

Found a bug, or have an idea? [Open an issue](https://github.com/fa1sepr0phet/Just-NWS-Weather/issues). Pull requests are welcome too. Please read the [Code of Conduct](CODE_OF_CONDUCT.md) first.

## License

[GPLv3](LICENSE). Use it, change it, share it. If you distribute a modified version, keep it open under the same license.

Weather data comes from the [National Weather Service](https://www.weather.gov/), and UV data from the [U.S. Environmental Protection Agency](https://www.epa.gov/). This app is not affiliated with or endorsed by either agency.
