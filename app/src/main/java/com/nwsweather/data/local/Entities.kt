package com.nwsweather.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_locations")
data class SavedLocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val city: String? = null,
    val state: String? = null,
    val displayOrder: Int = 0
)

@Entity(tableName = "point_cache")
data class PointCacheEntity(
    @PrimaryKey val key: String,
    val gridId: String,
    val gridX: Int,
    val gridY: Int,
    val forecastUrl: String,
    val forecastHourlyUrl: String,
    val forecastGridDataUrl: String,
    val timeZone: String? = null,
    val city: String? = null,
    val state: String? = null,
    val cachedAtEpochMs: Long
)

@Entity(tableName = "weather_snapshot")
data class WeatherSnapshotEntity(
    @PrimaryKey val id: Int = 0,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val temperature: Int,
    val temperatureUnit: String,
    val shortForecast: String,
    val humidity: Int? = null,
    val windSpeed: String,
    val windDirection: String,
    val uvIndex: Int? = null,
    val updatedAtEpochMs: Long,
    val isDaytime: Boolean
)
