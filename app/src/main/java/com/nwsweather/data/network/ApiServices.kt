package com.nwsweather.data.network

import com.nwsweather.data.model.NwsAlertsResponse
import com.nwsweather.data.model.NwsForecastResponse
import com.nwsweather.data.model.NwsPointsResponse
import com.nwsweather.data.model.NwsStationsResponse
import com.nwsweather.data.model.NwsObservationResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface EpaUvApi {
    @GET("getEnvirofactsUVHOURLY/CITY/{city}/STATE/{state}/JSON")
    suspend fun getHourlyUv(
        @Path("city") city: String,
        @Path("state") state: String
    ): List<com.nwsweather.data.model.EpaUvHour>
}

interface NwsApi {
    @GET("points/{lat},{lon}")
    suspend fun getPointMetadata(
        @Path("lat") lat: String,
        @Path("lon") lon: String
    ): NwsPointsResponse

    @GET
    suspend fun getForecast(@Url url: String): NwsForecastResponse

    @GET
    suspend fun getStations(@Url url: String): NwsStationsResponse

    @GET
    suspend fun getLatestObservation(@Url url: String): NwsObservationResponse

    @GET("alerts/active")
    suspend fun getActiveAlerts(
        @Query("point") point: String
    ): NwsAlertsResponse
}
