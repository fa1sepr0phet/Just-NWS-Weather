package com.nwsweather.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
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
import com.nwsweather.data.model.NwsForecastPeriod
import com.nwsweather.presentation.TemperatureUnit
import com.nwsweather.util.formatTemperature

@Composable
fun ForecastCard(
    periods: List<NwsForecastPeriod>,
    temperatureUnit: TemperatureUnit = TemperatureUnit.FAHRENHEIT,
    cardColor: Color = Color.White.copy(alpha = 0.84f),
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit = {}
) {
    val dayPeriod = periods.firstOrNull { it.isDaytime } ?: periods.firstOrNull()
    val nightPeriod = periods.firstOrNull { !it.isDaytime } ?: periods.lastOrNull()
    
    if (dayPeriod == null) return

    val forecastText = dayPeriod.shortForecast ?: "Forecast unavailable"
    val icon = weatherIconForForecast(
        forecast = forecastText,
        isDaytime = dayPeriod.isDaytime
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
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dayPeriod.name.removeSuffix(" Night"),
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WbSunny,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = textColor.copy(alpha = 0.7f)
                        )
                        Text(
                            text = formatTemperature(
                                value = dayPeriod.temperature,
                                sourceUnit = dayPeriod.temperatureUnit,
                                targetUnit = temperatureUnit
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor
                        )
                    }
                    if (nightPeriod != null && nightPeriod != dayPeriod) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NightlightRound,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = textColor.copy(alpha = 0.5f)
                            )
                            Text(
                                text = formatTemperature(
                                    value = nightPeriod.temperature,
                                    sourceUnit = nightPeriod.temperatureUnit,
                                    targetUnit = temperatureUnit
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                color = textColor.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = forecastText,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = forecastText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor
                )
            }

            if (nightPeriod != null && nightPeriod != dayPeriod) {
                Text(
                    text = "Evening: ${nightPeriod.shortForecast ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}
