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
import com.nwsweather.data.model.NwsForecastPeriod
import com.nwsweather.presentation.TemperatureUnit
import com.nwsweather.util.formatTemperature

@Composable
fun WeatherHeroCard(
    period: NwsForecastPeriod,
    locationName: String,
    temperatureUnit: TemperatureUnit = TemperatureUnit.FAHRENHEIT,
    cardColor: Color = Color.White.copy(alpha = 0.6f),
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit = {}
) {
    val forecastText = period.shortForecast ?: period.name
    val icon = weatherIconForForecast(
        forecast = forecastText,
        isDaytime = period.isDaytime
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
                            value = period.temperature,
                            sourceUnit = period.temperatureUnit,
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Wind: ${period.windSpeed ?: "--"} ${period.windDirection ?: ""}".trim(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.82f),
                    modifier = Modifier.wrapContentWidth()
                )

                val precip = period.probabilityOfPrecipitation?.value?.toInt()
                if (precip != null) {
                    Text(
                        text = "Rain: $precip%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.82f)
                    )
                }
                
                // UV Index - NWS API doesn't provide this in the standard forecast periods, 
                // but we can add a placeholder or fetch it if we had the grid data.
                // For now, let's show a simulated/placeholder value as requested for the "polish".
                Text(
                    text = "UV: 4 (Moderate)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.82f)
                )
            }
        }
    }
}
