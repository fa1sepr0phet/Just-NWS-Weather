package com.nwsweather.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.ResolverStyle
import java.util.Locale

@Serializable
data class EpaUvHour(
    @SerialName("CITY") val city: String? = null,
    @SerialName("STATE") val state: String? = null,
    @SerialName("DATE_TIME") val dateTime: String? = null,
    @SerialName("UV_VALUE") val value: Int? = null
)

data class UvForecastHour(val value: Int, val startEpochMs: Long, val city: String, val state: String) {
    fun covers(nowEpochMs: Long): Boolean = nowEpochMs >= startEpochMs && nowEpochMs < startEpochMs + 3_600_000L

    fun label(timeZoneId: String): String = "EPA UV forecast · $city, $state · " +
        Instant.ofEpochMilli(startEpochMs).atZone(ZoneId.of(timeZoneId))
            .format(DateTimeFormatter.ofPattern("MMM d, h a z", Locale.getDefault()))
}

private val epaTimeFormat = DateTimeFormatterBuilder().parseCaseInsensitive()
    .appendPattern("MMM/dd/uuuu hh a").toFormatter(Locale.US).withResolverStyle(ResolverStyle.STRICT)

fun parseEpaUvHours(rows: List<EpaUvHour>, city: String, state: String, timeZoneId: String): List<UvForecastHour> {
    val zone = runCatching { ZoneId.of(timeZoneId) }.getOrNull() ?: return emptyList()
    return rows.mapNotNull { row ->
        val value = row.value?.takeIf { it >= 0 } ?: return@mapNotNull null
        if (!city.equals(row.city?.trim(), ignoreCase = true) ||
            !state.equals(row.state?.trim(), ignoreCase = true)) return@mapNotNull null
        val local = runCatching { LocalDateTime.parse(row.dateTime, epaTimeFormat) }.getOrNull()
            ?: return@mapNotNull null
        // EPA supplies local wall time without an offset. Do not guess during DST overlaps/gaps.
        val offset = zone.rules.getValidOffsets(local).singleOrNull() ?: return@mapNotNull null
        UvForecastHour(value, local.toInstant(offset).toEpochMilli(), city, state)
    }.sortedBy { it.startEpochMs }
}
