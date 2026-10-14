package com.hmiot.app.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hmiot.app.core.network.NetworkErrorMapper
import com.hmiot.app.core.network.model.ApiResult
import com.hmiot.app.feature.auth.data.ChallengeRequest
import com.hmiot.app.feature.auth.data.FaceLoginRequest
import com.hmiot.app.feature.auth.data.FingerprintChallengeRequest
import com.hmiot.app.feature.auth.data.FingerprintLoginRequest
import com.hmiot.app.feature.auth.data.PasswordLoginRequest
import com.hmiot.app.feature.auth.data.VerifyRequest
import com.hmiot.app.feature.auth.data.VoiceprintLoginRequest
import com.hmiot.app.feature.auth.domain.AuthRepository
import com.hmiot.app.feature.auth.domain.LoginMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun updateUsername(value: String) = _state.update { it.copy(username = value) }
    fun updatePassword(value: String) = _state.update { it.copy(password = value) }
    fun updateTerminalCode(value: String) = _state.update { it.copy(terminalCode = value) }
    fun updateTerminalIdentity(terminalCode: String, deviceFingerprint: String, metadataJson: String) =
        _state.update { it.copy(terminalCode = terminalCode, deviceFingerprint = deviceFingerprint, terminalMetadataJson = metadataJson) }
    fun updateSignature(value: String) = _state.update { it.copy(signatureB64 = value) }
    fun updateMode(value: LoginMode) = _state.update { it.copy(loginMode = value) }

    fun requestChallenge() {
        val current = _state.value
        if (current.loginMode == LoginMode.PASSWORD) {
            _state.update { it.copy(error = "密码登录无需获取Challenge，请直接提交登录") }
            return
        }
        if (current.username.isBlank() || current.terminalCode.isBlank()) {
            _state.update { it.copy(error = "用户名不能为空，或本机终端信息尚未初始化") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, info = null) }
            val request = ChallengeRequest(
                username = current.username.trim(),
                terminalCode = current.terminalCode.trim(),
                deviceFingerprint = current.deviceFingerprint,
                clientType = "ANDROID",
                clientVersion = "1.0.0-stage1",
                osName = "Android",
                osVersion = android.os.Build.VERSION.RELEASE ?: "unknown",
                userAgent = "HMIoTApp/1.0",
                metadata = loginMetadata(current, "CERTIFICATE")
            )
            when (val result = authRepository.requestChallenge(current.loginMode, request)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        loading = false,
                        sessionId = result.data.sessionId,
                        challengeId = result.data.challengeId,
                        challenge = result.data.challenge,
                        expiresAt = result.data.expiresAt,
                        info = "challenge获取成功，traceId=${result.traceId ?: "-"}"
                    )
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = NetworkErrorMapper.toMessage(result.code, result.message, result.traceId))
                }
            }
        }
    }

    fun verifyLogin(onSuccess: () -> Unit) {
        val current = _state.value
        if (current.loginMode == LoginMode.PASSWORD) {
            passwordLogin(onSuccess)
            return
        }
        if (current.sessionId.isBlank() || current.challengeId.isBlank() || current.signatureB64.isBlank()) {
            _state.update { it.copy(error = "请先获取challenge并填写签名") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, info = null) }
            val request = VerifyRequest(
                username = current.username.trim(),
                sessionId = current.sessionId,
                challengeId = current.challengeId,
                signatureB64 = current.signatureB64.trim(),
                terminalCode = current.terminalCode.trim()
            )
            when (val result = authRepository.verify(current.loginMode, request)) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            loading = false,
                            info = "登录成功: user=${result.data.username}, auth=${result.data.authMethod}, traceId=${result.traceId ?: "-"}"
                        )
                    }
                    onSuccess()
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = NetworkErrorMapper.toMessage(result.code, result.message, result.traceId))
                }
            }
        }
    }

    fun requestFingerprintChallenge(
        keyId: String,
        onReady: (FingerprintChallengeUiData) -> Unit
    ) {
        val current = _state.value
        if (current.username.isBlank() || current.terminalCode.isBlank()) {
            _state.update { it.copy(error = "请先填写用户名，或等待本机终端信息初始化") }
            return
        }
        if (keyId.isBlank()) {
            _state.update { it.copy(error = "指纹密钥ID为空") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, info = "正在申请指纹登录挑战...") }
            val request = FingerprintChallengeRequest(
                username = current.username.trim(),
                terminalCode = current.terminalCode.trim(),
                keyId = keyId.trim(),
                deviceFingerprint = current.deviceFingerprint,
                clientType = "ANDROID",
                clientVersion = "1.0.0",
                osName = "Android",
                osVersion = android.os.Build.VERSION.RELEASE ?: "unknown",
                userAgent = "HMIoTApp/1.0",
                metadata = loginMetadata(current, "FINGERPRINT")
            )
            when (val result = authRepository.requestFingerprintChallenge(request)) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            loading = false,
                            info = "指纹挑战获取成功，正在等待系统指纹验证..."
                        )
                    }
                    onReady(
                        FingerprintChallengeUiData(
                            sessionId = result.data.sessionId,
                            challengeId = result.data.challengeId,
                            challenge = result.data.challenge,
                            keyId = result.data.keyId
                        )
                    )
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = NetworkErrorMapper.toMessage(result.code, result.message, result.traceId))
                }
            }
        }
    }

    fun fingerprintLoginWithSignature(
        challenge: FingerprintChallengeUiData,
        signatureBase64: String,
        signAlgorithm: String,
        onSuccess: () -> Unit
    ) {
        val current = _state.value
        if (current.username.isBlank() || current.terminalCode.isBlank()) {
            _state.update { it.copy(error = "请先填写用户名，或等待本机终端信息初始化") }
            return
        }
        if (signatureBase64.isBlank()) {
            _state.update { it.copy(error = "指纹签名为空，无法登录") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, info = "指纹验证通过，正在登录...") }
            val request = FingerprintLoginRequest(
                username = current.username.trim(),
                sessionId = challenge.sessionId,
                challengeId = challenge.challengeId,
                keyId = challenge.keyId,
                signatureBase64 = signatureBase64.trim(),
                signAlgorithm = signAlgorithm.trim().ifBlank { "SHA256withECDSA" },
                terminalCode = current.terminalCode.trim(),
                deviceFingerprint = current.deviceFingerprint,
                clientType = "ANDROID",
                clientVersion = "1.0.0",
                osName = "Android",
                osVersion = android.os.Build.VERSION.RELEASE ?: "unknown",
                userAgent = "HMIoTApp/1.0",
                metadata = loginMetadata(current, "FINGERPRINT", """"keyId":"${challenge.keyId}"""")
            )
            when (val result = authRepository.loginByFingerprint(request)) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            loading = false,
                            info = "登录成功: user=${result.data.username}, auth=${result.data.authMethod}, traceId=${result.traceId ?: "-"}"
                        )
                    }
                    onSuccess()
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = NetworkErrorMapper.toMessage(result.code, result.message, result.traceId))
                }
            }
        }
    }

    fun faceLogin(imageBase64: String, onSuccess: () -> Unit) {
        val current = _state.value
        if (current.username.isBlank() || current.terminalCode.isBlank()) {
            _state.update { it.copy(error = "请先填写用户名，或等待本机终端信息初始化") }
            return
        }
        if (imageBase64.isBlank()) {
            _state.update { it.copy(error = "人脸样本为空，无法登录") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, info = "人脸采集完成，正在远端比对登录...") }
            when (val result = authRepository.loginByFace(buildFaceLoginRequest(current, imageBase64))) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            loading = false,
                            info = "登录成功: user=${result.data.username}, auth=${result.data.authMethod}, traceId=${result.traceId ?: "-"}"
                        )
                    }
                    onSuccess()
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = NetworkErrorMapper.toMessage(result.code, result.message, result.traceId))
                }
            }
        }
    }

    fun voiceprintLogin(audioBase64: String, onSuccess: () -> Unit) {
        val current = _state.value
        if (current.username.isBlank() || current.terminalCode.isBlank()) {
            _state.update { it.copy(error = "请先填写用户名，或等待本机终端信息初始化") }
            return
        }
        if (audioBase64.isBlank()) {
            _state.update { it.copy(error = "声纹样本为空，无法登录") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, info = "声纹采集完成，正在远端比对登录...") }
            when (val result = authRepository.loginByVoiceprint(buildVoiceprintLoginRequest(current, audioBase64))) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            loading = false,
                            info = "登录成功: user=${result.data.username}, auth=${result.data.authMethod}, traceId=${result.traceId ?: "-"}"
                        )
                    }
                    onSuccess()
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = NetworkErrorMapper.toMessage(result.code, result.message, result.traceId))
                }
            }
        }
    }

    private fun passwordLogin(onSuccess: () -> Unit) {
        val current = _state.value
        if (current.username.isBlank() || current.password.length < 6 || current.terminalCode.isBlank()) {
            _state.update { it.copy(error = "请输入用户名、密码(>=6)，并等待本机终端信息初始化") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, info = null) }
            val request = PasswordLoginRequest(
                username = current.username.trim(),
                password = current.password,
                terminalCode = current.terminalCode.trim(),
                deviceFingerprint = current.deviceFingerprint,
                clientType = "ANDROID",
                clientVersion = "1.0.0",
                osName = "Android",
                osVersion = android.os.Build.VERSION.RELEASE ?: "unknown",
                userAgent = "HMIoTApp/1.0",
                metadata = loginMetadata(current, "PASSWORD")
            )
            when (val result = authRepository.loginByPassword(request)) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            loading = false,
                            info = "登录成功: user=${result.data.username}, auth=${result.data.authMethod}, traceId=${result.traceId ?: "-"}"
                        )
                    }
                    onSuccess()
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = NetworkErrorMapper.toMessage(result.code, result.message, result.traceId))
                }
            }
        }
    }

    private fun buildFaceLoginRequest(current: LoginUiState, imageBase64: String): FaceLoginRequest {
        return FaceLoginRequest(
            username = current.username.trim(),
            imageBase64 = imageBase64.trim(),
            imageFormat = "jpeg",
            terminalCode = current.terminalCode.trim(),
            deviceFingerprint = current.deviceFingerprint,
            clientType = "ANDROID",
            clientVersion = "1.0.0",
            osName = "Android",
            osVersion = android.os.Build.VERSION.RELEASE ?: "unknown",
            userAgent = "HMIoTApp/1.0",
            metadata = loginMetadata(current, "FACE")
        )
    }

    private fun buildVoiceprintLoginRequest(current: LoginUiState, audioBase64: String): VoiceprintLoginRequest {
        return VoiceprintLoginRequest(
            username = current.username.trim(),
            audioBase64 = audioBase64.trim(),
            audioFormat = "wav",
            sampleRate = 16000,
            terminalCode = current.terminalCode.trim(),
            deviceFingerprint = current.deviceFingerprint,
            clientType = "ANDROID",
            clientVersion = "1.0.0",
            osName = "Android",
            osVersion = android.os.Build.VERSION.RELEASE ?: "unknown",
            userAgent = "HMIoTApp/1.0",
            metadata = loginMetadata(current, "VOICEPRINT")
        )
    }

    private fun loginMetadata(current: LoginUiState, mode: String, extra: String? = null): String {
        val base = current.terminalMetadataJson.trim().takeIf { it.startsWith("{") && it.endsWith("}") } ?: "{}"
        val additions = buildList {
            add(""""loginMode":"$mode"""")
            if (!extra.isNullOrBlank()) add(extra)
        }.joinToString(",")
        return if (base == "{}") {
            "{$additions}"
        } else {
            base.dropLast(1) + "," + additions + "}"
        }
    }

    fun probeEndpoint() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, info = null) }
            when (val result = authRepository.probeProtectedEndpoint()) {
                is ApiResult.Success -> _state.update {
                    it.copy(loading = false, info = "受保护接口访问成功，记录数=${result.data}, traceId=${result.traceId ?: "-"}")
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = NetworkErrorMapper.toMessage(result.code, result.message, result.traceId))
                }
            }
        }
    }
}

data class FingerprintChallengeUiData(
    val sessionId: String,
    val challengeId: String,
    val challenge: String,
    val keyId: String
)

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val terminalCode: String = "",
    val deviceFingerprint: String = "",
    val terminalMetadataJson: String = "{}",
    val signatureB64: String = "",
    val loginMode: LoginMode = LoginMode.PASSWORD,
    val sessionId: String = "",
    val challengeId: String = "",
    val challenge: String = "",
    val expiresAt: String = "",
    val loading: Boolean = false,
    val info: String? = null,
    val error: String? = null
)

class LoginViewModelFactory(
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unsupported ViewModel: $modelClass")
    }
}
