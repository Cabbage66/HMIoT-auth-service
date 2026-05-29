package com.hmiot.app.core.network

import android.os.Build
import com.hmiot.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private val BASE_URL: String = resolveBaseUrl()

    private val httpClient: OkHttpClient by lazy {
        val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        OkHttpClient.Builder()
            .addInterceptor(AuthHeaderInterceptor())
            .addInterceptor(logger)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    private fun resolveBaseUrl(): String {
        val configured = BuildConfig.API_BASE_URL.trim()
        if (configured.isNotBlank() && configured != "AUTO") {
            return if (configured.endsWith("/")) configured else "$configured/"
        }
        val isEmulator = Build.FINGERPRINT.contains("generic", true) ||
            Build.MODEL.contains("Emulator", true) ||
            Build.MODEL.contains("Android SDK built for", true) ||
            Build.BRAND.contains("generic", true) ||
            Build.DEVICE.contains("generic", true) ||
            Build.PRODUCT.contains("sdk", true)
        return if (isEmulator) {
            "http://10.0.2.2:8080/api/v1/"
        } else {
            "http://127.0.0.1:8080/api/v1/"
        }
    }
}
