package com.nwsweather.util

import com.nwsweather.data.model.NwsForecastPeriod
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.LocalDate

data class WidgetDailyForecast(
    val date: LocalDate,
    val day: NwsForecastPeriod?,
    val night: NwsForecastPeriod?
)

fun widgetDailyForecast(periods: List<NwsForecastPeriod>, zoneId: String?, now: Long): WidgetDailyForecast? {
    val zone = runCatching { ZoneId.of(zoneId) }.getOrNull() ?: return null
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val todayPeriods = periods.filter {
        runCatching { OffsetDateTime.parse(it.startTime).atZoneSameInstant(zone).toLocalDate() }.getOrNull() == today
    }
    val day = todayPeriods.firstOrNull { it.isDaytime }
    val night = todayPeriods.firstOrNull { !it.isDaytime }
    return if (day == null && night == null) null else WidgetDailyForecast(today, day, night)
}

/** Keep today's earlier forecast when NWS drops that period after sunset. */
fun mergeWidgetForecast(
    fresh: List<NwsForecastPeriod>, cached: List<NwsForecastPeriod>, zoneId: String?, now: Long
): List<NwsForecastPeriod> {
    val earlierDay = widgetDailyForecast(cached, zoneId, now)?.day
    return (fresh + listOfNotNull(earlierDay)).distinctBy { it.startTime }
}
