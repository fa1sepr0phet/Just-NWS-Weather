package com.nwsweather.util

import com.nwsweather.presentation.TemperatureUnit
import kotlin.math.roundToInt

fun formatTemperature(value: Int, sourceUnit: String, targetUnit: TemperatureUnit): String {
    val isCelsiusSource = sourceUnit.equals("C", ignoreCase = true)
    
    return when (targetUnit) {
        TemperatureUnit.FAHRENHEIT -> {
            if (isCelsiusSource) {
                val converted = (value * 9/5.0) + 32
                "${converted.roundToInt()}°F"
            } else {
                "$value°F"
            }
        }
        TemperatureUnit.CELSIUS -> {
            if (!isCelsiusSource) {
                val converted = (value - 32) * 5/9.0
                "${converted.roundToInt()}°C"
            } else {
                "$value°C"
            }
        }
    }
}
