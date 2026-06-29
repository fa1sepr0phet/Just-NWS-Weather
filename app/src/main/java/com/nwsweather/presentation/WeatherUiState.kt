package com.nwsweather.presentation

import com.nwsweather.data.local.SavedLocationEntity
import com.nwsweather.data.repository.CitySuggestion
import com.nwsweather.data.repository.ForecastLoadResult

enum class AppTheme {
    SYSTEM, LIGHT, DARK, MIDNIGHT
}

enum class TemperatureUnit {
    FAHRENHEIT, CELSIUS
}

data class WeatherUiState(
    val searchQuery: String = "",
    val citySuggestions: List<CitySuggestion> = emptyList(),
    val saveLabel: String = "",
    val isLoading: Boolean = false,
    val locationName: String? = null,
    val forecastResult: ForecastLoadResult? = null,
    val savedLocations: List<SavedLocationEntity> = emptyList(),
    val errorMessage: String? = null,
    val theme: AppTheme = AppTheme.SYSTEM,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.FAHRENHEIT,
    val notificationsEnabled: Boolean = false,
    val statusBarTempEnabled: Boolean = false,
    val showTutorial: Boolean = false,
    val showRatingPrompt: Boolean = false,
    val showSearchHelp: Boolean = false,
    val showFavoritesHelp: Boolean = false,
    val editingLocation: SavedLocationEntity? = null
)
