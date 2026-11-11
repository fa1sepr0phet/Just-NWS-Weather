package com.nwsweather.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.ui.text.font.FontWeight
import com.nwsweather.data.model.NwsForecastPeriod
import com.nwsweather.presentation.TemperatureUnit
import com.nwsweather.util.formatTemperature

@Composable
fun WeatherHeroCard(
    period: NwsForecastPeriod,
    hourlyPeriod: NwsForecastPeriod? = null,
    uvForecast: com.nwsweather.data.model.UvForecastHour? = null,
    uvTimeZoneId: String? = null,
    displayTemperature: Double = (hourlyPeriod?.temperature ?: period.temperature).toDouble(),
    displayTemperatureUnit: String = hourlyPeriod?.temperatureUnit ?: period.temperatureUnit,
    forecastText: String = hourlyPeriod?.shortForecast ?: period.shortForecast ?: period.name,
    readingTimestampLabel: String? = null,
    isDaytime: Boolean = hourlyPeriod?.isDaytime ?: period.isDaytime,
    temperatureUnit: TemperatureUnit = TemperatureUnit.FAHRENHEIT,
    cardColor: Color = Color.White.copy(alpha = 0.6f),
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit = {}
) {
    val icon = weatherIconForForecast(
        forecast = forecastText,
        isDaytime = isDaytime
    )

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = forecastText,
                    modifier = Modifier.size(42.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Column {
                    Text(
                        text = formatTemperature(
                            value = displayTemperature,
                            sourceUnit = displayTemperatureUnit,
                            targetUnit = temperatureUnit
                        ),
                        style = MaterialTheme.typography.displayMedium,
                        color = textColor
                    )

                    Text(
                        text = forecastText,
                        style = MaterialTheme.typography.titleLarge,
                        color = textColor
                    )

                    readingTimestampLabel?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor
                        )
                    }
                }
            }

            period.detailedForecast?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = "${period.name} forecast: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }

            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // These details describe the forecast period, not the observation.
                WeatherDetailItem(
                    icon = Icons.Outlined.Air,
                    label = if (hourlyPeriod != null) "Hourly wind" else "Wind",
                    value = "${(hourlyPeriod ?: period).windSpeed ?: "--"} ${(hourlyPeriod ?: period).windDirection ?: ""}".trim(),
                    textColor = textColor
                )

                // Humidity
                val humidity = (hourlyPeriod ?: period).relativeHumidity?.value?.toInt()
                WeatherDetailItem(
                    icon = Icons.Outlined.Opacity,
                    label = if (hourlyPeriod != null) "Hourly humidity" else "Humidity",
                    value = humidity?.let { "$it%" } ?: "--",
                    textColor = textColor
                )

                // Precipitation
                val precip = (hourlyPeriod ?: period).probabilityOfPrecipitation?.value?.toInt()
                if (precip != null) {
                    WeatherDetailItem(
                        icon = Icons.Outlined.WaterDrop,
                        label = if (hourlyPeriod != null) "Hourly precip" else "Precip",
                        value = "$precip%",
                        textColor = textColor
                    )
                }
                if (uvForecast != null && uvTimeZoneId != null) {
                    WeatherDetailItem(
                        icon = Icons.Outlined.WbSunny,
                        label = "UV forecast",
                        value = uvForecast.value.toString(),
                        textColor = textColor
                    )
                }
            }
            if (uvForecast != null && uvTimeZoneId != null) {
                Text(
                    text = uvForecast.label(uvTimeZoneId),
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun WeatherDetailItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    textColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = textColor
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = textColor
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        }
    }
}
