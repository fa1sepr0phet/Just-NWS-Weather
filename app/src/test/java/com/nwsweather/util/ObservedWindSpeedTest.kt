package com.nwsweather.util

import com.nwsweather.data.repository.observedWindSpeedMph
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObservedWindSpeedTest {
    @Test
    fun convertsKilometresPerHourToMph() {
        assertEquals("10 mph", observedWindSpeedMph(16.09, "wmoUnit:km_h-1"))
    }

    @Test
    fun convertsMetresPerSecondToMph() {
        assertEquals("22 mph", observedWindSpeedMph(10.0, "wmoUnit:m_s-1"))
    }

    @Test
    fun unknownOrMissingReadingsFallBackToTheForecast() {
        assertNull(observedWindSpeedMph(null, "wmoUnit:km_h-1"))
        assertNull(observedWindSpeedMph(12.0, null))
        assertNull(observedWindSpeedMph(12.0, "wmoUnit:knots"))
        assertNull(observedWindSpeedMph(Double.NaN, "wmoUnit:km_h-1"))
    }
}
