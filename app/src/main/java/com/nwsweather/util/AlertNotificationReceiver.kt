package com.nwsweather.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.nwsweather.MainActivity
import com.nwsweather.data.local.AppDatabase
import com.nwsweather.data.local.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlertNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == NotificationHelper.ACTION_OPEN_ALERT_NOTIFICATION) {
            openAlertDetails(context, intent.getStringExtra(NotificationHelper.EXTRA_DETAILS_URL))
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                restoreStatusBarTemperature(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun openAlertDetails(context: Context, detailsUrl: String?) {
        val targetIntent = if (!detailsUrl.isNullOrBlank()) {
            Intent(Intent.ACTION_VIEW, Uri.parse(detailsUrl))
        } else {
            Intent(context, MainActivity::class.java)
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(targetIntent)
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
