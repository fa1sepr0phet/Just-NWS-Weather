# Status bar background refresh investigation — October 24, 2026

## Evidence

The connected Samsung SM-S938U runs version 12.9 (109). Notifications are granted and the package is not force-stopped. JobScheduler has the weather worker registered, with a 15-minute minimum latency, network constraint, no charging requirement, and no idle requirement. These are eligibility conditions, not an execution deadline.

Scheduler history showed the periodic job waiting about 25 minutes in total. During that wait, CONNECTIVITY and DEVICE_NOT_DOZING eligibility disappeared. Both periodic and immediate weather work subsequently logged SUCCESS at approximately 13:45 local time. This supports delayed delivery; it does not prove that every previously reported stale reading had the same cause.

The source path is `WidgetRefreshWorker -> WeatherRepository.refreshLatestSnapshot -> saveSnapshot -> NotificationHelper.updateStatusBarTemperature`. It does not require an Activity. Application-level unlock/network listeners cannot start a dead process. The ViewModel refreshes on opening the app, which masks delayed background work. Source observations can also remain unchanged after a successful download; the notification correctly retains the observation timestamp.

Existing uncommitted changes already isolate background temperature fetching from daily forecasts/alerts, retry failures, preserve the notification, and restore work after boot/package replacement. Those changes were retained.

## Change

- Keep the durable 15-minute WorkManager schedule (Android's minimum periodic interval).
- While status bar temperature is enabled, also schedule a non-exported, explicit `ELAPSED_REALTIME_WAKEUP` alarm with `setAndAllowWhileIdle`, targeting 20 minutes.
- The alarm refreshes the last selected location directly, without waiting for an Activity or another ordinary job. It rearms before fetching and uses a bounded seven-second attempt and a nine-second partial wake lock. Timeout/failure enqueues the existing durable worker. The existing network client propagates coroutine cancellation.
- Application startup preserves an existing alarm deadline. Turning the preference off cancels it; turning it on restores it. Boot and package replacement explicitly recreate it.
- No exact-alarm permission, ongoing foreground service, or automatic battery-policy changes are introduced.

## Platform limits

An inexact idle alarm is allowed during Doze but is not a 20-minute deadline. Android/vendor restrictions, network availability, and source latency can still delay fresh data. A slow fetch falls back to WorkManager, which can itself be deferred. Locking alone does not prohibit updates. Force-stop prevents background delivery until the user launches the app again. After reboot, credential-protected settings/database require the first unlock. Background refresh uses the last selected coordinates, not background GPS tracking.

References: [periodic work](https://developer.android.com/reference/androidx/work/PeriodicWorkRequest), [idle alarms](https://developer.android.com/reference/android/app/AlarmManager#setAndAllowWhileIdle(int,long,android.app.PendingIntent)), [Doze restrictions](https://developer.android.com/training/monitoring-device-state/doze-standby).

## Device verification after installing this change

Automated validation completed: 11 unit tests passed in each of the Play and FOSS variants (22 executions), including cancellation of both concurrent requests. FOSS debug assembly and lint passed, and `git diff --check` passed. The new build has not been installed on the connected phone; the diagnostic evidence above comes from its existing installation. The locked/Doze behavior below still requires device validation.

Build-environment note: Windows Java socket creation failed before compilation. Running Gradle with the process-only `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:/__unused_java_socket_directory__` (an intentionally nonexistent directory) selected OpenJDK's TCP fallback and allowed all checks to complete. No repository JVM settings were changed.

1. Enable status temperature, close the Activity, and check `dumpsys alarm` for `REFRESH_STATUS_TEMPERATURE` and a roughly 20-minute deadline. Reopening must not postpone it; disabling must remove it.
2. Lock the phone and observe `StatusBarRefresh` logcat entries and notification updates without opening the app. Check the next alarm remains scheduled after delivery. Allow for OS delay; verify actual fetches instead of expecting a changed temperature every time.
3. Repeat after process eviction (not force-stop), after reboot/first unlock, and package replacement.
4. Test offline and slow responses: receiver must finish promptly, preserve the last reading, and leave retry work queued. Restore network and confirm refresh.
5. Exercise Doze on a test device; restore any test power overrides afterwards. Verify both successful idle delivery and deferred/failing cases. Do not infer locked-phone reliability solely from unit tests.
