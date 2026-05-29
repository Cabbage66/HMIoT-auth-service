package com.hmiot.app.feature.registration.data

import com.hmiot.app.core.network.model.ApiEnvelope
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface RegistrationApi {
    @POST("registrations")
    suspend fun submit(@Body request: RegistrationSubmitRequest): ApiEnvelope<RegistrationSubmitResponse>

    @GET("registrations/progress")
    suspend fun progress(
        @Query("username") username: String,
        @Query("current") current: Long = 1,
        @Query("size") size: Long = 20
    ): ApiEnvelope<PageEnvelope<RegistrationProgressItem>>
}

data class RegistrationSubmitRequest(
    val username: String,
    val password: String,
    val realName: String,
    val phone: String?,
    val email: String?,
    val idCardNo: String?
)

data class RegistrationSubmitResponse(
    val registrationId: Long,
    val userId: Long,
    val username: String,
    val status: String,
    val submittedAt: String
)

data class RegistrationProgressItem(
    val registrationId: Long,
    val username: String,
    val realName: String,
    val phone: String?,
    val email: String?,
    val status: String,
    val rejectReason: String?,
    val reviewedAt: String?,
    val createdAt: String
)

data class PageEnvelope<T>(
    val current: Long,
    val size: Long,
    val total: Long,
    val records: List<T>
)
