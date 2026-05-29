package com.hmiot.app.feature.auth.data

import com.hmiot.app.core.network.model.ApiEnvelope
import retrofit2.http.GET

interface TerminalEventApi {
    @GET("terminal-events/my/recent")
    suspend fun recent(): ApiEnvelope<List<Map<String, Any?>>>
}
