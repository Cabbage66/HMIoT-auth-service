package com.hmiot.app.feature.auth.data

import com.hmiot.app.core.network.model.ApiEnvelope
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/password/login")
    suspend fun passwordLogin(@Body request: PasswordLoginRequest): ApiEnvelope<LoginResponse>

    @POST("auth/fingerprint/login")
    suspend fun fingerprintLogin(@Body request: BiometricLoginRequest): ApiEnvelope<LoginResponse>

    @POST("auth/face/login")
    suspend fun faceLogin(@Body request: BiometricLoginRequest): ApiEnvelope<LoginResponse>

    @POST("auth/voiceprint/login")
    suspend fun voiceprintLogin(@Body request: VoiceprintLoginRequest): ApiEnvelope<LoginResponse>

    @POST("auth/certificate/challenge")
    suspend fun certificateChallenge(@Body request: ChallengeRequest): ApiEnvelope<ChallengeResponse>

    @POST("auth/certificate/verify")
    suspend fun certificateVerify(@Body request: VerifyRequest): ApiEnvelope<LoginResponse>
}

data class ChallengeRequest(
    val username: String,
    val terminalCode: String,
    val deviceFingerprint: String,
    val clientType: String,
    val clientVersion: String,
    val osName: String,
    val osVersion: String,
    val userAgent: String,
    val metadata: String
)

data class PasswordLoginRequest(
    val username: String,
    val password: String,
    val terminalCode: String,
    val deviceFingerprint: String,
    val clientType: String,
    val clientVersion: String,
    val osName: String,
    val osVersion: String,
    val userAgent: String,
    val metadata: String
)

data class BiometricLoginRequest(
    val username: String,
    val terminalCode: String,
    val deviceFingerprint: String,
    val clientType: String,
    val clientVersion: String,
    val osName: String,
    val osVersion: String,
    val userAgent: String,
    val metadata: String
)

data class VoiceprintLoginRequest(
    val username: String,
    val audioBase64: String,
    val audioFormat: String,
    val sampleRate: Int,
    val terminalCode: String,
    val deviceFingerprint: String,
    val clientType: String,
    val clientVersion: String,
    val osName: String,
    val osVersion: String,
    val userAgent: String,
    val metadata: String
)

data class VerifyRequest(
    val username: String,
    val sessionId: String,
    val challengeId: String,
    val signatureB64: String,
    val terminalCode: String
)

data class ChallengeResponse(
    val sessionId: String,
    val challengeId: String,
    val challenge: String,
    val expiresInSeconds: Int,
    val expiresAt: String
)

data class LoginResponse(
    val token: String,
    val tokenType: String,
    val userId: Long,
    val username: String,
    val realName: String,
    val authMethod: String,
    val terminalId: Long,
    val terminalCode: String,
    val clientType: String
)
