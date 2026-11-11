package com.nwsweather.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nwsweather.data.local.AppDatabase
import com.nwsweather.data.local.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Runs when an alert notification is dismissed, to put the status bar temperature back. */
class AlertNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                restoreStatusBarTemperature(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun restoreStatusBarTemperature(context: Context) {
        val settings = SettingsManager(context)
        if (!settings.statusBarTempEnabled.value) return

        val snapshot = AppDatabase.getInstance(context).weatherSnapshotDao().getLatest() ?: return
        NotificationHelper(context).updateStatusBarTemperature(
            temp = snapshot.temperature,
            sourceUnit = snapshot.temperatureUnit,
            targetUnit = settings.unit.value,
            locationName = snapshot.locationName,
            forecast = snapshot.shortForecast,
            isDaytime = snapshot.isDaytime
        )
    }
}
