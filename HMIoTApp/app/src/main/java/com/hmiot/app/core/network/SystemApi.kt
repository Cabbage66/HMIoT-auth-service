package com.hmiot.app.core.network

import retrofit2.http.GET

interface SystemApi {
    @GET("health")
    suspend fun health(): String
}
