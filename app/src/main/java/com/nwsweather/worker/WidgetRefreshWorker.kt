package com.nwsweather.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nwsweather.util.NotificationHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class WidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        runCatching {
            val container = (applicationContext as com.nwsweather.NwsWeatherApp).container
            val repository = container.weatherRepository
            val settings = container.settingsManager
            
            val result = repository.refreshLatestSnapshot()

            val helper = NotificationHelper(applicationContext)
            helper.createNotificationChannel()

            // Fetch alerts only after publishing the temperature. A failed alert
            // request still retries, but cannot hold the status icon hostage.
            val alerts = if (settings.notificationsEnabled.value && result != null) {
                repository.fetchAlerts(result.latitude, result.longitude)
            } else emptyList()
            if (result != null && alerts.isNotEmpty()) {
                val alert = alerts.first()
                val lastAlertId = settings.getLastNotifiedAlertId()
                
                // Only show a notification if this is a NEW alert we haven't notified for yet
                if (alert.id != lastAlertId) {
                    helper.showWeatherAlert(
                        title = alert.event ?: "Weather Alert",
                        message = alert.headline ?: "Severe weather conditions reported.",
                        detailsUrl = "https://forecast.weather.gov/MapClick.php?lat=${result.latitude}&lon=${result.longitude}"
                    )
                    settings.setLastNotifiedAlertId(alert.id)
                }
            }
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { 
                if (it is CancellationException) throw it
                // A temporary network/server failure must not remove the last
                // temperature while WorkManager waits to retry.
                it.printStackTrace()
                Result.retry() 
            }
        )
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "weather_widget_periodic_refresh"
        private const val IMMEDIATE_WORK_NAME = "weather_widget_immediate_refresh"

        fun schedulePeriodic(context: Context): Operation {
            val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(15, TimeUnit.MINUTES)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            return WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun enqueueImmediate(context: Context): Operation {
            val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
                .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            return WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
