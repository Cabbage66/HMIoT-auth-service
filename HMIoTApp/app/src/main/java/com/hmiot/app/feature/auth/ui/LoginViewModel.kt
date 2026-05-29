package com.hmiot.app.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hmiot.app.core.network.NetworkErrorMapper
import com.hmiot.app.core.network.model.ApiResult
import com.hmiot.app.feature.auth.data.ChallengeRequest
import com.hmiot.app.feature.auth.data.BiometricLoginRequest
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
    fun updateSignature(value: String) = _state.update { it.copy(signatureB64 = value) }
    fun updateMode(value: LoginMode) = _state.update { it.copy(loginMode = value) }

    fun requestChallenge() {
        val current = _state.value
        if (current.loginMode == LoginMode.PASSWORD) {
            _state.update { it.copy(error = "密码登录无需获取Challenge，请直接提交登录") }
            return
        }
        if (current.username.isBlank() || current.terminalCode.isBlank()) {
            _state.update { it.copy(error = "用户名与终端编码不能为空") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, info = null) }
            val request = ChallengeRequest(
                username = current.username.trim(),
                terminalCode = current.terminalCode.trim(),
                deviceFingerprint = "android-emulator",
                clientType = "ANDROID",
                clientVersion = "1.0.0-stage1",
                osName = "Android",
                osVersion = android.os.Build.VERSION.RELEASE ?: "unknown",
                userAgent = "HMIoTApp/1.0",
                metadata = """{"phase":"stage1"}"""
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

    fun fingerprintLogin(onSuccess: () -> Unit) {
        val current = _state.value
        if (current.username.isBlank() || current.terminalCode.isBlank()) {
            _state.update { it.copy(error = "请先填写用户名与终端编码") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, info = "指纹认证通过，正在登录...") }
            when (val result = authRepository.loginByFingerprint(buildBiometricLoginRequest(current, "FINGERPRINT"))) {
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

    fun faceLogin(onSuccess: () -> Unit) {
        val current = _state.value
        if (current.username.isBlank() || current.terminalCode.isBlank()) {
            _state.update { it.copy(error = "请先填写用户名与终端编码") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, info = "人脸识别通过，正在登录...") }
            when (val result = authRepository.loginByFace(buildBiometricLoginRequest(current, "FACE"))) {
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
            _state.update { it.copy(error = "请先填写用户名与终端编码") }
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
            _state.update { it.copy(error = "请输入用户名、密码(>=6)和终端编码") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, info = null) }
            val request = PasswordLoginRequest(
                username = current.username.trim(),
                password = current.password,
                terminalCode = current.terminalCode.trim(),
                deviceFingerprint = "android-emulator",
                clientType = "ANDROID",
                clientVersion = "1.0.0",
                osName = "Android",
                osVersion = android.os.Build.VERSION.RELEASE ?: "unknown",
                userAgent = "HMIoTApp/1.0",
                metadata = """{"loginMode":"PASSWORD"}"""
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

    private fun buildBiometricLoginRequest(current: LoginUiState, type: String): BiometricLoginRequest {
        return BiometricLoginRequest(
            username = current.username.trim(),
            terminalCode = current.terminalCode.trim(),
            deviceFingerprint = "android-${android.os.Build.DEVICE ?: "unknown"}",
            clientType = "ANDROID",
            clientVersion = "1.0.0",
            osName = "Android",
            osVersion = android.os.Build.VERSION.RELEASE ?: "unknown",
            userAgent = "HMIoTApp/1.0",
            metadata = """{"loginMode":"$type"}"""
        )
    }

    private fun buildVoiceprintLoginRequest(current: LoginUiState, audioBase64: String): VoiceprintLoginRequest {
        return VoiceprintLoginRequest(
            username = current.username.trim(),
            audioBase64 = audioBase64.trim(),
            audioFormat = "wav",
            sampleRate = 16000,
            terminalCode = current.terminalCode.trim(),
            deviceFingerprint = "android-${android.os.Build.DEVICE ?: "unknown"}",
            clientType = "ANDROID",
            clientVersion = "1.0.0",
            osName = "Android",
            osVersion = android.os.Build.VERSION.RELEASE ?: "unknown",
            userAgent = "HMIoTApp/1.0",
            metadata = """{"loginMode":"VOICEPRINT"}"""
        )
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

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val terminalCode: String = "",
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
