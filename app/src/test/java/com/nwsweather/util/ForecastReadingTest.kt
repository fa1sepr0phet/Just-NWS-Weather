package com.nwsweather.util

import com.nwsweather.data.model.*
import com.nwsweather.data.repository.*
import org.junit.Assert.*
import org.junit.Test

class ForecastReadingTest {
    private val now = WeatherFreshness.epochMillis("2026-10-17T12:15:00Z")!!
    private fun period(start: String, end: String, temp: Int) = NwsForecastPeriod(
        1, "Today", start, end, true, temp, "F"
    )
    private val current = period("2026-10-17T12:00:00Z", "2026-10-17T13:00:00Z", 65)
    private fun result(observation: NwsObservationProperties?, hourly: List<NwsForecastPeriod> = listOf(current)) = ForecastLoadResult(
        forecast = NwsForecastResponse(NwsForecastProperties(periods = listOf(current))),
        hourlyForecast = NwsForecastResponse(NwsForecastProperties(periods = hourly)),
        locationName = "Test", latitude = 0.0, longitude = 0.0,
        source = ForecastSource.Coordinates, observation = observation, fetchedAtEpochMs = now
    )

    @Test fun staleObservationFallsBackToCurrentHourlyPeriod() {
        val stale = NwsObservationProperties(timestamp = "2026-10-17T10:00:00Z", temperature = NwsObservationValue(99.0, "wmoUnit:degC"))
        val expired = period("2026-10-17T11:00:00Z", "2026-10-17T12:00:00Z", 20)
        val reading = result(stale, listOf(expired, current))
        assertEquals(65.0, reading.currentTemperatureValue, 0.0)
        assertEquals("F", reading.currentTemperatureUnit)
        assertNull(reading.freshObservation)
        assertTrue(reading.currentReadingTimestampLabel!!.startsWith("Forecast for"))
    }

    @Test fun recentObservationKeepsItsSourceTimeAndUnit() {
        val reading = result(NwsObservationProperties(timestamp = "2026-10-17T12:10:00Z", temperature = NwsObservationValue(20.0, "wmoUnit:degC")))
        assertEquals(20.0, reading.currentTemperatureValue, 0.0)
        assertEquals("C", reading.currentTemperatureUnit)
        assertEquals(WeatherFreshness.epochMillis("2026-10-17T12:10:00Z"), reading.readingEpochMs)
    }

    @Test(expected = IllegalStateException::class) fun dailyHighMustNotBecomeCurrentTemperature() {
        result(null, emptyList()).currentTemperatureValue
    }
}
