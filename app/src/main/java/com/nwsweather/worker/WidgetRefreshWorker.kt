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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class WidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        runCatching {
            val container = AppContainer(applicationContext)
            val repository = container.weatherRepository
            val settings = container.settingsManager
            
            val result = repository.refreshLatestSnapshot()
            WeatherAppWidget().updateAll(applicationContext)

            val helper = NotificationHelper(applicationContext)
            helper.createNotificationChannel()

            if (settings.statusBarTempEnabled.value && result != null) {
                val period = result.currentHourlyPeriod ?: result.currentPeriod
                if (period != null) {
                    helper.updateStatusBarTemperature(
                        temp = period.temperature,
                        locationName = result.locationName,
                        forecast = period.shortForecast ?: "",
                        isDaytime = period.isDaytime
                    )
                }
            } else if (!settings.statusBarTempEnabled.value) {
                helper.cancelStatusBarTemperature()
            }

            if (settings.notificationsEnabled.value && result != null && result.alerts.isNotEmpty()) {
                val alert = result.alerts.first()
                val lastAlertId = settings.getLastNotifiedAlertId()
                
                // Only show a notification if this is a NEW alert we haven't notified for yet
                if (alert.id != lastAlertId) {
                    helper.showWeatherAlert(
                        title = alert.event ?: "Weather Alert",
                        message = alert.headline ?: "Severe weather conditions reported."
                    )
                    settings.setLastNotifiedAlertId(alert.id)
                }
            }
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "weather_widget_periodic_refresh"
        private const val IMMEDIATE_WORK_NAME = "weather_widget_immediate_refresh"

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(20, TimeUnit.MINUTES)
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
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
