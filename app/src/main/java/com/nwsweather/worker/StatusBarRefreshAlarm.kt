package com.nwsweather.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import java.util.concurrent.TimeUnit

/** A best-effort idle wakeup, alongside the durable 15-minute WorkManager job. */
object StatusBarRefreshAlarm {
    const val ACTION_REFRESH = "com.nwsweather.action.REFRESH_STATUS_TEMPERATURE"
    private const val REQUEST_CODE = 2100

    @Synchronized
    fun sync(context: Context, enabled: Boolean, replace: Boolean = false) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, StatusBarRefreshReceiver::class.java).setAction(ACTION_REFRESH)
        val existing = PendingIntent.getBroadcast(context, REQUEST_CODE, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (!enabled) {
            existing?.let { alarms.cancel(it); it.cancel() }
            return
        }
        // Process startup must not continually push an already scheduled alarm back.
        if (existing != null && !replace) return
        val operation = PendingIntent.getBroadcast(context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        try {
            alarms.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + TimeUnit.MINUTES.toMillis(20),
                operation
            )
        } catch (e: RuntimeException) {
            operation.cancel()
            // Keep periodic work available even if the device refuses an alarm.
            Log.w("StatusBarRefresh", "Could not schedule idle refresh", e)
        }
    }
}
