package com.nwsweather.util

import com.nwsweather.data.model.NwsForecastPeriod
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class WidgetDailyForecastTest {
    private val now = Instant.parse("2026-11-11T17:00:00Z").toEpochMilli()
    private val zone = "America/Chicago"
    private val day = NwsForecastPeriod(1, "Veterans Day", "2026-11-11T06:00:00-06:00", "2026-11-11T18:00:00-06:00", true, 69, "F")
    private val night = day.copy(number = 2, name = "Wednesday Night", startTime = day.endTime, endTime = "2026-11-12T06:00:00-06:00", isDaytime = false, temperature = 40)
    private val tomorrow = day.copy(startTime = night.endTime, endTime = "2026-11-12T18:00:00-06:00", temperature = 80)

    @Test fun holidayHighAndOvernightLowUseDates() {
        val forecast = widgetDailyForecast(listOf(day, night, tomorrow), zone, now)!!
        assertEquals(69, forecast.day!!.temperature)
        assertEquals(40, forecast.night!!.temperature)
        assertEquals("2026-11-11", forecast.date.toString())
    }
    @Test fun eveningRetainsEarlierHighWhileUpdatingNight() {
        val updatedNight = night.copy(temperature = 39)
        val evening = Instant.parse("2026-11-12T02:00:00Z").toEpochMilli()
        val merged = mergeWidgetForecast(listOf(updatedNight, tomorrow), listOf(day, night), zone, evening)
        val forecast = widgetDailyForecast(merged, zone, evening)!!
        assertEquals(day, forecast.day)
        assertEquals(39, forecast.night!!.temperature)
    }
    @Test fun freshHighReplacesCachedHigh() {
        val merged = mergeWidgetForecast(listOf(day.copy(temperature = 70), night), listOf(day), zone, now)
        assertEquals(70, widgetDailyForecast(merged, zone, now)!!.day!!.temperature)
    }
    @Test fun nightOnlyDoesNotInventHighFromTomorrow() {
        val forecast = widgetDailyForecast(listOf(night, tomorrow), zone, now)!!
        assertNull(forecast.day)
        assertEquals(night, forecast.night)
    }
    @Test fun midnightChangesDateInLocationTimezone() {
        val nextDay = Instant.parse("2026-11-12T06:01:00Z").toEpochMilli()
        val forecast = widgetDailyForecast(listOf(day, night, tomorrow), zone, nextDay)!!
        assertEquals(tomorrow, forecast.day)
        assertNull(forecast.night)
        assertNull(widgetDailyForecast(listOf(day, night), zone, nextDay))
    }
    @Test fun oldDayIsNotRetainedAndMalformedDatesAreIgnored() {
        val nextDay = Instant.parse("2026-11-12T18:00:00Z").toEpochMilli()
        assertEquals(listOf(tomorrow), mergeWidgetForecast(listOf(tomorrow), listOf(day), zone, nextDay))
        assertNull(widgetDailyForecast(listOf(day.copy(startTime = "invalid")), zone, now))
        assertNull(widgetDailyForecast(listOf(day), null, now))
    }
}
