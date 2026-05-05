package com.nwsweather.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class CitySuggestion(
    val city: String,
    val state: String,
    val latitude: Double,
    val longitude: Double,
    val fullDisplay: String
)

class CityRepository(private val context: Context) {
    private var allCities: List<CitySuggestion> = emptyList()

    suspend fun searchCities(query: String): List<CitySuggestion> = withContext(Dispatchers.IO) {
        if (query.length < 2) return@withContext emptyList()

        if (allCities.isEmpty()) {
            loadCities()
        }

        allCities.filter { city ->
            city.fullDisplay.contains(query, ignoreCase = true) ||
                    city.city.contains(query, ignoreCase = true)
        }.take(5) // Limit to 5 suggestions
    }

    private fun loadCities() {
        val cities = mutableListOf<CitySuggestion>()
        try {
            context.assets.open("us_cities.txt").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.forEachLine { line ->
                        if (line.isBlank()) return@forEachLine
                        val parts = line.split("|").map { it.trim() }
                        if (parts.size >= 4) {
                            val city = parts[0]
                            val state = parts[1]
                            val lat = parts[2].toDoubleOrNull() ?: return@forEachLine
                            val lon = parts[3].toDoubleOrNull() ?: return@forEachLine
                            cities.add(
                                CitySuggestion(
                                    city = city,
                                    state = state,
                                    latitude = lat,
                                    longitude = lon,
                                    fullDisplay = "$city, $state"
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        allCities = cities
    }
}
