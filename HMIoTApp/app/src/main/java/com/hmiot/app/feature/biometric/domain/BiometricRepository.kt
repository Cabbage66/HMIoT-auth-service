package com.hmiot.app.feature.biometric.domain

import com.hmiot.app.core.network.model.ApiEnvelope
import com.hmiot.app.core.network.model.ApiResult
import com.hmiot.app.core.storage.datastore.AppPreferenceStore
import com.hmiot.app.core.storage.datastore.BiometricAuditLog
import com.hmiot.app.feature.biometric.data.BiometricApi
import com.hmiot.app.feature.biometric.data.DynamicQueryRequest
import com.hmiot.app.feature.biometric.data.DynamicAuthRecordStatusResponse
import com.hmiot.app.feature.biometric.data.DynamicVerifyResponse
import com.hmiot.app.feature.biometric.data.FaceAuthRecordStatusResponse
import com.hmiot.app.feature.biometric.data.FaceHealthResponse
import com.hmiot.app.feature.biometric.data.FaceSubmitRequest
import com.hmiot.app.feature.biometric.data.FaceTemplateStatusResponse
import com.hmiot.app.feature.biometric.data.FaceVerifyRequest
import com.hmiot.app.feature.biometric.data.FaceVerifyResponse
import com.hmiot.app.feature.biometric.data.FingerprintAuthRecordStatusResponse
import com.hmiot.app.feature.biometric.data.FingerprintChallengeRequest
import com.hmiot.app.feature.biometric.data.FingerprintChallengeResponse
import com.hmiot.app.feature.biometric.data.HealthResponse
import com.hmiot.app.feature.biometric.data.FingerprintHealthResponse
import com.hmiot.app.feature.biometric.data.PageEnvelope
import com.hmiot.app.feature.biometric.data.FingerprintSubmitRequest
import com.hmiot.app.feature.biometric.data.FingerprintTemplateStatusResponse
import com.hmiot.app.feature.biometric.data.FingerprintVerifyRequest
import com.hmiot.app.feature.biometric.data.FingerprintVerifyResponse
import com.hmiot.app.feature.biometric.data.VoiceAuthRecordStatusResponse
import com.hmiot.app.feature.biometric.data.VoiceTemplateStatusResponse
import com.hmiot.app.feature.biometric.data.VoiceVerifyRequest
import com.hmiot.app.feature.biometric.data.VoiceVerifyResponse
import retrofit2.HttpException
import java.time.OffsetDateTime
import kotlinx.coroutines.flow.Flow

