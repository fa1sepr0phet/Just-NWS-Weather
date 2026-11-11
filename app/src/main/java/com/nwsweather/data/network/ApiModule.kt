package com.nwsweather.data.network

import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType

object ApiModule {
    // EPA is used only for UV; keep its client and headers separate from NWS.
    val epaUvApi: EpaUvApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://data.epa.gov/dmapservice/")
            .client(OkHttpClient.Builder()
                .callTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    chain.proceed(chain.request().newBuilder()
                        .header("Accept", "application/json")
                        .header("User-Agent", "(Just NWS Weather, falsepr0phet@protonmail.com)")
                        .build())
                }.build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(EpaUvApi::class.java)
    }

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val nwsHeaders = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("User-Agent", "(Just NWS Weather, falsepr0phet@protonmail.com)")
            .header("Accept", "application/geo+json")
            .header("Cache-Control", "no-cache")
            .header("Pragma", "no-cache")
            .build()
        chain.proceed(request)
    }

    private val nwsClient = OkHttpClient.Builder()
        .callTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
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
