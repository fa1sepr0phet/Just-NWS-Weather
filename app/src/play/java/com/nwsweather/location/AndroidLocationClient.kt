package com.nwsweather.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import android.os.SystemClock
import kotlinx.coroutines.withTimeoutOrNull

class AndroidLocationClient(
    context: Context
) : DeviceLocationClient {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): DeviceCoordinates? = withTimeoutOrNull(20_000L) { readLocation() }

    @SuppressLint("MissingPermission")
    private suspend fun readLocation(): DeviceCoordinates? {
        val currentLocation = suspendCancellableCoroutine<DeviceCoordinates?> { continuation ->
            val tokenSource = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                tokenSource.token
            ).addOnSuccessListener { location ->
                continuation.resume(
                    location?.takeIf { SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos in 0..120_000_000_000L }?.let { DeviceCoordinates(it.latitude, it.longitude) }
                )
            }.addOnFailureListener {
                continuation.resume(null)
            }
            continuation.invokeOnCancellation { tokenSource.cancel() }
        }

        if (currentLocation != null) return currentLocation

        return suspendCancellableCoroutine { continuation ->
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    continuation.resume(
                        location?.takeIf { SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos in 0..120_000_000_000L }?.let { DeviceCoordinates(it.latitude, it.longitude) }
                    )
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        }
    }
}
