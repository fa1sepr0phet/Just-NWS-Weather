package com.nwsweather.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nwsweather.data.local.SavedLocationEntity
import com.nwsweather.data.local.SettingsManager
import com.nwsweather.data.repository.CityRepository
import com.nwsweather.data.repository.CitySuggestion
import com.nwsweather.data.repository.WeatherRepository
import com.nwsweather.util.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WeatherViewModel(
    private val repository: WeatherRepository,
    private val cityRepository: CityRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        observeSavedLocations()
        observeSettings()
        checkRatingPrompt()
        refreshStatusBarTemp()
    }

    fun prepareSearch() {
        searchJob?.cancel()
        _uiState.update { 
            it.copy(
                searchQuery = "", 
                citySuggestions = emptyList(),
                saveLabel = "",
                editingLocation = null 
            ) 
        }
    }

    fun onSearchQueryChanged(value: String) {
        _uiState.update { it.copy(searchQuery = value) }
        
        searchJob?.cancel()
        val trimmed = value.trim()
        if (trimmed.length < 2) {
            _uiState.update { it.copy(citySuggestions = emptyList()) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(300) // Debounce to prevent flicker and unnecessary work
            val suggestions = cityRepository.searchCities(trimmed)
            _uiState.update { it.copy(citySuggestions = suggestions) }
        }
    }

    fun onCitySuggestionSelected(suggestion: CitySuggestion) {
        _uiState.update {
            it.copy(
                searchQuery = suggestion.fullDisplay,
                citySuggestions = emptyList()
            )
        }
        searchAddress()
    }

    fun onSaveLabelChanged(value: String) {
        _uiState.update { it.copy(saveLabel = value) }
    }

    fun onThemeChanged(theme: AppTheme) {
        settingsManager.setTheme(theme)
    }

    fun onTemperatureUnitChanged(unit: TemperatureUnit) {
        settingsManager.setTemperatureUnit(unit)
        refreshStatusBarTemp()
    }

    fun onNotificationsToggleChanged(enabled: Boolean) {
        settingsManager.setNotificationsEnabled(enabled)
    }

    fun onStatusBarTempToggleChanged(enabled: Boolean) {
        settingsManager.setStatusBarTempEnabled(enabled)
        if (enabled) {
            val helper = NotificationHelper(repository.appContext)
            if (!helper.hasNotificationPermission()) {
                _uiState.update { it.copy(errorMessage = "Notification permission is required for the status bar temperature display.") }
            }
            refreshStatusBarTemp()
        } else {
            NotificationHelper(repository.appContext).cancelStatusBarTemperature()
        }
    }

    fun refreshStatusBarTemp() {
        if (!settingsManager.statusBarTempEnabled.value) return
        viewModelScope.launch {
            val snapshot = repository.getLatestSnapshot()
            if (snapshot != null) {
                NotificationHelper(repository.appContext).updateStatusBarTemperature(
                    temp = snapshot.temperature,
                    sourceUnit = snapshot.temperatureUnit,
                    targetUnit = settingsManager.unit.value,
                    locationName = snapshot.locationName,
                    forecast = snapshot.shortForecast,
                    isDaytime = snapshot.isDaytime
                )
            }
        }
    }

    fun onShowTutorial() {
        settingsManager.setShowTutorial(true)
    }

    fun onDismissTutorial() {
        settingsManager.setShowTutorial(false)
    }

    fun onDismissRatingPrompt() {
        _uiState.update { it.copy(showRatingPrompt = false) }
    }

    fun onDismissSearchHelp() {
        settingsManager.setHasSeenSearchHelp(true)
    }

    fun onDismissFavoritesHelp() {
        settingsManager.setHasSeenFavoritesHelp(true)
    }

    fun onRateApp() {
        settingsManager.markRated()
        _uiState.update { it.copy(showRatingPrompt = false) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun searchAddress() {
        val current = _uiState.value
        val query = current.searchQuery.trim()
        if (query.isBlank()) return

        _uiState.update { it.copy(citySuggestions = emptyList()) }
        
        launchLoad {
            repository.loadForecastForAddress(
                address = query,
                label = current.saveLabel.trim().ifBlank { null },
                existingId = current.editingLocation?.id
            )
        }
        
        if (current.editingLocation != null) {
            stopEditingLocation()
        }
    }

    fun fetchCurrentLocation() {
        launchLoad {
            repository.loadForecastForCurrentLocation()
        }
    }

    fun loadSavedLocation(location: SavedLocationEntity) {
        launchLoad {
            repository.loadForecastForSavedLocation(location)
        }
    }

    fun refreshForecast() {
        val forecastResult = _uiState.value.forecastResult ?: return
        launchLoad {
            repository.loadForecastForCoordinates(
                latitude = forecastResult.latitude,
                longitude = forecastResult.longitude,
                source = forecastResult.source
            )
        }
    }

    fun onLocationPermissionDenied() {
        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = "Location permission was denied. You can still search by address, or enable location in your device settings under Apps > Just NWS Weather > Permissions."
            )
        }
    }

    fun deleteSavedLocation(location: SavedLocationEntity) {
        viewModelScope.launch {
            repository.deleteSavedLocation(location)
        }
    }

    fun moveLocation(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        viewModelScope.launch {
            repository.reorderSavedLocations(fromIndex, toIndex)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _uiState.update {
                it.copy(
                    forecastResult = null,
                    locationName = null,
                    searchQuery = "",
                    saveLabel = ""
                )
            }
        }
    }

    fun startEditingLocation(location: SavedLocationEntity) {
        _uiState.update {
            it.copy(
                editingLocation = location,
                searchQuery = location.address,
                saveLabel = location.label
            )
        }
    }

    fun stopEditingLocation() {
        _uiState.update { 
            it.copy(
                editingLocation = null, 
                searchQuery = "", 
                saveLabel = "",
                citySuggestions = emptyList()
            ) 
        }
    }

    private fun observeSavedLocations() {
        viewModelScope.launch {
            repository.observeSavedLocations().collect { locations ->
                _uiState.update { it.copy(savedLocations = locations) }
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            combine(
                settingsManager.theme,
                settingsManager.unit,
                settingsManager.notificationsEnabled,
                settingsManager.statusBarTempEnabled,
                settingsManager.showTutorial
            ) { theme, unit, notifications, statusBarTemp, showTutorial ->
                ObservedSettings(
                    theme = theme,
                    temperatureUnit = unit,
                    notificationsEnabled = notifications,
                    statusBarTempEnabled = statusBarTemp,
                    showTutorial = showTutorial
                )
            }.combine(settingsManager.hasSeenSearchHelp) { settings, seenSearch ->
                settings.copy(hasSeenSearchHelp = seenSearch)
            }.combine(settingsManager.hasSeenFavoritesHelp) { settings, seenFavorites ->
                _uiState.update {
                    it.copy(
                        theme = settings.theme,
                        temperatureUnit = settings.temperatureUnit,
                        notificationsEnabled = settings.notificationsEnabled,
                        statusBarTempEnabled = settings.statusBarTempEnabled,
                        showTutorial = settings.showTutorial,
                        showSearchHelp = !settings.hasSeenSearchHelp,
                        showFavoritesHelp = !seenFavorites
                    )
                }
            }.collect {}
        }
    }

    private fun checkRatingPrompt() {
        if (settingsManager.shouldPromptForRating()) {
            _uiState.update { it.copy(showRatingPrompt = true) }
        }
    }

    private fun launchLoad(block: suspend () -> com.nwsweather.data.repository.ForecastLoadResult): kotlinx.coroutines.Job {
        return viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { block() }
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            forecastResult = result,
                            locationName = result.locationName,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Something went sideways."
                        )
                    }
                }
        }
    }

    private data class ObservedSettings(
        val theme: AppTheme,
        val temperatureUnit: TemperatureUnit,
        val notificationsEnabled: Boolean,
        val statusBarTempEnabled: Boolean,
        val showTutorial: Boolean,
        val hasSeenSearchHelp: Boolean = false
    )
}
