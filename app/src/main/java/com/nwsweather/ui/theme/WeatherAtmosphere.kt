package com.nwsweather.ui.theme

import androidx.compose.material3.ColorScheme
import com.nwsweather.presentation.AppTheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class WeatherMood {
    SUNNY,
    CLOUDY,
    RAIN,
    STORM,
    SNOW,
    CLEAR_NIGHT,
    CLOUDY_NIGHT
}

/** Cards are slightly see-through so the weather animation behind them stays visible. */
const val WEATHER_CARD_ALPHA = 0.72f

data class WeatherVisuals(
    val background: Brush,
    val cardColor: Color,
    val onCardTextColor: Color,
    val appBarContentColor: Color
)

fun mapWeatherMood(forecast: String, isDay: Boolean): WeatherMood {
    val text = forecast.lowercase()

    return when {
        text.contains("storm") || text.contains("thunder") -> WeatherMood.STORM
        text.contains("snow") || text.contains("flurr") || text.contains("sleet") -> WeatherMood.SNOW
        text.contains("rain") || text.contains("shower") || text.contains("drizzle") -> WeatherMood.RAIN
        text.contains("cloud") || text.contains("overcast") -> {
            if (isDay) WeatherMood.CLOUDY else WeatherMood.CLOUDY_NIGHT
        }
        else -> {
            if (isDay) WeatherMood.SUNNY else WeatherMood.CLEAR_NIGHT
        }
    }
}

fun visualsForMood(mood: WeatherMood, resolvedTheme: AppTheme, colors: ColorScheme): WeatherVisuals {
    if (resolvedTheme == AppTheme.MIDNIGHT) {
        // Midnight has no animation to show through, so its cards stay solid.
        return WeatherVisuals(
            background = Brush.verticalGradient(listOf(Color.Black, Color.Black)),
            cardColor = colors.surface,
            onCardTextColor = colors.onSurface,
            appBarContentColor = colors.onBackground
        )
    }
    if (resolvedTheme == AppTheme.DARK) {
        val bottom = when (mood) {
            WeatherMood.SUNNY -> Color(0xFF293B50)
            WeatherMood.CLOUDY, WeatherMood.CLOUDY_NIGHT -> Color(0xFF263541)
            WeatherMood.RAIN, WeatherMood.STORM -> Color(0xFF1C3043)
            WeatherMood.SNOW -> Color(0xFF304354)
            WeatherMood.CLEAR_NIGHT -> Color(0xFF14243D)
        }
        return WeatherVisuals(
            background = Brush.verticalGradient(listOf(Color(0xFF0E1722), bottom)),
            cardColor = colors.surface.copy(alpha = WEATHER_CARD_ALPHA),
            onCardTextColor = colors.onSurface,
            appBarContentColor = colors.onBackground
        )
    }
    return lightVisualsForMood(mood).copy(
        cardColor = colors.surface.copy(alpha = WEATHER_CARD_ALPHA),
        onCardTextColor = colors.onSurface
    )
}

private fun lightVisualsForMood(mood: WeatherMood): WeatherVisuals {
    val darkText = Color.Black
    val lightText = Color.White
    val lightCard = Color.White

    return when (mood) {
        WeatherMood.SUNNY -> WeatherVisuals(
            background = Brush.verticalGradient(
                listOf(
                    Color(0xFF69B7FF),
                    Color(0xFFAAD8FF),
                    Color(0xFFFFD67A)
                )
            ),
            cardColor = lightCard,
            onCardTextColor = darkText,
            appBarContentColor = darkText
        )

        WeatherMood.CLOUDY -> WeatherVisuals(
            background = Brush.verticalGradient(
                listOf(
                    Color(0xFF8FA3B0),
                    Color(0xFFB8C7D0),
                    Color(0xFFE2E8EC)
                )
            ),
            cardColor = lightCard,
            onCardTextColor = darkText,
            appBarContentColor = darkText
        )

        WeatherMood.RAIN -> WeatherVisuals(
            background = Brush.verticalGradient(
                listOf(
                    Color(0xFF355C7D),
                    Color(0xFF4F6D7A),
                    Color(0xFF7C98A3)
                )
            ),
            cardColor = lightCard,
            onCardTextColor = darkText,
            appBarContentColor = lightText
        )

        WeatherMood.STORM -> WeatherVisuals(
            background = Brush.verticalGradient(
                listOf(
                    Color(0xFF16213E),
                    Color(0xFF23395B),
                    Color(0xFF3C4F76)
                )
            ),
            cardColor = lightCard,
            onCardTextColor = darkText,
            appBarContentColor = lightText
        )

        WeatherMood.SNOW -> WeatherVisuals(
            background = Brush.verticalGradient(
                listOf(
                    Color(0xFFDDEAF7),
                    Color(0xFFF4F8FC),
                    Color(0xFFFFFFFF)
                )
            ),
            cardColor = Color.White,
            onCardTextColor = darkText,
            appBarContentColor = darkText
        )

        WeatherMood.CLEAR_NIGHT -> WeatherVisuals(
            background = Brush.verticalGradient(
                listOf(
                    Color(0xFF09111F),
                    Color(0xFF14243D),
                    Color(0xFF223A5E)
                )
            ),
            cardColor = lightCard,
            onCardTextColor = darkText,
            appBarContentColor = lightText
        )

        WeatherMood.CLOUDY_NIGHT -> WeatherVisuals(
            background = Brush.verticalGradient(
                listOf(
                    Color(0xFF1A2633),
                    Color(0xFF2C3E50),
                    Color(0xFF415B76)
                )
            ),
            cardColor = lightCard,
            onCardTextColor = darkText,
            appBarContentColor = lightText
        )
    }
}
