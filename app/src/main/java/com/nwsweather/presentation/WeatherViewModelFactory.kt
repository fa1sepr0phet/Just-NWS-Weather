package com.nwsweather.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nwsweather.data.local.SettingsManager
import com.nwsweather.data.repository.WeatherRepository

class WeatherViewModelFactory(
    private val repository: WeatherRepository,
    private val settingsManager: SettingsManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(WeatherViewModel::class.java))
        return WeatherViewModel(repository, settingsManager) as T
    }
}
