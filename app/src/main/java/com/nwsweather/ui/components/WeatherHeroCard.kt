package com.nwsweather.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.ui.text.font.FontWeight
import com.nwsweather.data.model.NwsForecastPeriod
import com.nwsweather.presentation.TemperatureUnit
import com.nwsweather.util.formatTemperature

@Composable
fun WeatherHeroCard(
    period: NwsForecastPeriod,
    hourlyPeriod: NwsForecastPeriod? = null,
    locationName: String,
    temperatureUnit: TemperatureUnit = TemperatureUnit.FAHRENHEIT,
    cardColor: Color = Color.White.copy(alpha = 0.6f),
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit = {}
) {
    val displayTemp = hourlyPeriod?.temperature ?: period.temperature
    val displayUnit = hourlyPeriod?.temperatureUnit ?: period.temperatureUnit
    val forecastText = hourlyPeriod?.shortForecast ?: period.shortForecast ?: period.name
    
    val icon = weatherIconForForecast(
        forecast = forecastText,
        isDaytime = hourlyPeriod?.isDaytime ?: period.isDaytime
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
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = locationName,
                style = MaterialTheme.typography.titleMedium,
                color = textColor
            )

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
                            value = displayTemp,
                            sourceUnit = displayUnit,
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
                }
            }

            period.detailedForecast?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }

            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Wind
                WeatherDetailItem(
                    icon = Icons.Outlined.Air,
                    label = "Wind",
                    value = "${period.windSpeed ?: "--"} ${period.windDirection ?: ""}".trim(),
                    textColor = textColor
                )

                // Humidity
                val humidity = period.relativeHumidity?.value?.toInt()
                    ?: hourlyPeriod?.relativeHumidity?.value?.toInt()
                WeatherDetailItem(
                    icon = Icons.Outlined.Opacity,
                    label = "Humidity",
                    value = humidity?.let { "$it%" } ?: "--",
                    textColor = textColor
                )

                // Precipitation
                val precip = period.probabilityOfPrecipitation?.value?.toInt()
                if (precip != null) {
                    WeatherDetailItem(
                        icon = Icons.Outlined.Umbrella,
                        label = "Rain",
                        value = "$precip%",
                        textColor = textColor
                    )
                }

                // UV Index (Placeholder as NWS standard periods don't include it)
                WeatherDetailItem(
                    icon = Icons.Outlined.WbSunny,
                    label = "UV",
                    value = "4 (Mod)",
                    textColor = textColor
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
            tint = textColor.copy(alpha = 0.6f)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.6f)
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
