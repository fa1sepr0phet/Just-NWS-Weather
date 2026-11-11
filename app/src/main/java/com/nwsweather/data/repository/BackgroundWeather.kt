package com.nwsweather.data.repository

import com.nwsweather.data.model.NwsForecastResponse
import com.nwsweather.data.model.NwsObservationProperties
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

internal data class BackgroundWeather(
    val hourlyForecast: NwsForecastResponse?,
    val observation: NwsObservationProperties?
)

// Either source can supply the temperature. Validation of timestamps and missing
// readings stays in ForecastLoadResult, so a failed fetch never invents a reading.
internal suspend fun loadBackgroundWeather(
    fetchHourly: suspend () -> NwsForecastResponse,
    fetchObservation: suspend () -> NwsObservationProperties?
): BackgroundWeather = coroutineScope {
    val hourly = async { optionalWeatherRequest(fetchHourly) }
    val observation = async { optionalWeatherRequest(fetchObservation) }
    BackgroundWeather(hourly.await(), observation.await())
}

private suspend fun <T> optionalWeatherRequest(fetch: suspend () -> T): T? = try {
    fetch()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    null
}
