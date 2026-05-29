package com.hmiot.app.feature.auth.domain

import com.hmiot.app.core.network.model.ApiResult
import com.hmiot.app.core.session.SessionHolder
import com.hmiot.app.core.session.SessionManager
import com.hmiot.app.core.storage.datastore.LocalSession
import com.hmiot.app.feature.auth.data.AuthApi
import com.hmiot.app.feature.auth.data.BiometricLoginRequest
import com.hmiot.app.feature.auth.data.ChallengeRequest
import com.hmiot.app.feature.auth.data.ChallengeResponse
import com.hmiot.app.feature.auth.data.LoginResponse
import com.hmiot.app.feature.auth.data.PasswordLoginRequest
import com.hmiot.app.feature.auth.data.TerminalEventApi
import com.hmiot.app.feature.auth.data.VerifyRequest
import com.hmiot.app.feature.auth.data.VoiceprintLoginRequest
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException

class AuthRepository(
    private val authApi: AuthApi,
    private val terminalEventApi: TerminalEventApi,
    private val sessionManager: SessionManager
) {
    val session: Flow<LocalSession?> = sessionManager.session

    suspend fun requestChallenge(mode: LoginMode, request: ChallengeRequest): ApiResult<ChallengeResponse> {
        return runApi {
            when (mode) {
                LoginMode.PASSWORD -> throw IllegalStateException("密码登录无需challenge")
                LoginMode.CERTIFICATE -> authApi.certificateChallenge(request)
            }
        }
    }

    suspend fun verify(mode: LoginMode, request: VerifyRequest): ApiResult<LoginResponse> {
        val result = runApi {
            when (mode) {
                LoginMode.PASSWORD -> throw IllegalStateException("密码登录无需verify")
                LoginMode.CERTIFICATE -> authApi.certificateVerify(request)
            }
        }
        if (result is ApiResult.Success) {
            val data = result.data
            sessionManager.saveSession(
                LocalSession(
                    token = data.token,
                    tokenType = data.tokenType,
                    userId = data.userId,
                    username = data.username,
                    realName = data.realName,
                    authMethod = data.authMethod,
                    terminalId = data.terminalId,
                    terminalCode = data.terminalCode,
                    clientType = data.clientType
                )
            )
            SessionHolder.updateToken(data.token)
        }
        return result
    }

    suspend fun probeProtectedEndpoint(): ApiResult<Int> {
        return runApi { terminalEventApi.recent() }.let { result ->
            when (result) {
                is ApiResult.Success -> ApiResult.Success(result.data.size, result.traceId)
                is ApiResult.Failure -> result
            }
        }
    }

    suspend fun logout() {
        sessionManager.clearSession()
        SessionHolder.updateToken(null)
    }

    suspend fun loginByPassword(request: PasswordLoginRequest): ApiResult<LoginResponse> {
        val result = runApi { authApi.passwordLogin(request) }
        if (result is ApiResult.Success) {
            persistSession(result.data)
        }
        return result
    }

    suspend fun loginByFingerprint(request: BiometricLoginRequest): ApiResult<LoginResponse> {
        val result = runApi { authApi.fingerprintLogin(request) }
        if (result is ApiResult.Success) {
            persistSession(result.data)
        }
        return result
    }

    suspend fun loginByFace(request: BiometricLoginRequest): ApiResult<LoginResponse> {
        val result = runApi { authApi.faceLogin(request) }
        if (result is ApiResult.Success) {
            persistSession(result.data)
        }
        return result
    }

    suspend fun loginByVoiceprint(request: VoiceprintLoginRequest): ApiResult<LoginResponse> {
        val result = runApi { authApi.voiceprintLogin(request) }
        if (result is ApiResult.Success) {
            persistSession(result.data)
        }
        return result
    }

    private suspend fun persistSession(data: LoginResponse) {
        sessionManager.saveSession(
            LocalSession(
                token = data.token,
                tokenType = data.tokenType,
                userId = data.userId,
                username = data.username,
                realName = data.realName,
                authMethod = data.authMethod,
                terminalId = data.terminalId,
                terminalCode = data.terminalCode,
                clientType = data.clientType
            )
        )
        SessionHolder.updateToken(data.token)
    }

    private suspend fun <T> runApi(block: suspend () -> com.hmiot.app.core.network.model.ApiEnvelope<T>): ApiResult<T> {
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

enum class LoginMode {
    PASSWORD,
    CERTIFICATE
}
