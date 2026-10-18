package com.nwsweather.worker

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nwsweather.di.AppContainer
import com.nwsweather.util.NotificationHelper
import com.nwsweather.widget.WeatherAppWidget
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
            
            var result = repository.refreshLatestSnapshot()
            
            // If no snapshot exists (e.g. after destructive migration), try to recover using the first saved location
            if (result == null) {
                val locations = repository.getSavedLocations()
                if (locations.isNotEmpty()) {
                    result = repository.loadForecastForSavedLocation(locations.first())
                }
            }
            WeatherAppWidget().updateAll(applicationContext)

            val helper = NotificationHelper(applicationContext)
            helper.createNotificationChannel()

            if (settings.notificationsEnabled.value && result != null && result.alerts.isNotEmpty()) {
                val alert = result.alerts.first()
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

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun enqueueImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
