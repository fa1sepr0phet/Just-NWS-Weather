package com.nwsweather.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.nwsweather.presentation.WeatherViewModel

@Composable
fun WeatherRoute(
    viewModel: WeatherViewModel,
    onRequestCurrentLocation: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Auto-refresh only while the screen is on show, not while the app is in the background.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.refreshWhileVisible()
        }
    }

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
