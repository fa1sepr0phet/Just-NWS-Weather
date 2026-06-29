package com.nwsweather.di

import android.content.Context
import com.nwsweather.data.local.AppDatabase
import com.nwsweather.data.local.SettingsManager
import com.nwsweather.data.network.ApiModule
import com.nwsweather.data.repository.CityRepository
import com.nwsweather.data.repository.WeatherRepository
import com.nwsweather.location.AndroidLocationClient

class AppContainer(context: Context) {
    private val database = AppDatabase.getInstance(context.applicationContext)
    private val locationClient = AndroidLocationClient(context.applicationContext)
    val settingsManager = SettingsManager(context.applicationContext)

    val weatherRepository: WeatherRepository = WeatherRepository(
        nwsApi = ApiModule.nwsApi,
        savedLocationDao = database.savedLocationDao(),
        pointCacheDao = database.pointCacheDao(),
        weatherSnapshotDao = database.weatherSnapshotDao(),
        locationClient = locationClient,
        settingsManager = settingsManager,
        appContext = context.applicationContext
    )

    val cityRepository: CityRepository = CityRepository(context.applicationContext)
}
