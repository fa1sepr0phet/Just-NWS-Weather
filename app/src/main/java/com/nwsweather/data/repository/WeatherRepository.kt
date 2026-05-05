package com.nwsweather.data.repository

import com.nwsweather.data.local.PointCacheDao
import com.nwsweather.data.local.PointCacheEntity
import com.nwsweather.data.local.SavedLocationDao
import com.nwsweather.data.local.SavedLocationEntity
import com.nwsweather.data.local.WeatherSnapshotDao
import com.nwsweather.data.local.WeatherSnapshotEntity
import com.nwsweather.data.model.NwsAlertProperties
import com.nwsweather.data.model.NwsForecastPeriod
import com.nwsweather.data.model.NwsForecastResponse
import com.nwsweather.data.network.NwsApi
import android.content.Context
import android.location.Geocoder
import android.os.Build
import androidx.glance.appwidget.updateAll
import com.nwsweather.location.DeviceLocationClient
import com.nwsweather.widget.WeatherAppWidget
import com.nwsweather.util.roundCoordinate
import retrofit2.HttpException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow

class WeatherRepository(
    private val nwsApi: NwsApi,
    private val savedLocationDao: SavedLocationDao,
    private val pointCacheDao: PointCacheDao,
    private val weatherSnapshotDao: WeatherSnapshotDao,
    private val locationClient: DeviceLocationClient,
    private val appContext: Context
) {
    fun observeSavedLocations(): Flow<List<SavedLocationEntity>> = savedLocationDao.observeAll()

    suspend fun deleteSavedLocation(location: SavedLocationEntity) {
        savedLocationDao.deleteById(location.id)
    }

    suspend fun getLatestSnapshot(): WeatherSnapshotEntity? = weatherSnapshotDao.getLatest()

    suspend fun refreshLatestSnapshot(): ForecastLoadResult? {
        val snapshot = weatherSnapshotDao.getLatest() ?: return null
        return loadForecastForCoordinates(
            latitude = snapshot.latitude,
            longitude = snapshot.longitude,
            source = ForecastSource.WidgetRefresh
        )
    }

    suspend fun loadForecastForAddress(address: String, label: String? = null, existingId: Long? = null): ForecastLoadResult = coroutineScope {
        val geocoder = Geocoder(appContext)
        val (latitude, longitude, matchedAddress) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCoroutine { continuation ->
                geocoder.getFromLocationName(address, 1) { addresses ->
                    val addr = addresses.firstOrNull()
                    if (addr != null) {
                        continuation.resume(
                            Triple(
                                addr.latitude.roundCoordinate(),
                                addr.longitude.roundCoordinate(),
                                addr.getAddressLine(0) ?: address
                            )
                        )
                    } else {
                        continuation.resumeWithException(Exception("No location match found for '$address'."))
                    }
                }
            }
        } else {
            @Suppress("DEPRECATION")
            val addr = geocoder.getFromLocationName(address, 1)?.firstOrNull()
                ?: error("No location match found for '$address'.")
            Triple(
                addr.latitude.roundCoordinate(),
                addr.longitude.roundCoordinate(),
                addr.getAddressLine(0) ?: address
            )
        }

        val point = getOrFetchPoint(latitude, longitude)

        val forecastDeferred = async { nwsApi.getForecast(point.forecastUrl) }
        val alertsDeferred = async {
            try {
                nwsApi.getActiveAlerts("$latitude,$longitude").features.map { it.properties }
            } catch (e: Exception) {
                emptyList<NwsAlertProperties>()
            }
        }

        val forecast = forecastDeferred.await()
        val alerts = alertsDeferred.await()

        val displayName = buildDisplayName(
            preferredLabel = label,
            city = point.city,
            state = point.state,
            fallbackAddress = matchedAddress
        )

        if (!label.isNullOrBlank()) {
            savedLocationDao.insert(
                SavedLocationEntity(
                    id = existingId ?: 0L,
                    label = label,
                    address = matchedAddress,
                    latitude = latitude,
                    longitude = longitude,
                    city = point.city,
                    state = point.state
                )
            )
        }

        ForecastLoadResult(
            forecast = forecast,
            alerts = alerts,
            locationName = displayName,
            latitude = latitude,
            longitude = longitude,
            source = ForecastSource.AddressSearch(matchedAddress)
        ).also { saveSnapshot(it) }
    }

    suspend fun loadForecastForSavedLocation(location: SavedLocationEntity): ForecastLoadResult {
        val forecast = loadForecastForCoordinates(location.latitude, location.longitude)
        val displayName = location.label.ifBlank {
            listOfNotNull(location.city, location.state).joinToString(", ")
                .ifBlank { location.address }
        }
        return forecast.copy(
            locationName = displayName,
            source = ForecastSource.SavedLocation(location.label)
        ).also { saveSnapshot(it) }
    }

    suspend fun loadForecastForCurrentLocation(): ForecastLoadResult {
        val coordinates = locationClient.getCurrentLocation()
            ?: error("Could not get the current device location. Make sure location is on and try again.")

        return loadForecastForCoordinates(
            latitude = coordinates.latitude,
            longitude = coordinates.longitude,
            source = ForecastSource.CurrentLocation
        )
    }

    suspend fun loadForecastForCoordinates(
        latitude: Double,
        longitude: Double,
        source: ForecastSource = ForecastSource.Coordinates
    ): ForecastLoadResult = coroutineScope {
        val point = getOrFetchPoint(latitude.roundCoordinate(), longitude.roundCoordinate())

        val forecastDeferred = async { nwsApi.getForecast(point.forecastUrl) }
        val alertsDeferred = async {
            try {
                nwsApi.getActiveAlerts("${latitude.roundCoordinate()},${longitude.roundCoordinate()}").features.map { it.properties }
            } catch (e: Exception) {
                emptyList<NwsAlertProperties>()
            }
        }

        val forecast = forecastDeferred.await()
        val alerts = alertsDeferred.await()

        val displayName = buildDisplayName(
            city = point.city,
            state = point.state,
            fallbackAddress = "${latitude.roundCoordinate()}, ${longitude.roundCoordinate()}"
        )
        ForecastLoadResult(
            forecast = forecast,
            alerts = alerts,
            locationName = displayName,
            latitude = latitude.roundCoordinate(),
            longitude = longitude.roundCoordinate(),
            source = source
        ).also { saveSnapshot(it) }
    }

    private suspend fun saveSnapshot(result: ForecastLoadResult) {
        val current = result.currentPeriod ?: return
        weatherSnapshotDao.upsert(
            WeatherSnapshotEntity(
                id = 0,
                locationName = result.locationName,
                latitude = result.latitude,
                longitude = result.longitude,
                temperature = current.temperature,
                temperatureUnit = current.temperatureUnit,
                shortForecast = current.shortForecast.orEmpty().ifBlank { "Forecast unavailable" },
                windSpeed = current.windSpeed.orEmpty().ifBlank { "--" },
                windDirection = current.windDirection.orEmpty().ifBlank { "--" },
                updatedAtEpochMs = System.currentTimeMillis(),
                isDaytime = current.isDaytime
            )
        )
        WeatherAppWidget().updateAll(appContext)
    }

    private suspend fun getOrFetchPoint(latitude: Double, longitude: Double): PointCacheEntity {
        val roundedLatitude = latitude.roundCoordinate()
        val roundedLongitude = longitude.roundCoordinate()
        val key = "$roundedLatitude,$roundedLongitude"
        val cached = pointCacheDao.get(key)
        if (cached != null) return cached

        val point = try {
            nwsApi.getPointMetadata(
                lat = roundedLatitude.toString(),
                lon = roundedLongitude.toString()
            )
        } catch (e: HttpException) {
            if (e.code() == 404) {
                throw Exception("Unable to retrieve location. The National Weather Service only provides data for the United States.")
            }
            throw e
        }

        val entity = PointCacheEntity(
            key = key,
            gridId = point.properties.gridId,
            gridX = point.properties.gridX,
            gridY = point.properties.gridY,
            forecastUrl = point.properties.forecast,
            forecastHourlyUrl = point.properties.forecastHourly,
            forecastGridDataUrl = point.properties.forecastGridData,
            timeZone = point.properties.timeZone,
            city = point.properties.relativeLocation?.properties?.city,
            state = point.properties.relativeLocation?.properties?.state,
            cachedAtEpochMs = System.currentTimeMillis()
        )
        pointCacheDao.insert(entity)
        return entity
    }

    private fun buildDisplayName(
        preferredLabel: String? = null,
        city: String? = null,
        state: String? = null,
        fallbackAddress: String
    ): String {
        return preferredLabel?.takeIf { it.isNotBlank() }
            ?: listOfNotNull(city, state).joinToString(", ").takeIf { it.isNotBlank() }
            ?: fallbackAddress
    }
}

data class ForecastLoadResult(
    val forecast: NwsForecastResponse,
    val alerts: List<NwsAlertProperties> = emptyList(),
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val source: ForecastSource
) {
    val currentPeriod: NwsForecastPeriod?
        get() = forecast.properties.periods.firstOrNull()

    val upcomingPeriods: List<NwsForecastPeriod>
        get() = forecast.properties.periods.drop(1)
}

sealed interface ForecastSource {
    data object CurrentLocation : ForecastSource
    data object Coordinates : ForecastSource
    data object WidgetRefresh : ForecastSource
    data class AddressSearch(val query: String) : ForecastSource
    data class SavedLocation(val label: String) : ForecastSource
}
