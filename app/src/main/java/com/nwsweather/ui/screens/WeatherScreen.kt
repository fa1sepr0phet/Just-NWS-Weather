package com.nwsweather.ui.screens

import android.content.Intent
import androidx.core.net.toUri
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nwsweather.data.local.SavedLocationEntity
import com.nwsweather.presentation.AppTheme
import com.nwsweather.presentation.TemperatureUnit
import com.nwsweather.presentation.WeatherUiState
import com.nwsweather.util.groupForecastPeriods
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.nwsweather.ui.components.ForecastCard
import com.nwsweather.ui.components.LocationSearchCard
import com.nwsweather.ui.components.SavedLocationChips
import com.nwsweather.ui.components.WeatherAtmosphereAnimation
import com.nwsweather.ui.components.WeatherHeroCard
import com.nwsweather.ui.theme.resolveAppTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.alpha
import com.nwsweather.ui.theme.mapWeatherMood
import com.nwsweather.ui.theme.visualsForMood
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.scale

import androidx.compose.material3.Surface
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

import com.nwsweather.data.repository.CitySuggestion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    uiState: WeatherUiState,
    onSearchQueryChanged: (String) -> Unit,
    onCitySuggestionSelected: (CitySuggestion) -> Unit,
    onSaveLabelChanged: (String) -> Unit,
    onThemeChanged: (AppTheme) -> Unit,
    onTemperatureUnitChanged: (TemperatureUnit) -> Unit,
    onNotificationsToggleChanged: (Boolean) -> Unit,
    onSearchAddress: () -> Unit,
    onSavedLocationClick: (SavedLocationEntity) -> Unit,
    onRefresh: () -> Unit,
    onDismissError: () -> Unit,
    onUseCurrentLocation: () -> Unit,
    onDeleteLocation: (SavedLocationEntity) -> Unit,
    onEditLocation: (SavedLocationEntity) -> Unit,
    onStopEditing: () -> Unit,
    onShowTutorial: () -> Unit,
    onDismissTutorial: () -> Unit,
    onDismissSearchHelp: () -> Unit,
    onDismissFavoritesHelp: () -> Unit,
    onRateApp: () -> Unit,
    onDismissRatingPrompt: () -> Unit,
    onOpenSearch: () -> Unit,
    onClearAllData: () -> Unit,
    onStatusBarTempToggleChanged: (Boolean) -> Unit,
    onMoveLocation: (fromIndex: Int, toIndex: Int) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showThemeMenu by remember { mutableStateOf(false) }
    var showSearchMenu by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }
    var showClearDataConfirm by remember { mutableStateOf(false) }
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val context = LocalContext.current

    var uvNowEpochMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            uvNowEpochMs = System.currentTimeMillis()
            kotlinx.coroutines.delay(60_000L)
        }
    }

    val forecastResult = uiState.forecastResult
    // Keep the details attached to the exact forecast that supplied the alerts.
    var showAlertDetails by remember(forecastResult) { mutableStateOf(false) }
    if (showAlertDetails && forecastResult != null && forecastResult.alerts.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showAlertDetails = false },
            title = { Text("Weather alerts — ${forecastResult.locationName}") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.refreshFailed) {
                        Text("Refresh failed. These alerts may be outdated.")
                    }
                    forecastResult.alerts.forEach { alert ->
                        Text(alert.event ?: "Weather alert", fontWeight = FontWeight.Bold)
                        alert.headline?.takeIf { it.isNotBlank() }?.let { Text(it) }
                        Text("Affected area: ${alert.areaDesc?.takeIf { it.isNotBlank() } ?: "Not supplied by NWS"}")
                        Text(alert.description?.takeIf { it.isNotBlank() } ?: "No description supplied by NWS.")
                        alert.instruction?.takeIf { it.isNotBlank() }?.let { Text(it) }
                        HorizontalDivider()
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAlertDetails = false }) { Text("Close") }
            }
        )
    }
    val current = forecastResult?.currentPeriod

    val mood = mapWeatherMood(
        forecast = forecastResult?.currentShortForecast ?: current?.shortForecast ?: "",
        isDay = forecastResult?.isDaytime ?: current?.isDaytime ?: true
    )

    val openNwsWebsite = { lat: Double, lon: Double ->
        val url = "https://forecast.weather.gov/MapClick.php?lat=$lat&lon=$lon"
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        context.startActivity(intent)
    }

    val openRadar = { lat: Double, lon: Double ->
        val json = """{"agenda":{"id":"national","center":[$lon,$lat],"location":[$lon,$lat],"zoom":7,"layer":"bref_qcd"},"animating":true,"base":"standard","artcc":false,"county":false,"cwa":false,"rfc":false,"state":false,"menu":false,"shortFusedOnly":false,"opacity":{"alerts":0.8,"local":0.6,"localStations":0.8,"national":0.6}}"""
        val encoded = Base64.encodeToString(json.toByteArray(), Base64.NO_WRAP or Base64.URL_SAFE)
        val url = "https://radar.weather.gov/?settings=v1_$encoded"
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        context.startActivity(intent)
    }

    val openPlayStore = {
        val packageName = context.packageName
        val intent = Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            context.startActivity(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$packageName".toUri()))
        }
    }

    val resolvedTheme = resolveAppTheme(uiState.theme, isSystemInDarkTheme())
    val visuals = visualsForMood(mood, resolvedTheme, MaterialTheme.colorScheme)

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onDismissError()
    }

    if (showThemeMenu) {
        AlertDialog(
            onDismissRequest = { showThemeMenu = false },
            title = { Text("Theme") },
            text = {
                Column(Modifier.selectableGroup()) {
                    listOf(
                        AppTheme.SYSTEM to "System",
                        AppTheme.LIGHT to "Light",
                        AppTheme.DARK to "Dark",
                        AppTheme.MIDNIGHT to "Midnight"
                    ).forEach { (theme, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(
                                selected = uiState.theme == theme,
                                role = Role.RadioButton,
                                onClick = {
                                    onThemeChanged(theme)
                                    showThemeMenu = false
                                }
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = uiState.theme == theme, onClick = null)
                            Text(label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeMenu = false }) { Text("Close") }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(visuals.background)
    ) {
        if (resolvedTheme == AppTheme.LIGHT) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.14f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        if (resolvedTheme != AppTheme.MIDNIGHT) {
            Box(Modifier.fillMaxSize().alpha(if (resolvedTheme == AppTheme.DARK) 0.25f else 1f)) {
                WeatherAtmosphereAnimation(mood = mood)
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = visuals.cardColor,
                        scrolledContainerColor = visuals.cardColor,
                        titleContentColor = visuals.onCardTextColor,
                        actionIconContentColor = visuals.onCardTextColor
                    ),
                    navigationIcon = {
                        // Radar lives on the main screen so it is one tap away.
                        TextButton(
                            enabled = forecastResult != null,
                            onClick = { forecastResult?.let { openRadar(it.latitude, it.longitude) } },
                            colors = ButtonDefaults.textButtonColors(contentColor = visuals.onCardTextColor)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Radar,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text("Radar", modifier = Modifier.padding(start = 6.dp))
                        }
                    },
                    title = {
                        Text(
                            text = uiState.locationName ?: "Just NWS Weather",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                onOpenSearch()
                                showSearchMenu = true
                            }
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Locations and settings menu"
                            )
                        }

                        if (showSearchMenu) {
                            ModalBottomSheet(
                                onDismissRequest = { showSearchMenu = false },
                                sheetState = sheetState,
                                // The sheet stays solid; only the cards on the main screen are see-through.
                                containerColor = visuals.cardColor.copy(alpha = 1f),
                                contentColor = visuals.onCardTextColor
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .padding(bottom = 32.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(24.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box {
                                            TextButton(onClick = { showSettingsMenu = true }) {
                                                Text("Settings", color = visuals.onCardTextColor)
                                            }
                                            DropdownMenu(
                                                expanded = showSettingsMenu,
                                                onDismissRequest = { showSettingsMenu = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text("Temperature Unit")
                                                            Text(
                                                                text = if (uiState.temperatureUnit == TemperatureUnit.FAHRENHEIT) "\u00B0F" else "\u00B0C",
                                                                style = MaterialTheme.typography.labelLarge,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    },
                                                    leadingIcon = { Icon(Icons.Outlined.Thermostat, null) },
                                                    onClick = {
                                                        val nextUnit = if (uiState.temperatureUnit == TemperatureUnit.FAHRENHEIT)
                                                            TemperatureUnit.CELSIUS else TemperatureUnit.FAHRENHEIT
                                                        onTemperatureUnitChanged(nextUnit)
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text("Weather Alerts")
                                                            Switch(
                                                                checked = uiState.notificationsEnabled,
                                                                onCheckedChange = onNotificationsToggleChanged,
                                                                modifier = Modifier.scale(0.8f)
                                                            )
                                                        }
                                                    },
                                                    leadingIcon = { Icon(Icons.Outlined.Warning, null) },
                                                    onClick = { onNotificationsToggleChanged(!uiState.notificationsEnabled) }
                                                )
                                                DropdownMenuItem(
                                                    text = {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text("Status Bar Temp")
                                                            Switch(
                                                                checked = uiState.statusBarTempEnabled,
                                                                onCheckedChange = onStatusBarTempToggleChanged,
                                                                modifier = Modifier.scale(0.8f)
                                                            )
                                                        }
                                                    },
                                                    leadingIcon = { Icon(Icons.Outlined.Thermostat, null) },
                                                    onClick = { onStatusBarTempToggleChanged(!uiState.statusBarTempEnabled) }
                                                )
                                                HorizontalDivider()
                                                DropdownMenuItem(
                                                    text = { Text("Rate Just NWS Weather") },
                                                    leadingIcon = { Icon(Icons.Outlined.Star, null) },
                                                    onClick = {
                                                        openPlayStore()
                                                        showSettingsMenu = false
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Send Feedback") },
                                                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.HelpOutline, null) },
                                                    onClick = {
                                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                                            data = "mailto:falsepr0phet@protonmail.com".toUri()
                                                            putExtra(Intent.EXTRA_SUBJECT, "Just NWS Weather Feedback")
                                                        }
                                                        context.startActivity(intent)
                                                        showSettingsMenu = false
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("App Tutorial") },
                                                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.HelpOutline, null) },
                                                    onClick = {
                                                        onShowTutorial()
                                                        showSettingsMenu = false
                                                    }
                                                )
                                                HorizontalDivider()
                                                DropdownMenuItem(
                                                    text = { Text("Clear App Data", color = MaterialTheme.colorScheme.error) },
                                                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                                    onClick = {
                                                        showClearDataConfirm = true
                                                        showSettingsMenu = false
                                                    }
                                                )
                                            }
                                        }

                                        Box {
                                            TextButton(onClick = { showThemeMenu = true }) {
                                                Text("Theme", color = visuals.onCardTextColor)
                                            }
                                        }

                                    }
                                    val isEditing = uiState.editingLocation != null

                                    LocationSearchCard(
                                        searchQuery = uiState.searchQuery,
                                        citySuggestions = uiState.citySuggestions,
                                        onCitySuggestionSelected = { suggestion ->
                                            onCitySuggestionSelected(suggestion)
                                        },
                                        saveLabel = uiState.saveLabel,
                                        isLoading = uiState.isLoading,
                                        onSearchQueryChanged = onSearchQueryChanged,
                                        onSaveLabelChanged = onSaveLabelChanged,
                                        onUseCurrentLocation = {
                                            onUseCurrentLocation()
                                            showSearchMenu = false
                                        },
                                        onSearchAddress = {
                                            onSearchAddress()
                                            if (isEditing) onStopEditing()
                                            showSearchMenu = false
                                        },
                                        hasAlerts = false,
                                        cardColor = Color.Transparent,
                                        textColor = visuals.onCardTextColor,
                                        showHelp = uiState.showSearchHelp,
                                        onDismissHelp = onDismissSearchHelp
                                    )

                                    if (isEditing) {
                                        TextButton(
                                            onClick = onStopEditing,
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                        ) {
                                            Text("Cancel Editing", color = MaterialTheme.colorScheme.primary)
                                        }
                                    }

                                    if (uiState.savedLocations.isNotEmpty() && !isEditing) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text(
                                                text = "Manage saved locations",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = visuals.onCardTextColor
                                            )

                                            uiState.savedLocations.forEach { location ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clickable {
                                                                onEditLocation(location)
                                                            }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.LocationOn,
                                                            contentDescription = null,
                                                            tint = visuals.onCardTextColor.copy(alpha = 0.6f)
                                                        )
                                                        Column {
                                                            Text(
                                                                text = location.label,
                                                                style = MaterialTheme.typography.bodyLarge,
                                                                color = visuals.onCardTextColor
                                                            )
                                                            Text(
                                                                text = location.address,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = visuals.onCardTextColor.copy(alpha = 0.7f),
                                                                maxLines = 1
                                                            )
                                                        }
                                                    }

                                                    IconButton(
                                                        onClick = { onDeleteLocation(location) }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Delete ${location.label}",
                                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        IconButton(
                            onClick = onRefresh,
                            enabled = uiState.forecastResult != null && !uiState.isLoading
                        ) {
                            Icon(
                                Icons.Outlined.Refresh,
                                contentDescription = "Refresh forecast"
                            )
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            if (uiState.showTutorial) {
                TutorialOverlay(onDismiss = onDismissTutorial)
            }

            if (uiState.showRatingPrompt) {
                RatingPrompt(
                    onRate = {
                        onRateApp()
                        openPlayStore()
                    },
                    onDismiss = onDismissRatingPrompt
                )
            }

            if (showClearDataConfirm) {
                AlertDialog(
                    onDismissRequest = { showClearDataConfirm = false },
                    title = { Text("Clear All App Data?") },
                    text = { Text("This will delete all saved locations and cached weather data. This action cannot be undone.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onClearAllData()
                                showClearDataConfirm = false
                            }
                        ) {
                            Text("Clear Everything", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearDataConfirm = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = onRefresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (uiState.refreshFailed) {
                        item {
                            Text(
                                text = "Refresh failed. Weather and alert information may be outdated.",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    if (uiState.forecastResult?.alerts?.isNotEmpty() == true) {
                        item {
                            ElevatedCard(
                                onClick = { showAlertDetails = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = uiState.forecastResult.alerts
                                                .map { it.event ?: "Weather alert" }.distinct().joinToString(),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${uiState.forecastResult.locationName} • Tap for affected areas and details",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (uiState.savedLocations.isNotEmpty()) {
                        item {
                            Text(
                                text = "Saved locations",
                                style = MaterialTheme.typography.titleMedium,
                                color = visuals.appBarContentColor
                            )
                        }

                        if (uiState.showFavoritesHelp) {
                            item {
                                ElevatedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.elevatedCardColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            "Your saved locations appear here for quick access. You can manage them in the menu.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        TextButton(
                                            onClick = onDismissFavoritesHelp,
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text("Got it", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            SavedLocationChips(
                                locations = uiState.savedLocations,
                                onClick = onSavedLocationClick,
                                onMoveLocation = onMoveLocation,
                                textColor = visuals.appBarContentColor
                            )
                        }
                    }

                    if (uiState.isLoading) {
                        item {
                            Text(
                                text = "Loading forecast...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = visuals.appBarContentColor
                            )
                        }
                    }

                    val result = uiState.forecastResult
                    if (result != null) {
                        result.currentPeriod?.let { currentPeriod ->
                            item {
                                WeatherHeroCard(
                                    period = currentPeriod,
                                    uvForecast = result.uvForecastHours.firstOrNull { it.covers(uvNowEpochMs) },
                                    uvTimeZoneId = result.timeZoneId,
                                    hourlyPeriod = result.currentHourlyPeriod,
                                    displayTemperature = result.currentTemperatureValue,
                                    displayTemperatureUnit = result.currentTemperatureUnit,
                                    forecastText = result.currentShortForecast,
                                    readingTimestampLabel = result.currentReadingTimestampLabel,
                                    isDaytime = result.isDaytime,
                                    temperatureUnit = uiState.temperatureUnit,
                                    cardColor = visuals.cardColor,
                                    textColor = visuals.onCardTextColor,
                                    onClick = { openNwsWebsite(result.latitude, result.longitude) }
                                )
                            }
                        }

                        if (result.upcomingPeriods.isNotEmpty()) {
                            item {
                                HorizontalDivider(color = visuals.appBarContentColor.copy(alpha = 0.28f))
                            }
                            item {
                                Text(
                                    text = "Upcoming forecast",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = visuals.appBarContentColor
                                )
                            }

                            val groupedPeriods = groupForecastPeriods(result.upcomingPeriods)

                            items(groupedPeriods) { periods ->
                                ForecastCard(
                                    periods = periods,
                                    temperatureUnit = uiState.temperatureUnit,
                                    cardColor = visuals.cardColor,
                                    textColor = visuals.onCardTextColor,
                                    onClick = { openNwsWebsite(result.latitude, result.longitude) }
                                )
                            }
                        }
                    } else if (!uiState.isLoading) {
                        item {
                            Text(
                                text = "Search an address or use your current location to load a forecast.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = visuals.appBarContentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RatingPrompt(
    onRate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enjoying Just NWS Weather?") },
        text = { Text("Your rating helps others find this privacy-focused weather app. If you'd rather skip it, we won't ask again.") },
        confirmButton = {
            TextButton(onClick = onRate) {
                Text("Rate Now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("No Thanks")
            }
        }
    )
}

private data class TutorialStep(val title: String, val body: String)

// Keep these in step with the screen: each one describes something the user can see or tap.
private val tutorialSteps = listOf(
    TutorialStep(
        title = "Welcome to Just NWS Weather",
        body = "The top card shows the current temperature and conditions, with a timestamp " +
            "so you can tell how fresh the reading is. Swipe down or tap the refresh icon to update."
    ),
    TutorialStep(
        title = "Find a place",
        body = "Tap the menu icon in the top right, then search by city, ZIP code, or address, " +
            "or use your current location. Fill in a label before you search to save the place."
    ),
    TutorialStep(
        title = "Saved places",
        body = "Saved places appear as chips on the main screen. Tap one to load it, or press and hold, " +
            "then drag, to reorder. To rename or delete one, open the menu and look under " +
            "\"Manage saved locations\"."
    ),
    TutorialStep(
        title = "Radar, settings, and theme",
        body = "Tap Radar in the top left to open the NWS radar for the place you are viewing. " +
            "Settings and Theme are at the top of the menu: switch between \u00B0F and \u00B0C, " +
            "turn on Weather Alerts, and keep the temperature in your status bar."
    ),
    TutorialStep(
        title = "Alerts and full forecasts",
        body = "When the National Weather Service has an alert for the place you are viewing, a banner " +
            "appears at the top. Tap it for the full text. Tap any forecast card to open the complete " +
            "forecast on weather.gov."
    ),
    TutorialStep(
        title = "You're all set",
        body = "You can also add home screen widgets, including an NWS forecast graph. " +
            "No ads, no tracking, just weather."
    )
)

@Composable
fun TutorialOverlay(onDismiss: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    val steps = tutorialSteps
    val current = steps[step]

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
            color = Color.Black.copy(alpha = 0.85f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text("Skip", color = Color.White.copy(alpha = 0.7f))
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clickable(onClick = {
                            if (step < steps.size - 1) step++ else onDismiss()
                        }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text(
                        text = current.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Text(
                        text = current.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Text(
                        text = if (step < steps.size - 1) "Tap to continue" else "Got it!",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(steps.size) { index ->
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = if (index == step) MaterialTheme.colorScheme.primary
                                                else Color.White.copy(alpha = 0.3f),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
