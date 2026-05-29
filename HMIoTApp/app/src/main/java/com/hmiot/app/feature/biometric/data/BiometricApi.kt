package com.hmiot.app.feature.biometric.data

import com.hmiot.app.core.network.model.ApiEnvelope
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface BiometricApi {
    @GET("voiceprints/health")
    suspend fun voiceHealth(): ApiEnvelope<HealthResponse>

    @POST("voiceprints/verify")
    suspend fun voiceVerify(@Body request: VoiceVerifyRequest): ApiEnvelope<VoiceVerifyResponse>

    @POST("voiceprints/enroll")
    suspend fun voiceEnroll(@Body request: VoiceVerifyRequest): ApiEnvelope<VoiceTemplateStatusResponse>

    @POST("voiceprints/recollect")
    suspend fun voiceRecollect(@Body request: VoiceVerifyRequest): ApiEnvelope<VoiceTemplateStatusResponse>

    @GET("voiceprints/me")
    suspend fun voiceTemplates(): ApiEnvelope<List<VoiceTemplateStatusResponse>>

    @GET("voiceprints/auth-records/me")
    suspend fun voiceAuthRecords(
        @Query("authResult") authResult: String? = null,
        @Query("current") current: Long = 1,
        @Query("size") size: Long = 20
    ): ApiEnvelope<PageEnvelope<VoiceAuthRecordStatusResponse>>

    @GET("dynamic-biometrics/health")
    suspend fun dynamicHealth(): ApiEnvelope<HealthResponse>

    @POST("dynamic-biometrics/query")
    suspend fun dynamicQuery(@Body request: DynamicQueryRequest): ApiEnvelope<DynamicVerifyResponse>

    @GET("dynamic-biometrics/auth-records/me")
    suspend fun dynamicAuthRecords(
        @Query("authResult") authResult: String? = null,
        @Query("current") current: Long = 1,
        @Query("size") size: Long = 20
    ): ApiEnvelope<PageEnvelope<DynamicAuthRecordStatusResponse>>
}

data class HealthResponse(
    val healthy: Boolean,
    val targetSystem: String,
    val modelVersion: String
)

data class VoiceVerifyRequest(
    val audioBase64: String,
    val audioFormat: String,
    val sampleRate: Int
)

data class VoiceVerifyResponse(
    val recordId: Long,
    val verified: Boolean,
    val score: Double?,
    val threshold: Double?,
    val failReason: String?
)

data class DynamicQueryRequest(
    val requestId: String?,
    val biometricType: String?
)

data class DynamicVerifyResponse(
    val recordId: Long,
    val requestId: String?,
    val matched: Boolean,
    val score: Double?,
    val threshold: Double?,
    val failReason: String?
)

data class VoiceTemplateStatusResponse(
    val id: Long,
    val biometricType: String,
    val modelSource: String?,
    val modelVersion: String?,
    val threshold: Double?,
    val auditStatus: String,
    val templateStatus: String,
    val versionNo: Int,
    val submittedAt: String?,
    val reviewedAt: String?,
    val revokedAt: String?,
    val rejectReason: String?,
    val createdAt: String?,
    val updatedAt: String?
)

data class VoiceAuthRecordStatusResponse(
    val id: Long,
    val biometricType: String,
    val templateId: Long?,
    val score: Double?,
    val threshold: Double?,
    val authResult: String,
    val failReason: String?,
    val modelSource: String?,
    val createdAt: String
)

data class DynamicAuthRecordStatusResponse(
    val id: Long,
    val biometricType: String,
    val score: Double?,
    val threshold: Double?,
    val authResult: String,
    val failReason: String?,
    val modelSource: String?,
    val createdAt: String
)

data class PageEnvelope<T>(
    val current: Long,
    val size: Long,
    val total: Long,
    val records: List<T>
)
