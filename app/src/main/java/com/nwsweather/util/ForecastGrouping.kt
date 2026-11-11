package com.nwsweather.util

import com.nwsweather.data.model.NwsForecastPeriod
import java.time.OffsetDateTime

/** Pair a daytime period with its contiguous overnight period, regardless of holiday names. */
fun groupForecastPeriods(periods: List<NwsForecastPeriod>): List<List<NwsForecastPeriod>> {
    val groups = mutableListOf<MutableList<NwsForecastPeriod>>()
    for (period in periods) {
        val previous = groups.lastOrNull()?.singleOrNull()
        val followsDay = previous != null && previous.isDaytime && !period.isDaytime &&
            runCatching {
                OffsetDateTime.parse(previous.endTime).toInstant() ==
                    OffsetDateTime.parse(period.startTime).toInstant()
            }.getOrDefault(false)
        if (followsDay) groups.last().add(period)
        else groups.add(mutableListOf(period))
    }
    return groups
}
