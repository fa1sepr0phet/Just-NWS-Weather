package com.nwsweather.util

import com.nwsweather.data.network.NwsApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AlertLocationTest {
    @Test fun alertRequestsKeepPointFilterAndDoNotReuseAnotherLocationsAlerts() = runBlocking {
        val requestedPoints = mutableListOf<String?>()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            assertEquals("/alerts/active", request.url.encodedPath)
            val point = request.url.queryParameter("point")
            requestedPoints.add(point)
            val body = when (point) {
                "37.7749,-122.4194" -> """{"features":[{"properties":{"id":"sf","event":"Wind Advisory","areaDesc":"San Francisco, CA","description":"California details","instruction":"California instructions"}}]}"""
                "39.3111,-94.9225" -> """{"features":[{"properties":{"id":"ks","event":"Test local alert","areaDesc":"Leavenworth, KS","description":"Kansas details","instruction":"Kansas instructions"}}]}"""
                else -> """{"features":[]}"""
            }
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK")
                .body(body.toResponseBody("application/geo+json".toMediaType())).build()
        }.build()
        val api = Retrofit.Builder().baseUrl("https://api.weather.gov/").client(client)
            .addConverterFactory(Json.asConverterFactory("application/geo+json".toMediaType()))
            .build().create(NwsApi::class.java)

        assertEquals("sf", api.getActiveAlerts("37.7749,-122.4194").features.single().properties.id)
        val local = api.getActiveAlerts("39.3111,-94.9225").features.single().properties
        assertEquals("ks", local.id)
        assertEquals("Leavenworth, KS", local.areaDesc)
        assertEquals("Kansas details", local.description)
        assertEquals("Kansas instructions", local.instruction)
        assertTrue(api.getActiveAlerts("40.0,-95.0").features.isEmpty())
        assertEquals(listOf("37.7749,-122.4194", "39.3111,-94.9225", "40.0,-95.0"), requestedPoints)
    }
}
