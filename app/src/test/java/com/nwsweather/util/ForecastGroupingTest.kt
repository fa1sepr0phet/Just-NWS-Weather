package com.nwsweather.util

import com.nwsweather.data.model.NwsForecastPeriod
import org.junit.Assert.assertEquals
import org.junit.Test

class ForecastGroupingTest {
    private fun period(name: String, day: Boolean, start: String, end: String) =
        NwsForecastPeriod(1, name, start, end, day, 40, "F")
    private val holiday = period("Veterans Day", true, "2026-11-11T06:00:00-06:00", "2026-11-11T18:00:00-06:00")
    private val night = period("Wednesday Night", false, "2026-11-11T18:00:00-06:00", "2026-11-12T06:00:00-06:00")

    @Test fun holidayPairsWithFollowingNight() {
        assertEquals(listOf(listOf(holiday, night)), groupForecastPeriods(listOf(holiday, night)))
    }
    @Test fun standaloneNightKeepsItsName() {
        val groups = groupForecastPeriods(listOf(night))
        assertEquals("Wednesday Night", groups.single().single().name)
        assertEquals(false, groups.single().single().isDaytime)
    }
    @Test fun tonightDoesNotPairWithFollowingDay() {
        val tonight = period("Tonight", false, "2026-11-10T18:00:00-06:00", holiday.startTime)
        assertEquals(listOf(listOf(tonight), listOf(holiday, night)), groupForecastPeriods(listOf(tonight, holiday, night)))
    }
    @Test fun missingNightDoesNotPairWithLaterNight() {
        val later = night.copy(startTime = "2026-11-12T18:00:00-06:00")
        assertEquals(2, groupForecastPeriods(listOf(holiday, later)).size)
    }
    @Test fun equivalentOffsetsPairByInstant() {
        assertEquals(1, groupForecastPeriods(listOf(holiday, night.copy(startTime = "2026-11-12T00:00:00Z"))).size)
    }
    @Test fun malformedTimestampsRemainSeparate() {
        assertEquals(2, groupForecastPeriods(listOf(holiday.copy(endTime = "invalid"), night)).size)
    }
}
