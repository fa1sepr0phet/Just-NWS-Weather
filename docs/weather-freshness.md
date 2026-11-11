# Weather freshness review

## Findings and changes

- Any observation, regardless of age, previously took priority over hourly forecasts. Only the first station was queried. The repository now checks up to three stations concurrently and selects the newest usable temperature observation no more than 30 minutes old. Missing, invalid, future, and stale observation timestamps are rejected.
- The first forecast period was assumed to be current. Period selection now requires that its time range contain the fetch time. Without a recent observation or current hourly forecast, refresh fails explicitly; daily highs/lows and zero are no longer substituted for current temperature.
- Snapshot timestamps previously recorded download time even for old observations. Observations now retain their source timestamps. Hourly forecast snapshots record retrieval time and are identified as forecasts. Reading labels include the date.
- The temperature notification retains the last reading while Android defers background work. It displays "Last reading" and the original reading timestamp without an expiration timer. Failed background refreshes retain it and request a WorkManager retry. Disabling the preference or clearing app data still removes it.
- Background work already used WorkManager's minimum 15-minute interval. This interval remains. The application additionally requests immediate work after network validation or unlocking while its process is alive, independently of widget installation. Enabling the temperature notification requests immediate work. Duplicate immediate requests keep an existing request instead of cancelling it.
- Activity and background workers now share the same repository/settings container. This avoids background work retaining obsolete notification preferences while the app is running.
- Foreground automatic refresh is now every five minutes instead of ten. Superseded UI loads are cancelled, and cancellation propagates through optional requests. Startup restoration avoids cancelling an active location request. Background snapshot writes check that the selected location has not changed during the request.
- Cached device coordinates are limited to two minutes; location acquisition times out after 20 seconds. HTTP calls time out after 45 seconds.
- Alert-fetch failures no longer become an empty alert list. Refresh failures produce a persistent warning until a refresh succeeds, as well as an error message.
- Removed the fabricated UV value of 4. UV is explicitly unavailable because these endpoints do not provide it.

## Limits

These changes cannot guarantee live conditions at all times. Android can defer WorkManager during Doze, battery saving, or app restrictions. An unlock receiver registered at runtime cannot revive a terminated process. Force-stop, disabled notifications, lost connectivity, and delayed source observations remain constraints. Background refresh updates the last selected coordinates; it does not track travel in the background. A recent station report is still a station report, and the hourly fallback is a forecast.

Notifications and widgets can retain cached readings between Android updates. The notification timestamp indicates the reading age; persistence does not guarantee fresh data. The app must not promise continuous safety-critical delivery.

## Verification

Regression tests cover timestamp boundaries, invalid/future timestamps, timezone offsets, forecast range boundaries, stale-observation fallback, observation units/source time, and refusal to use a daily forecast as current temperature. Run both distribution variants' unit tests and lint tasks.

Before release, test on devices with no widget installed: close the activity and observe periodic notification changes; disable/re-enable connectivity; lock for more than 30 minutes and confirm the temperature remains through unlock without reopening the app; keep the device offline past 30 minutes and confirm the last reading and original timestamp remain; restore connectivity and confirm a successful refresh updates the reading; toggle the preference while work runs; switch locations during slow requests; and verify behavior under Doze, battery saver, and force-stop. Confirm denied notification/location permissions and delayed or unavailable NWS endpoints produce truthful UI states.
