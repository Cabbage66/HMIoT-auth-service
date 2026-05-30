package com.hmiot.app.feature.terminal.data

import com.hmiot.app.core.network.model.ApiEnvelope
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface TerminalMonitorApi {
    @POST("terminal-events/reports")
    suspend fun report(@Body request: TerminalEventReportRequest): ApiEnvelope<TerminalEventResponse>

    @GET("terminal-events/my/recent")
    suspend fun recent(): ApiEnvelope<List<TerminalEventResponse>>

    @POST("trust-assessments/check")
    suspend fun trustCheck(@Body request: TrustCheckRequest): ApiEnvelope<TrustCheckResponse>
}

data class TerminalEventReportRequest(
    val eventType: String,
    val eventLevel: String,
    val eventTime: String,
    val eventPayload: Map<String, Any?>
)

data class TerminalEventResponse(
    val id: Long,
    val terminalId: Long,
    val userId: Long,
    val eventType: String,
    val eventLevel: String,
    val eventTime: String,
    val eventPayload: Map<String, Any?>?,
    val normalizedPayload: Map<String, Any?>?,
    val reportedAt: String
)

data class TrustCheckRequest(
    val appCode: String? = null,
    val appResourceId: Long? = null,
    val requestId: String? = null
)

data class TrustCheckResponse(
    val assessmentResultId: Long?,
    val requestId: String?,
    val trusted: Boolean,
    val trustScore: Double?,
    val trustLevel: Int?,
    val riskLevel: String?,
    val decision: String?,
    val action: String?,
    val failReason: String?,
    val assessedAt: String?,
    val requestLogPath: String?,
    val resultLogPath: String?
)
