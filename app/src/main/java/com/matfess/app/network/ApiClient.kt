package com.matfess.app.network

import com.matfess.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                    else HttpLoggingInterceptor.Level.NONE
                }
            )
            .build()
    }

    val api: MatfessApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://date.nager.at/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MatfessApi::class.java)
    }

    const val QUOTE_ENDPOINT = "https://dummyjson.com/quotes/random"
}
