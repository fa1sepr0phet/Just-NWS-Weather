package com.nwsweather.util

import com.nwsweather.presentation.TemperatureUnit
import kotlin.math.roundToInt

fun convertTemperature(value: Int, sourceUnit: String, targetUnit: TemperatureUnit): Int {
    val isCelsiusSource = sourceUnit.equals("C", ignoreCase = true)

    return when (targetUnit) {
        TemperatureUnit.FAHRENHEIT -> {
            if (isCelsiusSource) {
                ((value * 9 / 5.0) + 32).roundToInt()
            } else {
                value
            }
        }
        TemperatureUnit.CELSIUS -> {
            if (!isCelsiusSource) {
                ((value - 32) * 5 / 9.0).roundToInt()
            } else {
                value
            }
        }
    }
}

fun formatTemperature(value: Int, sourceUnit: String, targetUnit: TemperatureUnit): String {
    val converted = convertTemperature(value, sourceUnit, targetUnit)
    val suffix = when (targetUnit) {
        TemperatureUnit.FAHRENHEIT -> "\u00B0F"
        TemperatureUnit.CELSIUS -> "\u00B0C"
    }

    return "$converted$suffix"
}
