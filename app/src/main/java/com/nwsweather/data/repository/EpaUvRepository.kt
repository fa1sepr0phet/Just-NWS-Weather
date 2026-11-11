package com.nwsweather.data.repository

import com.nwsweather.data.model.UvForecastHour
import com.nwsweather.data.model.parseEpaUvHours
import com.nwsweather.data.network.EpaUvApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

/** Optional enrichment: EPA failures must never fail the NWS weather refresh. */
class EpaUvRepository(private val api: EpaUvApi) {
    suspend fun fetch(city: String?, state: String?, timeZoneId: String?): List<UvForecastHour> {
        val cityName = city?.trim()?.takeIf { it.isNotEmpty() } ?: return emptyList()
        val stateCode = state?.trim()?.takeIf { it.isNotEmpty() } ?: return emptyList()
        val zone = timeZoneId?.takeIf { it.isNotBlank() } ?: return emptyList()
        return withTimeoutOrNull(5_000L) {
            try {
                parseEpaUvHours(api.getHourlyUv(cityName, stateCode), cityName, stateCode, zone)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
    }
}
