package com.nwsweather.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nwsweather.presentation.WeatherViewModel

@Composable
fun WeatherRoute(
    viewModel: WeatherViewModel,
    onRequestCurrentLocation: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    WeatherScreen(
        uiState = uiState,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onCitySuggestionSelected = viewModel::onCitySuggestionSelected,
        onSaveLabelChanged = viewModel::onSaveLabelChanged,
        onThemeChanged = viewModel::onThemeChanged,
        onTemperatureUnitChanged = viewModel::onTemperatureUnitChanged,
        onNotificationsToggleChanged = viewModel::onNotificationsToggleChanged,
        onSearchAddress = viewModel::searchAddress,
        onSavedLocationClick = viewModel::loadSavedLocation,
        onRefresh = viewModel::refreshForecast,
        onDismissError = viewModel::dismissError,
        onUseCurrentLocation = onRequestCurrentLocation,
        onDeleteLocation = viewModel::deleteSavedLocation,
        onEditLocation = viewModel::startEditingLocation,
        onStopEditing = viewModel::stopEditingLocation,
        onShowTutorial = viewModel::onShowTutorial,
        onDismissTutorial = viewModel::onDismissTutorial,
        onDismissSearchHelp = viewModel::onDismissSearchHelp,
        onDismissFavoritesHelp = viewModel::onDismissFavoritesHelp,
        onRateApp = viewModel::onRateApp,
        onDismissRatingPrompt = viewModel::onDismissRatingPrompt,
        onOpenSearch = viewModel::prepareSearch,
        onClearAllData = viewModel::clearAllData,
        onStatusBarTempToggleChanged = viewModel::onStatusBarTempToggleChanged,
        onMoveLocation = viewModel::moveLocation
    )
}
