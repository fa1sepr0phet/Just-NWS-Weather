package com.nwsweather.util

import com.nwsweather.data.model.*
import com.nwsweather.data.repository.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.async
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class BackgroundWeatherTest {
    private val now = WeatherFreshness.epochMillis("2026-10-22T12:15:00Z")!!
    private val observation = NwsObservationProperties(
        timestamp = "2026-10-22T12:10:00Z",
        temperature = NwsObservationValue(20.0, "wmoUnit:degC")
    )
    private val hourly = NwsForecastResponse(NwsForecastProperties(periods = listOf(
        NwsForecastPeriod(1, "", "2026-10-22T12:00:00Z", "2026-10-22T13:00:00Z", true, 67, "F")
    )))

    private fun reading(data: BackgroundWeather) = ForecastLoadResult(
        forecast = NwsForecastResponse(NwsForecastProperties()),
        hourlyForecast = data.hourlyForecast,
        observation = data.observation,
        locationName = "Home", latitude = 40.0, longitude = -90.0,
        source = ForecastSource.WidgetRefresh, fetchedAtEpochMs = now
    )

    @Test fun observationUpdatesWithoutDailyOrHourlyForecast() = runBlocking {
        val result = reading(loadBackgroundWeather({ throw IOException("hourly unavailable") }, { observation }))
        assertNull(result.currentPeriod)
        assertEquals(20.0, result.currentTemperatureValue, 0.0)
        assertEquals("C", result.currentTemperatureUnit)
        assertEquals(WeatherFreshness.epochMillis(observation.timestamp), result.readingEpochMs)
    }

    @Test fun hourlyUpdatesWithoutDailyForecastOrObservation() = runBlocking {
        val result = reading(loadBackgroundWeather({ hourly }, { throw IOException("station unavailable") }))
        assertEquals(67.0, result.currentTemperatureValue, 0.0)
        assertEquals("F", result.currentTemperatureUnit)
    }

    @Test(expected = IllegalStateException::class)
    fun failedSourcesCannotPublishFabricatedTemperature(): Unit = runBlocking {
        reading(loadBackgroundWeather({ throw IOException() }, { throw IOException() })).currentTemperatureValue
        Unit
    }

    @Test(expected = IllegalStateException::class)
    fun staleObservationCannotReplaceLastReading(): Unit = runBlocking {
        reading(loadBackgroundWeather({ throw IOException() }, {
            observation.copy(timestamp = "2026-10-21T12:10:00Z")
        })).currentTemperatureValue
        Unit
    }

    @Test(expected = CancellationException::class)
    fun cancellationStopsBackgroundRequest(): Unit = runBlocking {
        loadBackgroundWeather({ throw CancellationException("worker stopped") }, { observation })
        Unit
    }

    @Test fun endingIdleWindowCancelsBothInFlightRequests() = runBlocking {
        val hourlyStarted = CompletableDeferred<Unit>()
        val observationStarted = CompletableDeferred<Unit>()
        var hourlyCancelled = false
        var observationCancelled = false
        val request = async {
            loadBackgroundWeather(
                {
                    hourlyStarted.complete(Unit)
                    try { awaitCancellation() } finally { hourlyCancelled = true }
                },
                {
                    observationStarted.complete(Unit)
                    try { awaitCancellation() } finally { observationCancelled = true }
                }
            )
        }
        hourlyStarted.await()
        observationStarted.await()
        request.cancel()
        request.join()
        assertTrue(hourlyCancelled)
        assertTrue(observationCancelled)
    }
}
