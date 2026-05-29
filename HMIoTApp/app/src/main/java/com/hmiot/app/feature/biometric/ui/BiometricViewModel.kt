package com.hmiot.app.feature.biometric.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hmiot.app.core.network.NetworkErrorMapper
import com.hmiot.app.core.network.model.ApiResult
import com.hmiot.app.feature.biometric.data.DynamicAuthRecordStatusResponse
import com.hmiot.app.core.storage.datastore.BiometricAuditLog
import com.hmiot.app.feature.biometric.data.VoiceAuthRecordStatusResponse
import com.hmiot.app.feature.biometric.data.VoiceTemplateStatusResponse
import com.hmiot.app.feature.biometric.domain.BiometricRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BiometricViewModel(
    private val repository: BiometricRepository
) : ViewModel() {
    private val _state = MutableStateFlow(BiometricUiState())
    val state: StateFlow<BiometricUiState> = _state.asStateFlow()

    init {
        repository.biometricAuditLogs.onEach { logs ->
            _state.update { it.copy(auditLogs = logs) }
        }.launchIn(viewModelScope)
        viewModelScope.launch {
            _state.update { it.copy(hasProtectionPassphrase = repository.hasProtectionPassphrase()) }
        }
    }

    fun updateVoiceSample(value: String) = _state.update { it.copy(voiceSampleBase64 = value) }
    fun updateDynamicRequestId(value: String) = _state.update { it.copy(dynamicRequestId = value) }
    fun updatePassphrase(value: String) = _state.update { it.copy(passphrase = value) }
    fun updateFaceRequestId(value: String) = _state.update { it.copy(faceRequestId = value) }

    fun collectVoiceSample() {
        viewModelScope.launch {
            _state.update { it.copy(info = "正在采集3秒声纹样本...", error = null) }
            runCatching { repository.collectVoiceSampleBase64(3) }
                .onSuccess { sample ->
                    _state.update { it.copy(voiceSampleBase64 = sample, info = "声纹样本采集完成（WAV Base64）") }
                    repository.appendAudit("COLLECT", "VOICEPRINT", "LOCAL_CAPTURE", "SUCCESS", "采集3秒WAV")
                }
                .onFailure { ex ->
                    _state.update { it.copy(error = "声纹采集失败: ${ex.message}") }
                    repository.appendAudit("COLLECT", "VOICEPRINT", "LOCAL_CAPTURE", "FAILED", ex.message ?: "采集失败")
                }
        }
    }

    fun setFingerprintResult(success: Boolean, message: String) {
        _state.update { it.copy(fingerprintPassed = success, info = message, error = null) }
        viewModelScope.launch {
            repository.appendAudit("VERIFY", "FINGERPRINT", "LOCAL_MATCH", if (success) "SUCCESS" else "FAILED", message)
        }
    }

    fun setupProtectionPassphrase() {
        val pass = _state.value.passphrase.trim()
        if (pass.length < 6) {
            _state.update { it.copy(error = "保护口令至少6位") }
            return
        }
        viewModelScope.launch {
            repository.setProtectionPassphrase(pass)
            repository.appendAudit("KEY_PROTECT", "BIOMETRIC_KEY", "LOCAL", "SUCCESS", "设置口令保护")
            _state.update { it.copy(info = "已设置生物模板保护口令", error = null, hasProtectionPassphrase = true, passphrase = "") }
        }
    }

    private suspend fun ensureUnlockedForSensitiveAction(): Boolean {
        if (!repository.hasProtectionPassphrase()) return true
        val pass = _state.value.passphrase.trim()
        if (pass.isBlank()) {
            _state.update { it.copy(error = "请输入保护口令后再执行该操作") }
            return false
        }
        val ok = repository.verifyProtectionPassphrase(pass)
        if (!ok) {
            repository.appendAudit("KEY_VERIFY", "BIOMETRIC_KEY", "LOCAL", "FAILED", "口令校验失败")
            _state.update { it.copy(error = "保护口令错误") }
            return false
        }
        repository.appendAudit("KEY_VERIFY", "BIOMETRIC_KEY", "LOCAL", "SUCCESS", "口令校验通过")
        return true
    }

    fun checkVoiceHealth() {
        viewModelScope.launch {
            when (val result = repository.voiceHealth()) {
                is ApiResult.Success -> _state.update {
                    it.copy(info = "声纹服务健康=${result.data.healthy}, model=${result.data.modelVersion}", error = null)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(error = NetworkErrorMapper.toMessage(result.code, result.message, result.traceId))
                }
            }
        }
    }

    fun verifyVoice() {
        val sample = _state.value.voiceSampleBase64.trim()
        if (sample.isBlank()) {
            _state.update { it.copy(error = "请输入voice sample base64") }
            return
        }
        viewModelScope.launch {
            if (!ensureUnlockedForSensitiveAction()) return@launch
            when (val result = repository.voiceVerify(sample)) {
                is ApiResult.Success -> _state.update {
                    it.copy(info = "声纹认证 verified=${result.data.verified}, recordId=${result.data.recordId}", error = null)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(error = NetworkErrorMapper.toMessage(result.code, result.message, result.traceId))
                }
            }
            repository.appendAudit("VERIFY", "VOICEPRINT", "REMOTE_MATCH", "DONE", "执行声纹认证")
        }
    }

    fun enrollVoice() {
        val sample = _state.value.voiceSampleBase64.trim()
        if (sample.isBlank()) {
            _state.update { it.copy(error = "请先采集或输入声纹样本") }
            return
        }
        viewModelScope.launch {
            if (!ensureUnlockedForSensitiveAction()) return@launch
            when (val result = repository.voiceEnroll(sample)) {
                is ApiResult.Success -> _state.update {
                    it.copy(info = "声纹注册成功: templateId=${result.data.id}, audit=${result.data.auditStatus}", error = null)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(error = NetworkErrorMapper.toMessage(result.code, result.message, result.traceId))
                }
            }
            repository.appendAudit("ENROLL", "VOICEPRINT", "REMOTE_TEMPLATE", "DONE", "执行声纹模板建立")
        }
    }

    fun recollectVoice() {
        val sample = _state.value.voiceSampleBase64.trim()
        if (sample.isBlank()) {
            _state.update { it.copy(error = "请先采集或输入声纹样本") }
            return
        }
        viewModelScope.launch {
            if (!ensureUnlockedForSensitiveAction()) return@launch
            when (val result = repository.voiceRecollect(sample)) {
                is ApiResult.Success -> _state.update {
                    it.copy(info = "声纹重采成功: templateId=${result.data.id}, version=${result.data.versionNo}", error = null)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(error = NetworkErrorMapper.toMessage(result.code, result.message, result.traceId))
                }
            }
            repository.appendAudit("RECOLLECT", "VOICEPRINT", "REMOTE_TEMPLATE", "DONE", "执行声纹模板重采")
        }
    }

    fun loadVoiceTemplates() {
        viewModelScope.launch {
            when (val result = repository.voiceTemplates()) {
                is ApiResult.Success -> _state.update {
                    it.copy(voiceTemplates = result.data, info = "模板数=${result.data.size}", error = null)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(error = NetworkErrorMapper.toMessage(result.code, result.message, result.traceId))
                }
            }
        }
    }

    fun loadVoiceAuthRecords() {
        viewModelScope.launch {
            when (val result = repository.voiceAuthRecords()) {
                is ApiResult.Success -> _state.update {
                    it.copy(voiceRecords = result.data.records, info = "声纹记录数=${result.data.records.size}", error = null)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(error = NetworkErrorMapper.toMessage(result.code, result.message, result.traceId))
                }
            }
        }
    }

    fun checkDynamicHealth() {
        viewModelScope.launch {
            when (val result = repository.dynamicHealth()) {
                is ApiResult.Success -> _state.update {
                    it.copy(info = "动态生物服务健康=${result.data.healthy}, model=${result.data.modelVersion}", error = null)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(error = NetworkErrorMapper.toMessage(result.code, result.message, result.traceId))
                }
            }
        }
    }

    fun queryDynamic() {
        viewModelScope.launch {
            when (val result = repository.dynamicQuery(_state.value.dynamicRequestId, "DYNAMIC_BIOMETRIC")) {
                is ApiResult.Success -> _state.update {
                    it.copy(info = "动态生物 matched=${result.data.matched}, recordId=${result.data.recordId}", error = null)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(error = NetworkErrorMapper.toMessage(result.code, result.message, result.traceId))
                }
            }
        }
    }

    fun loadDynamicAuthRecords() {
        viewModelScope.launch {
            when (val result = repository.dynamicAuthRecords()) {
                is ApiResult.Success -> _state.update {
                    it.copy(dynamicRecords = result.data.records, info = "动态生物记录数=${result.data.records.size}", error = null)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(error = NetworkErrorMapper.toMessage(result.code, result.message, result.traceId))
                }
            }
            repository.appendAudit("VERIFY", "DYNAMIC_BIOMETRIC", "REMOTE_MATCH", "DONE", "执行动态生物认证")
        }
    }

    fun verifyFaceRemote() {
        viewModelScope.launch {
            if (!ensureUnlockedForSensitiveAction()) return@launch
            when (val result = repository.dynamicQuery(_state.value.faceRequestId, "FACE")) {
                is ApiResult.Success -> _state.update {
                    it.copy(info = "人脸远端认证 matched=${result.data.matched}, recordId=${result.data.recordId}", error = null)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(error = NetworkErrorMapper.toMessage(result.code, result.message, result.traceId))
                }
            }
            repository.appendAudit("VERIFY", "FACE", "REMOTE_MATCH", "DONE", "执行人脸远端认证")
        }
    }
}

data class BiometricUiState(
    val fingerprintPassed: Boolean = false,
    val voiceSampleBase64: String = "",
    val dynamicRequestId: String = "",
    val faceRequestId: String = "",
    val passphrase: String = "",
    val hasProtectionPassphrase: Boolean = false,
    val voiceTemplates: List<VoiceTemplateStatusResponse> = emptyList(),
    val voiceRecords: List<VoiceAuthRecordStatusResponse> = emptyList(),
    val dynamicRecords: List<DynamicAuthRecordStatusResponse> = emptyList(),
    val auditLogs: List<BiometricAuditLog> = emptyList(),
    val info: String? = null,
    val error: String? = null
)

class BiometricViewModelFactory(
    private val repository: BiometricRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BiometricViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BiometricViewModel(repository) as T
        }
        throw IllegalArgumentException("Unsupported ViewModel: $modelClass")
    }
}
