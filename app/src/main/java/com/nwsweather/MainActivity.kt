package com.nwsweather

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nwsweather.di.AppContainer
import com.nwsweather.presentation.WeatherViewModel
import com.nwsweather.presentation.WeatherViewModelFactory
import com.nwsweather.ui.screens.WeatherRoute
import com.nwsweather.ui.theme.NwsWeatherTheme

class MainActivity : ComponentActivity() {
    private val appContainer by lazy { (application as NwsWeatherApp).container }
    private val viewModel: WeatherViewModel by viewModels {
        WeatherViewModelFactory(
            appContainer.weatherRepository,
            appContainer.cityRepository,
            appContainer.settingsManager
        )
    }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (granted) {
                viewModel.fetchCurrentLocation()
            } else {
                viewModel.onLocationPermissionDenied()
            }
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            viewModel.refreshStatusBarTemp()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Only on a fresh launch. Coming back to the app must not replace the place being viewed.
        if (savedInstanceState == null) {
            loadCurrentLocationOnLaunch()
        }

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            NwsWeatherTheme(appTheme = uiState.theme) {
                WeatherRoute(
                    viewModel = viewModel,
                    onRequestCurrentLocation = { requestCurrentLocation() }
                )
            }
        }
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

    private fun launchLocationPermissionRequest() {
        appContainer.settingsManager.markLocationPermissionRequested()
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    /** Fresh launch: use the device location if allowed, and ask for permission only the first time. */
    private fun loadCurrentLocationOnLaunch() {
        when {
            hasLocationPermission() -> viewModel.fetchCurrentLocation()
            !appContainer.settingsManager.hasRequestedLocationPermission() -> launchLocationPermissionRequest()
            // Otherwise the view model restores the last viewed place.
        }
    }

    /** The "Use current location" button: the user asked, so it is fine to ask again. */
    private fun requestCurrentLocation() {
        if (hasLocationPermission()) {
            viewModel.fetchCurrentLocation()
        } else {
            launchLocationPermissionRequest()
        }
    }
}
