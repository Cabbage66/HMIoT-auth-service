package com.hmiot.app.feature.access.data

import com.hmiot.app.core.network.model.ApiEnvelope
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AccessApi {
    @GET("apps")
    suspend fun apps(
        @Query("status") status: String = "ENABLED",
        @Query("current") current: Long = 1,
        @Query("size") size: Long = 50
    ): ApiEnvelope<PageEnvelope<AppResource>>

    @POST("trust-assessments/check")
    suspend fun trustCheck(@Body request: TrustCheckRequest = TrustCheckRequest()): ApiEnvelope<TrustCheckResponse>

    @POST("tickets/issue")
    suspend fun issueTicket(@Body request: TicketIssueRequest): ApiEnvelope<IdentityTicket>

    @POST("sso/access")
    suspend fun ssoAccess(@Body request: SsoAccessRequest): ApiEnvelope<SsoAccessResponse>
}

data class PageEnvelope<T>(
    val current: Long,
    val size: Long,
    val total: Long,
    val records: List<T>
)

data class AppResource(
    val id: Long,
    val appCode: String,
    val appName: String,
    val resourceType: String,
    val accessUrl: String?,
    val securityLevel: Int,
    val requiredTrustLevel: Int,
    val status: String,
    val description: String?
)

data class TrustCheckRequest(
    val appCode: String? = null,
    val appResourceId: Long? = null,
    val requestId: String? = null
)

data class TrustCheckResponse(
    val trusted: Boolean,
    val trustLevel: Int?,
    val failReason: String?
)

data class TicketIssueRequest(
    val appCode: String,
    val audienceSystem: String,
    val terminalId: Long? = null,
    val trustLevel: Int? = null
)

data class IdentityTicket(
    val ticketId: String,
    val appCode: String,
    val trustLevel: Int?,
    val status: String
)

data class SsoAccessRequest(
    val appCode: String,
    val targetSystem: String
)

data class SsoAccessResponse(
    val success: Boolean,
    val message: String,
    val appCode: String,
    val accessUrl: String?,
    val ticket: IdentityTicket?
)
