package com.nwsweather.data.model

import kotlinx.serialization.Serializable

@Serializable
data class NwsPointsResponse(
    val properties: NwsPointProperties
)

@Serializable
data class NwsPointProperties(
    val cwa: String? = null,
    val gridId: String,
    val gridX: Int,
    val gridY: Int,
    val forecast: String,
    val forecastHourly: String,
    val forecastGridData: String,
    val timeZone: String? = null,
    val relativeLocation: RelativeLocation? = null
)

@Serializable
data class RelativeLocation(
    val properties: RelativeLocationProperties? = null
)

@Serializable
data class RelativeLocationProperties(
    val city: String? = null,
    val state: String? = null
)

@Serializable
data class NwsForecastResponse(
    val properties: NwsForecastProperties
)

@Serializable
data class NwsForecastProperties(
    val updated: String? = null,
    val periods: List<NwsForecastPeriod> = emptyList()
)

@Serializable
data class NwsForecastPeriod(
    val number: Int,
    val name: String,
    val startTime: String,
    val endTime: String,
    val isDaytime: Boolean,
    val temperature: Int,
    val temperatureUnit: String,
    val shortForecast: String? = null,
    val detailedForecast: String? = null,
    val windSpeed: String? = null,
    val windDirection: String? = null,
    val relativeHumidity: QuantifiedValue? = null,
    val probabilityOfPrecipitation: QuantifiedValue? = null
)

@Serializable
data class QuantifiedValue(
    val value: Double? = null,
    val unitCode: String? = null
)

@Serializable
data class NwsAlertsResponse(
    val features: List<NwsAlertFeature> = emptyList()
)

@Serializable
data class NwsAlertFeature(
    val properties: NwsAlertProperties
)

@Serializable
data class NwsAlertProperties(
    val id: String,
    val areaDesc: String? = null,
    val headline: String? = null,
    val description: String? = null,
    val instruction: String? = null,
    val severity: String? = null,
    val certainty: String? = null,
    val urgency: String? = null,
    val event: String? = null
)
