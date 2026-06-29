package com.nwsweather.data.network

import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType

object ApiModule {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val nwsHeaders = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("User-Agent", "(Just NWS Weather, anonymous)")
            .header("Accept", "application/geo+json")
            .build()
        chain.proceed(request)
    }

    private val nwsClient = OkHttpClient.Builder()
        .addInterceptor(nwsHeaders)
        .build()

    val nwsApi: NwsApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.weather.gov/")
            .client(nwsClient)
            .addConverterFactory(json.asConverterFactory("application/geo+json".toMediaType()))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(NwsApi::class.java)
    }
}