class BiometricRepository(
    private val api: BiometricApi,
    private val collector: VoiceSampleCollector,
    private val store: AppPreferenceStore
) {
    val biometricAuditLogs: Flow<List<BiometricAuditLog>> = store.biometricAuditLogs

    suspend fun hasProtectionPassphrase(): Boolean = store.hasBiometricProtectionPassphrase()
    suspend fun setProtectionPassphrase(passphrase: String) = store.setBiometricProtectionPassphrase(passphrase)
    suspend fun verifyProtectionPassphrase(passphrase: String): Boolean = store.verifyBiometricProtectionPassphrase(passphrase)

    suspend fun voiceHealth(): ApiResult<HealthResponse> = runApi { api.voiceHealth() }
    suspend fun dynamicHealth(): ApiResult<HealthResponse> = runApi { api.dynamicHealth() }
    suspend fun fingerprintHealth(): ApiResult<FingerprintHealthResponse> = runApi { api.fingerprintHealth() }
    suspend fun fingerprintChallenge(purpose: String, keyId: String): ApiResult<FingerprintChallengeResponse> =
        runApi { api.fingerprintChallenge(FingerprintChallengeRequest(purpose = purpose, keyId = keyId)) }
    suspend fun fingerprintEnroll(request: FingerprintSubmitRequest): ApiResult<FingerprintTemplateStatusResponse> =
        runApi { api.fingerprintEnroll(request) }
    suspend fun fingerprintRecollect(request: FingerprintSubmitRequest): ApiResult<FingerprintTemplateStatusResponse> =
        runApi { api.fingerprintRecollect(request) }
    suspend fun fingerprintVerify(request: FingerprintVerifyRequest): ApiResult<FingerprintVerifyResponse> =
        runApi { api.fingerprintVerify(request) }
    suspend fun fingerprintTemplates(): ApiResult<List<FingerprintTemplateStatusResponse>> = runApi { api.fingerprintTemplates() }
    suspend fun fingerprintAuthRecords(): ApiResult<PageEnvelope<FingerprintAuthRecordStatusResponse>> = runApi { api.fingerprintAuthRecords() }
    suspend fun faceHealth(): ApiResult<FaceHealthResponse> = runApi { api.faceHealth() }
    suspend fun faceEnroll(imageBase64: String, imageFormat: String = "jpeg"): ApiResult<FaceTemplateStatusResponse> =
        runApi { api.faceEnroll(FaceSubmitRequest(imageBase64 = imageBase64, imageFormat = imageFormat)) }
    suspend fun faceRecollect(imageBase64: String, imageFormat: String = "jpeg"): ApiResult<FaceTemplateStatusResponse> =
        runApi { api.faceRecollect(FaceSubmitRequest(imageBase64 = imageBase64, imageFormat = imageFormat)) }
    suspend fun faceVerify(imageBase64: String, imageFormat: String = "jpeg"): ApiResult<FaceVerifyResponse> =
        runApi { api.faceVerify(FaceVerifyRequest(imageBase64 = imageBase64, imageFormat = imageFormat)) }
    suspend fun faceTemplates(): ApiResult<List<FaceTemplateStatusResponse>> = runApi { api.faceTemplates() }
    suspend fun faceAuthRecords(): ApiResult<PageEnvelope<FaceAuthRecordStatusResponse>> = runApi { api.faceAuthRecords() }

    suspend fun voiceVerify(audioBase64: String): ApiResult<VoiceVerifyResponse> =
        runApi { api.voiceVerify(VoiceVerifyRequest(audioBase64 = audioBase64, audioFormat = "wav", sampleRate = 16000)) }
    suspend fun voiceEnroll(audioBase64: String): ApiResult<VoiceTemplateStatusResponse> =
        runApi { api.voiceEnroll(VoiceVerifyRequest(audioBase64 = audioBase64, audioFormat = "wav", sampleRate = 16000)) }
    suspend fun voiceRecollect(audioBase64: String): ApiResult<VoiceTemplateStatusResponse> =
        runApi { api.voiceRecollect(VoiceVerifyRequest(audioBase64 = audioBase64, audioFormat = "wav", sampleRate = 16000)) }
    suspend fun voiceTemplates(): ApiResult<List<VoiceTemplateStatusResponse>> = runApi { api.voiceTemplates() }
    suspend fun voiceAuthRecords(): ApiResult<PageEnvelope<VoiceAuthRecordStatusResponse>> = runApi { api.voiceAuthRecords() }

    suspend fun dynamicQuery(requestId: String, biometricType: String): ApiResult<DynamicVerifyResponse> =
        runApi { api.dynamicQuery(DynamicQueryRequest(requestId = requestId.ifBlank { null }, biometricType = biometricType)) }
    suspend fun dynamicAuthRecords(): ApiResult<PageEnvelope<DynamicAuthRecordStatusResponse>> = runApi { api.dynamicAuthRecords() }
    suspend fun collectVoiceSampleBase64(seconds: Int = 3): String = collector.collectBase64Wav(seconds = seconds)
    suspend fun appendAudit(action: String, biometricType: String, compareMode: String, result: String, detail: String) {
        store.appendBiometricAuditLog(
            BiometricAuditLog(
                action = action,
                biometricType = biometricType,
                compareMode = compareMode,
                result = result,
                detail = detail,
                at = OffsetDateTime.now().toString()
            )
        )
    }

    private suspend fun <T> runApi(block: suspend () -> ApiEnvelope<T>): ApiResult<T> {
        return try {
            val envelope = block()
            if (envelope.code == "0" && envelope.data != null) {
                ApiResult.Success(envelope.data, envelope.traceId)
            } else {
                ApiResult.Failure(envelope.code, envelope.message, envelope.traceId)
            }
        } catch (http: HttpException) {
            ApiResult.Failure(http.code().toString(), "HTTP ${http.code()}", null)
        } catch (ex: Exception) {
            ApiResult.Failure("NETWORK_ERROR", ex.message ?: "网络异常", null)
        }
    }
}
