package com.nwsweather.util

import org.junit.Assert.*
import org.junit.Test

class WeatherFreshnessTest {
    private val now = WeatherFreshness.epochMillis("2026-10-17T12:00:00Z")!!

    @Test fun rejectsMissingMalformedFutureAndStaleReadings() {
        assertFalse(WeatherFreshness.isRecent(null, now))
        assertFalse(WeatherFreshness.isRecent("invalid", now))
        assertFalse(WeatherFreshness.isRecent("2026-10-17T12:00:01Z", now))
        assertFalse(WeatherFreshness.isRecent("2026-10-17T11:29:59Z", now))
        assertTrue(WeatherFreshness.isRecent("2026-10-17T11:30:00Z", now))
        assertTrue(WeatherFreshness.isRecent("2026-10-17T07:59:00-04:00", now))
    }

    @Test fun forecastMustCoverNowWithExclusiveEnd() {
        assertTrue(WeatherFreshness.coversNow("2026-10-17T12:00:00Z", "2026-10-17T13:00:00Z", now))
        assertFalse(WeatherFreshness.coversNow("2026-10-17T11:00:00Z", "2026-10-17T12:00:00Z", now))
        assertFalse(WeatherFreshness.coversNow("2026-10-17T13:00:00Z", "2026-10-17T14:00:00Z", now))
        assertFalse(WeatherFreshness.coversNow("invalid", "2026-10-17T14:00:00Z", now))
    }
}
