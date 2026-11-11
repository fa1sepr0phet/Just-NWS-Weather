package com.nwsweather

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.ContextCompat
import com.nwsweather.di.AppContainer
import com.nwsweather.worker.WidgetRefreshWorker
import com.nwsweather.worker.StatusBarRefreshAlarm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NwsWeatherApp : Application() {
    val container by lazy { AppContainer(this) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refreshIfNeeded()
        }
    }
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) refreshIfNeeded()
        }
    }

    override fun onCreate() {
        super.onCreate()
        WidgetRefreshWorker.schedulePeriodic(this)
        StatusBarRefreshAlarm.sync(this, container.settingsManager.statusBarTempEnabled.value)
        // Runtime receivers also work when no home-screen widget is installed.
        ContextCompat.registerReceiver(this, unlockReceiver, IntentFilter(Intent.ACTION_USER_PRESENT), ContextCompat.RECEIVER_NOT_EXPORTED)
        getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(networkCallback)
    }

    private fun refreshIfNeeded() {
        scope.launch {
            val snapshot = container.weatherRepository.getLatestSnapshot()
            if (snapshot == null || System.currentTimeMillis() - snapshot.updatedAtEpochMs >= 60_000L) {
                WidgetRefreshWorker.enqueueImmediate(this@NwsWeatherApp)
            }
        }
    }
}
