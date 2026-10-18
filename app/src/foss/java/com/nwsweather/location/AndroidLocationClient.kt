package com.nwsweather.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import android.os.SystemClock
import kotlinx.coroutines.withTimeoutOrNull

class AndroidLocationClient(
    private val context: Context
) : DeviceLocationClient {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): DeviceCoordinates? = withTimeoutOrNull(20_000L) { readLocation() }

    @SuppressLint("MissingPermission")
    private suspend fun readLocation(): DeviceCoordinates? {
        val lastKnown = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

        if (lastKnown != null && SystemClock.elapsedRealtimeNanos() - lastKnown.elapsedRealtimeNanos in 0..120_000_000_000L) {
            return DeviceCoordinates(lastKnown.latitude, lastKnown.longitude)
        }

        return suspendCancellableCoroutine { continuation ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    locationManager.removeUpdates(this)
                    continuation.resume(DeviceCoordinates(location.latitude, location.longitude))
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            try {
                val provider = if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    LocationManager.NETWORK_PROVIDER
                } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    LocationManager.GPS_PROVIDER
                } else {
                    null
                }

                if (provider != null) {
                    locationManager.requestLocationUpdates(
                        provider,
                        0L,
                        0f,
                        listener,
                        Looper.getMainLooper()
                    )
                } else {
                    continuation.resume(null)
                }
            } catch (e: Exception) {
                continuation.resume(null)
            }

            continuation.invokeOnCancellation {
                locationManager.removeUpdates(listener)
            }
        }
    }
}
