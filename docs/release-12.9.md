# 12.9 (version code 109)

## Play Console release notes

Improved background updates for the status bar temperature and weather widgets. Temperature updates no longer depend on daily forecast or weather-alert requests succeeding. Improved recovery after network failures, phone restarts, and app updates.

## Review findings

The previous background worker called the full foreground forecast loader. That loader required the daily forecast and alert requests to succeed before saving a temperature, even with severe-weather notifications disabled. A failure in either endpoint discarded otherwise usable observations/hourly data. Snapshot saving also required a current daily forecast period unnecessarily. Repeated failures used exponential retry delays; opening the app started a separate foreground fetch and could appear to fix the stuck notification.

Background refresh now uses only observations/hourly forecasts, keeps the saved location label, and publishes the temperature before requesting enabled alerts. Failed alert requests still retry and never become a successful "no alerts" result. Freshness checks still reject stale observations and daily highs/lows as current temperatures. Retry delays grow linearly from one minute. A manifest receiver persists periodic and immediate work after boot and package replacement, independently of widgets or an open activity.

These are confirmed code failure paths, not a device-level diagnosis: no Android device was connected during this review. Android can still defer periodic work during Doze or battery restrictions; the 15-minute interval is not an exact timer. Runtime unlock/network callbacks cannot revive a killed process, and force-stop blocks background execution until the user launches the app again.

References: [Android periodic work](https://developer.android.com/reference/androidx/work/PeriodicWorkRequest), [retry policies](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work), [manifest receivers](https://developer.android.com/guide/topics/manifest/receiver-element).

## Release verification

- Verified: Play and FOSS unit tests (10 each, zero failures), Play release lint (zero errors, 21 warnings), and minified Play release bundle packaging. The final check completed successfully. The merged manifest declares 12.9 / 109, and R8 retains the worker and reboot receiver. Signed bundle creation is blocked by missing local signing credentials.
- Regression tests cover independent observation/hourly failures, missing daily forecasts, stale/missing readings, and worker cancellation.
- Before production rollout, install through Play internal testing with no widget installed. Enable status bar temperature, leave the activity, and confirm the reading timestamp changes after background work. Test reboot and upgrade recovery, offline/online transitions, and idle/battery saver behavior.
- Signing is not configured in this checkout: `local.properties` contains only `sdk.dir`. To generate an uploadable bundle, use Android Studio's **Build > Generate Signed App Bundle / APK > Android App Bundle**, select the existing upload key and `playRelease`, or configure `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD` locally and run `:app:bundlePlayRelease`. Never commit those credentials. The Gradle bundle output is `app/build/outputs/bundle/playRelease/app-play-release.aab`. Verify code 109 exceeds the highest version already uploaded in Play Console.
- Keep the existing untracked support sources, Room schemas, and tests in source control when committing this release; they were present before this fix and are used by the build.
