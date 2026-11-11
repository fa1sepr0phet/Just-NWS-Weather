package com.nwsweather.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.nwsweather.NwsWeatherApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

/** Runs without an Activity or widget, including during an allowed idle alarm. */
class StatusBarRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != StatusBarRefreshAlarm.ACTION_REFRESH) return
        val app = context.applicationContext as NwsWeatherApp
        val enabled = app.container.settingsManager.statusBarTempEnabled.value
        // Rearm before any network operation, including a failed/timed-out one.
        StatusBarRefreshAlarm.sync(app, enabled, replace = true)
        if (!enabled) return

        val pending = goAsync()
        val wakeLock = app.getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "JustWeather:statusRefresh")
        wakeLock.setReferenceCounted(false)
        wakeLock.acquire(9_000L)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Idle alarms grant only a brief execution window. Do not merely
                // enqueue an ordinary job here: that job can still wait for Doze.
                val completed = withTimeoutOrNull(7_000L) {
                    app.container.weatherRepository.refreshLatestSnapshot()
                    true
                } ?: false
                if (completed) {
                    Log.i("StatusBarRefresh", "Idle refresh completed")
                } else {
                    Log.i("StatusBarRefresh", "Idle refresh timed out; deferring to worker")
                    enqueueFallback(app)
                }
            } catch (e: Exception) {
                Log.w("StatusBarRefresh", "Idle refresh failed; deferring to worker", e)
                enqueueFallback(app)
            } finally {
                if (wakeLock.isHeld) wakeLock.release()
                pending.finish()
            }
        }
    }

    private fun enqueueFallback(context: Context) {
        try {
            WidgetRefreshWorker.enqueueImmediate(context).result.get(1, TimeUnit.SECONDS)
        } catch (e: Exception) {
            Log.w("StatusBarRefresh", "Could not persist retry; periodic work remains scheduled", e)
        }
    }
}
