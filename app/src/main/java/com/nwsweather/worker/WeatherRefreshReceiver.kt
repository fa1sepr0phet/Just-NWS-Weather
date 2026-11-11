package com.nwsweather.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/** Restore weather work after an update/reboot without waiting for an Activity. */
class WeatherRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        val app = appContext as com.nwsweather.NwsWeatherApp
        StatusBarRefreshAlarm.sync(app, app.container.settingsManager.statusBarTempEnabled.value, replace = true)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Keep the receiver alive until both requests are persisted.
                WidgetRefreshWorker.schedulePeriodic(appContext).result.get(4, TimeUnit.SECONDS)
                WidgetRefreshWorker.enqueueImmediate(appContext).result.get(4, TimeUnit.SECONDS)
            } catch (e: Exception) {
                Log.e("WeatherRefreshReceiver", "Could not restore weather refresh work", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
