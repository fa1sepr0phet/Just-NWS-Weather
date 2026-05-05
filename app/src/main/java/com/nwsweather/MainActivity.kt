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
import com.nwsweather.sensor.MovementTracker
import com.nwsweather.ui.screens.WeatherRoute
import com.nwsweather.ui.theme.NwsWeatherTheme

class MainActivity : ComponentActivity() {
    private val appContainer by lazy { AppContainer(this) }
    private val movementTracker by lazy { MovementTracker(this) }
    private val viewModel: WeatherViewModel by viewModels {
        WeatherViewModelFactory(appContainer.weatherRepository, appContainer.settingsManager)
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
            // Just request, we check at runtime in NotificationHelper too
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
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

    override fun onStart() {
        super.onStart()
        movementTracker.start()
        requestCurrentLocation()
    }

    override fun onStop() {
        super.onStop()
        movementTracker.stop()
    }

    private fun requestCurrentLocation() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            viewModel.fetchCurrentLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
}
