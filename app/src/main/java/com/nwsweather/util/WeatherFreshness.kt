package com.nwsweather.util

import java.time.OffsetDateTime

object WeatherFreshness {
    const val OBSERVATION_MAX_AGE_MS = 30 * 60 * 1000L
    const val FOREGROUND_REFRESH_MS = 5 * 60 * 1000L

    fun epochMillis(timestamp: String?): Long? =
        runCatching { OffsetDateTime.parse(timestamp).toInstant().toEpochMilli() }.getOrNull()

    fun isRecent(timestamp: String?, now: Long = System.currentTimeMillis()): Boolean =
        epochMillis(timestamp)?.let { now - it in 0..OBSERVATION_MAX_AGE_MS } ?: false

    fun coversNow(start: String, end: String, now: Long = System.currentTimeMillis()): Boolean {
        val startMs = epochMillis(start) ?: return false
        val endMs = epochMillis(end) ?: return false
        return now >= startMs && now < endMs
    }
}
