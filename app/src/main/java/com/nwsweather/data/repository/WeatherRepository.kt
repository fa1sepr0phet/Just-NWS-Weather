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
import com.nwsweather.data.model.NwsObservationProperties
import com.nwsweather.data.network.NwsApi
import android.content.Context
import android.location.Geocoder
import android.os.Build
import androidx.glance.appwidget.updateAll
import com.nwsweather.location.DeviceLocationClient
import com.nwsweather.widget.WeatherAppWidget
import com.nwsweather.presentation.TemperatureUnit
import com.nwsweather.util.roundCoordinate
import com.nwsweather.util.NotificationHelper
import com.nwsweather.util.convertTemperature
import com.nwsweather.data.local.SettingsManager
import retrofit2.HttpException
import com.nwsweather.util.awaitGeocoding
import android.location.Address
import com.nwsweather.util.WeatherFreshness
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.withTimeoutOrNull
import com.nwsweather.util.mergeWidgetForecast

class WeatherRepository(
    private val nwsApi: NwsApi,
    private val epaUvRepository: EpaUvRepository,
    private val savedLocationDao: SavedLocationDao,
    private val pointCacheDao: PointCacheDao,
    private val weatherSnapshotDao: WeatherSnapshotDao,
    private val locationClient: DeviceLocationClient,
    private val settingsManager: SettingsManager,
    val appContext: Context
) {
    companion object {
        private const val POINT_CACHE_MAX_AGE_MS = 60 * 60 * 1000L
    }

    fun observeSavedLocations(): Flow<List<SavedLocationEntity>> = savedLocationDao.observeAll()

    suspend fun deleteSavedLocation(location: SavedLocationEntity) {
        savedLocationDao.deleteById(location.id)
    }

    suspend fun clearAllData() {
        savedLocationDao.deleteAll()
        pointCacheDao.deleteAll()
        weatherSnapshotDao.deleteAll()
        com.nwsweather.widget.ForecastGraphWorker.clearCache(appContext)
        NotificationHelper(appContext).cancelStatusBarTemperature()
        WeatherAppWidget().updateAll(appContext)
        com.nwsweather.widget.ForecastGraphWorker.enqueue(appContext)
    }

    suspend fun reorderSavedLocations(fromIndex: Int, toIndex: Int) {
        val locations = savedLocationDao.getAll().toMutableList()
        if (fromIndex !in locations.indices || toIndex !in locations.indices) return
        
        val movedItem = locations.removeAt(fromIndex)
        locations.add(toIndex, movedItem)
        
        val updated = locations.mapIndexed { index, entity ->
            entity.copy(displayOrder = index)
        }
        savedLocationDao.insertAll(updated)
    }

    suspend fun getLatestSnapshot(): WeatherSnapshotEntity? = weatherSnapshotDao.getLatest()

    suspend fun refreshLatestSnapshot(): ForecastLoadResult? {
        val snapshot = weatherSnapshotDao.getLatest()
        val saved = if (snapshot == null) savedLocationDao.getAll().firstOrNull() else null
        val latitude = snapshot?.latitude ?: saved?.latitude ?: return null
        val longitude = snapshot?.longitude ?: saved?.longitude ?: return null
        val point = getOrFetchPoint(latitude, longitude)
        // Status temperature must not depend on the daily forecast or alert service.
        val data = loadBackgroundWeather(
            fetchHourly = { nwsApi.getForecast(point.forecastHourlyUrl) },
            fetchObservation = { fetchLatestObservation(point) }
        )
        val currentResult = ForecastLoadResult(
            forecast = NwsForecastResponse(com.nwsweather.data.model.NwsForecastProperties()),
            hourlyForecast = data.hourlyForecast,
            observation = data.observation,
            locationName = snapshot?.locationName ?: saved!!.label.ifBlank { saved.address },
            latitude = latitude,
            longitude = longitude,
            source = ForecastSource.WidgetRefresh,
            timeZoneId = point.timeZone
        ).also { saveSnapshot(it) }
        // Publish current conditions first; daily forecast failures cannot block the temperature.
        val daily = withTimeoutOrNull(8_000L) {
            try {
                nwsApi.getForecast(point.forecastUrl)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        }
        return if (daily != null) currentResult.copy(forecast = daily).also { saveSnapshot(it) }
            else currentResult
    }

    suspend fun fetchAlerts(latitude: Double, longitude: Double): List<NwsAlertProperties> =
        nwsApi.getActiveAlerts("${latitude.roundCoordinate()},${longitude.roundCoordinate()}").features.map { it.properties }

    suspend fun loadForecastForAddress(address: String, label: String? = null, existingId: Long? = null): ForecastLoadResult = coroutineScope {
        val geocoder = Geocoder(appContext)
        val (latitude, longitude, matchedAddress) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            awaitGeocoding<Triple<Double, Double, String>> { complete ->
                geocoder.getFromLocationName(address, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        val addr = addresses.firstOrNull()
                        complete(if (addr != null) Result.success(Triple(
                            addr.latitude.roundCoordinate(),
                            addr.longitude.roundCoordinate(),
                            addr.getAddressLine(0) ?: address
                        )) else Result.failure(Exception("No location match found for '$address'.")))
                    }
                    override fun onError(errorMessage: String?) {
                        complete(Result.failure(Exception(errorMessage ?: "Address lookup failed. Please try again.")))
                    }
                })
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

        val roundedLatitude = latitude.roundCoordinate()
        val roundedLongitude = longitude.roundCoordinate()
        val (point, weatherData) = fetchWeatherDataForLocation(
            latitude = roundedLatitude,
            longitude = roundedLongitude
        )

        val displayName = buildDisplayName(
            preferredLabel = label,
            city = point.city,
            state = point.state,
            fallbackAddress = matchedAddress
        )

        if (!label.isNullOrBlank()) {
            val displayOrder = if (existingId != null) {
                savedLocationDao.getById(existingId)?.displayOrder ?: 0
            } else {
                (savedLocationDao.getMaxOrder() ?: 0) + 1
            }
            savedLocationDao.insert(
                SavedLocationEntity(
                    id = existingId ?: 0L,
                    label = label,
                    address = matchedAddress,
                    latitude = roundedLatitude,
                    longitude = roundedLongitude,
                    city = point.city,
                    state = point.state,
                    displayOrder = displayOrder
                )
            )
        }

        ForecastLoadResult(
            forecast = weatherData.forecast,
            hourlyForecast = weatherData.hourlyForecast,
            alerts = weatherData.alerts,
            locationName = displayName,
            latitude = roundedLatitude,
            longitude = roundedLongitude,
            source = ForecastSource.AddressSearch(matchedAddress),
            observation = weatherData.observation,
            uvForecastHours = weatherData.uvForecastHours,
            timeZoneId = point.timeZone
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
        val roundedLatitude = latitude.roundCoordinate()
        val roundedLongitude = longitude.roundCoordinate()
        val (point, weatherData) = fetchWeatherDataForLocation(
            latitude = roundedLatitude,
            longitude = roundedLongitude
        )

        val displayName = buildDisplayName(
            city = point.city,
            state = point.state,
            fallbackAddress = "$roundedLatitude, $roundedLongitude"
        )
        ForecastLoadResult(
            forecast = weatherData.forecast,
            hourlyForecast = weatherData.hourlyForecast,
            alerts = weatherData.alerts,
            locationName = displayName,
            latitude = roundedLatitude,
            longitude = roundedLongitude,
            source = source,
            observation = weatherData.observation,
            uvForecastHours = weatherData.uvForecastHours,
            timeZoneId = point.timeZone
        ).also { saveSnapshot(it) }
    }

    private suspend fun saveSnapshot(result: ForecastLoadResult) {
        val selected = weatherSnapshotDao.getLatest()
        if (result.source == ForecastSource.WidgetRefresh) {
            // Do not replace a location selected while this background request was in flight.
            if (selected != null && (selected.latitude != result.latitude || selected.longitude != result.longitude)) return
        }
        val current = result.currentPeriod
        val hourly = result.currentHourlyPeriod
        val obs = result.freshObservation
        val temperature = convertTemperature(result.currentTemperatureValue, result.currentTemperatureUnit, TemperatureUnit.FAHRENHEIT)
        val temperatureUnit = "F"

        val humidity = obs?.relativeHumidity?.value?.toInt() 
            ?: current?.relativeHumidity?.value?.toInt()
            ?: hourly?.relativeHumidity?.value?.toInt()

        val isDaytime = result.isDaytime
        val shortForecast = obs?.textDescription?.takeIf { it.isNotBlank() } 
            ?: (hourly?.shortForecast ?: current?.shortForecast).orEmpty().ifBlank { "Forecast unavailable" }

        val windSpeed = obs?.windSpeed?.value?.let { "${it.toInt()} km/h" }
            ?: (hourly?.windSpeed ?: current?.windSpeed).orEmpty().ifBlank { "--" }
        
        val windDirection = (hourly?.windDirection ?: current?.windDirection).orEmpty().ifBlank { "--" }

        val now = System.currentTimeMillis()
        val sameLocation = selected?.latitude == result.latitude && selected.longitude == result.longitude
        val cachedForecast = selected?.takeIf {
            sameLocation && it.forecastFetchedAtEpochMs?.let { time -> now - time in 0..86_400_000L } == true
        }
        val cachedPeriods = runCatching {
            Json.decodeFromString<List<NwsForecastPeriod>>(cachedForecast?.forecastPeriodsJson ?: "[]")
        }.getOrDefault(emptyList())
        val freshPeriods = result.forecast.properties.periods
        val widgetPeriods = if (freshPeriods.isEmpty()) cachedPeriods else
            mergeWidgetForecast(freshPeriods, cachedPeriods, result.timeZoneId, now)

        weatherSnapshotDao.upsert(
            WeatherSnapshotEntity(
                id = 0,
                locationName = result.locationName,
                latitude = result.latitude,
                longitude = result.longitude,
                temperature = temperature,
                temperatureUnit = temperatureUnit,
                shortForecast = "${if (obs != null) "Observed" else "Hourly forecast"}: $shortForecast",
                humidity = humidity,
                windSpeed = windSpeed,
                windDirection = windDirection,
                uvIndex = null, // UV is displayed from timestamped EPA hours, not this untimed snapshot field.
                updatedAtEpochMs = result.readingEpochMs,
                isDaytime = isDaytime,
                forecastPeriodsJson = Json.encodeToString(widgetPeriods),
                forecastTimeZone = result.timeZoneId,
                forecastFetchedAtEpochMs = if (freshPeriods.isNotEmpty()) now else cachedForecast?.forecastFetchedAtEpochMs
            )
        )

        if (settingsManager.statusBarTempEnabled.value) {
            NotificationHelper(appContext).updateStatusBarTemperature(
                temp = temperature,
                sourceUnit = temperatureUnit,
                targetUnit = settingsManager.unit.value,
                locationName = result.locationName,
                forecast = "${if (obs != null) "Observed" else "Hourly forecast"}: $shortForecast",
                isDaytime = isDaytime,
                readingEpochMs = result.readingEpochMs
            )
        } else {
            NotificationHelper(appContext).cancelStatusBarTemperature()
        }

        WeatherAppWidget().updateAll(appContext)
        com.nwsweather.widget.ForecastGraphWorker.enqueue(appContext)
    }

    private suspend fun getOrFetchPoint(
        latitude: Double,
        longitude: Double,
        forceRefresh: Boolean = false
    ): PointCacheEntity {
        val roundedLatitude = latitude.roundCoordinate()
        val roundedLongitude = longitude.roundCoordinate()
        val key = "$roundedLatitude,$roundedLongitude"
        val cached = pointCacheDao.get(key)
        if (!forceRefresh && cached != null) {
            val cacheAgeMs = System.currentTimeMillis() - cached.cachedAtEpochMs
            if (cacheAgeMs < POINT_CACHE_MAX_AGE_MS) {
                return cached
            }
        }

        val point = try {
            nwsApi.getPointMetadata(
                lat = roundedLatitude.toString(),
                lon = roundedLongitude.toString()
            )
        } catch (e: HttpException) {
            if (e.code() == 404) {
                throw Exception("Unable to retrieve location. The National Weather Service only provides data for the United States.")
            }
            if (cached != null) return cached
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (cached != null) return cached
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
            observationStations = point.properties.observationStations,
            timeZone = point.properties.timeZone,
            city = point.properties.relativeLocation?.properties?.city,
            state = point.properties.relativeLocation?.properties?.state,
            cachedAtEpochMs = System.currentTimeMillis()
        )
        pointCacheDao.insert(entity)
        return entity
    }

    private suspend fun fetchWeatherDataForLocation(
        latitude: Double,
        longitude: Double
    ): Pair<PointCacheEntity, NwsWeatherData> {
        val point = getOrFetchPoint(latitude, longitude)

        return try {
            point to fetchWeatherData(point, latitude, longitude)
        } catch (e: HttpException) {
            if (e.code() !in setOf(404, 410)) {
                throw e
            }

            val refreshedPoint = getOrFetchPoint(latitude, longitude, forceRefresh = true)
            refreshedPoint to fetchWeatherData(refreshedPoint, latitude, longitude)
        }
    }

    private suspend fun fetchWeatherData(
        point: PointCacheEntity,
        latitude: Double,
        longitude: Double
    ): NwsWeatherData = coroutineScope {
        val roundedLatitude = latitude.roundCoordinate()
        val roundedLongitude = longitude.roundCoordinate()

        val forecastDeferred = async { nwsApi.getForecast(point.forecastUrl) }
        val hourlyForecastDeferred = async {
            try {
                nwsApi.getForecast(point.forecastHourlyUrl)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        }
        val alertsDeferred = async {
            // A failed alert request must not look like a successful "no alerts" response.
            nwsApi.getActiveAlerts("$roundedLatitude,$roundedLongitude").features.map { it.properties }
        }
        val observationDeferred = async { fetchLatestObservation(point) }
        val uvDeferred = async { epaUvRepository.fetch(point.city, point.state, point.timeZone) }

        NwsWeatherData(
            forecast = forecastDeferred.await(),
            hourlyForecast = hourlyForecastDeferred.await(),
            alerts = alertsDeferred.await(),
            observation = observationDeferred.await(),
            uvForecastHours = uvDeferred.await()
        )
    }

    private suspend fun fetchLatestObservation(point: PointCacheEntity): NwsObservationProperties? {
        val stationsUrl = point.observationStations ?: return null

        return supervisorScope {
            val stations = try {
                nwsApi.getStations(stationsUrl)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                return@supervisorScope null
            }
            // Keep the search local and bounded; a nearby station may be offline or delayed.
            stations.features.take(3).map { station ->
                async {
                    try {
                        val id = station.properties.stationIdentifier
                        nwsApi.getLatestObservation("https://api.weather.gov/stations/$id/observations/latest").properties
                            .takeIf { it.temperature?.value?.isFinite() == true && WeatherFreshness.isRecent(it.timestamp) }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        null
                    }
                }
            }.mapNotNull { it.await() }
                .maxByOrNull { WeatherFreshness.epochMillis(it.timestamp) ?: Long.MIN_VALUE }
        }
    }
    private fun buildDisplayName(
        preferredLabel: String? = null,
        city: String? = null,
        state: String? = null,
        fallbackAddress: String
    ): String {
        // 1. Prioritize user-provided labels (e.g., "Home", "Work")
        if (!preferredLabel.isNullOrBlank()) return preferredLabel

        // 2. If the fallback address is a geocoded name (not raw coordinates), use it.
        // This ensures "Denver, CO" doesn't show up as "Glendale, CO".
        val isCoordinates = fallbackAddress.contains(",") &&
                fallbackAddress.split(",").all { it.trim().toDoubleOrNull() != null }

        if (!isCoordinates) {
            return fallbackAddress
                .removeSuffix(", USA")
                .removeSuffix(", United States")
                .trim()
        }

        // 3. Fall back to NWS city/state for raw coordinate lookups (like current location)
        return listOfNotNull(city, state).joinToString(", ").takeIf { it.isNotBlank() }
            ?: fallbackAddress
    }
}

data class ForecastLoadResult(
    val forecast: NwsForecastResponse,
    val hourlyForecast: NwsForecastResponse? = null,
    val alerts: List<NwsAlertProperties> = emptyList(),
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val source: ForecastSource,
    val observation: NwsObservationProperties? = null,
    val timeZoneId: String? = null,
    val uvForecastHours: List<com.nwsweather.data.model.UvForecastHour> = emptyList(),
    val fetchedAtEpochMs: Long = System.currentTimeMillis()
) {
    val freshObservation: NwsObservationProperties?
        get() = observation?.takeIf { WeatherFreshness.isRecent(it.timestamp, fetchedAtEpochMs) && it.temperature?.value?.isFinite() == true }

    val readingEpochMs: Long
        get() = WeatherFreshness.epochMillis(freshObservation?.timestamp) ?: fetchedAtEpochMs

    val currentPeriod: NwsForecastPeriod?
        get() = forecast.properties.periods.firstOrNull { WeatherFreshness.coversNow(it.startTime, it.endTime, fetchedAtEpochMs) }

    val currentHourlyPeriod: NwsForecastPeriod?
        get() = hourlyForecast?.properties?.periods?.firstOrNull { WeatherFreshness.coversNow(it.startTime, it.endTime, fetchedAtEpochMs) }

    val upcomingPeriods: List<NwsForecastPeriod>
        get() = forecast.properties.periods.filter { (WeatherFreshness.epochMillis(it.startTime) ?: 0L) > System.currentTimeMillis() }

    val currentTemperatureValue: Double
        get() = freshObservation?.temperature?.value ?: currentHourlyPeriod?.temperature?.toDouble() ?: error("No recent observation or current hourly forecast is available. Please try again.")

    val currentTemperatureUnit: String
        get() = when {
            freshObservation?.temperature?.value != null -> freshObservation?.temperature?.unitCode?.let { unitCode ->
                if (unitCode.contains("degF", ignoreCase = true)) "F" else "C"
            } ?: "C"
            else -> currentHourlyPeriod?.temperatureUnit ?: currentPeriod?.temperatureUnit ?: "F"
        }

    val currentShortForecast: String
        get() = freshObservation?.textDescription?.takeIf { it.isNotBlank() } ?: currentHourlyPeriod?.shortForecast ?: currentPeriod?.shortForecast ?: "Unknown"

    val isDaytime: Boolean
        get() = currentHourlyPeriod?.isDaytime ?: currentPeriod?.isDaytime ?: true

    val currentReadingTimestampLabel: String?
        get() = freshObservation?.timestamp?.let { timestamp ->
            formatReadingTimestampLabel(
                prefix = "Observed",
                timestamp = timestamp,
                timeZoneId = timeZoneId,
                stationId = freshObservation?.stationId
            )
        } ?: currentHourlyPeriod?.startTime?.let { startTime ->
            formatReadingTimestampLabel(
                prefix = "Forecast for",
                timestamp = startTime,
                timeZoneId = timeZoneId
            )
        } ?: forecast.properties.updated?.let { updated ->
            formatReadingTimestampLabel(
                prefix = "Forecast updated",
                timestamp = updated,
                timeZoneId = timeZoneId
            )
        }
}

sealed interface ForecastSource {
    data object CurrentLocation : ForecastSource
    data object Coordinates : ForecastSource
    data object WidgetRefresh : ForecastSource
    data class AddressSearch(val query: String) : ForecastSource
    data class SavedLocation(val label: String) : ForecastSource
}

private data class NwsWeatherData(
    val forecast: NwsForecastResponse,
    val hourlyForecast: NwsForecastResponse?,
    val alerts: List<NwsAlertProperties>,
    val observation: NwsObservationProperties?,
    val uvForecastHours: List<com.nwsweather.data.model.UvForecastHour>
)

private fun formatReadingTimestampLabel(
    prefix: String,
    timestamp: String,
    timeZoneId: String?,
    stationId: String? = null
): String? {
    val parsedTimestamp = runCatching { OffsetDateTime.parse(timestamp) }.getOrNull() ?: return null
    val zoneId = runCatching {
        timeZoneId?.takeIf { it.isNotBlank() }?.let(ZoneId::of) ?: ZoneId.systemDefault()
    }.getOrDefault(ZoneId.systemDefault())
    val formatter = DateTimeFormatter.ofPattern("MMM d, h:mm a z", Locale.getDefault())
    val formattedTime = parsedTimestamp.atZoneSameInstant(zoneId).format(formatter)

    return buildString {
        append(prefix)
        append(' ')
        append(formattedTime)
        stationId?.takeIf { it.isNotBlank() }?.let {
            append(" at ")
            append(it)
        }
    }
}
