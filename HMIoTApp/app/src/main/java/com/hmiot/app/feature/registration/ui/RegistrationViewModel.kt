package com.hmiot.app.feature.registration.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hmiot.app.core.storage.datastore.LocalRegistrationRecord
import com.hmiot.app.core.network.NetworkErrorMapper
import com.hmiot.app.core.network.model.ApiResult
import com.hmiot.app.feature.registration.data.RegistrationProgressItem
import com.hmiot.app.feature.registration.data.RegistrationSubmitRequest
import com.hmiot.app.feature.registration.domain.RegistrationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.OffsetDateTime

class RegistrationViewModel(
    private val repository: RegistrationRepository
) : ViewModel() {
    private val _state = MutableStateFlow(RegistrationUiState())
    val state: StateFlow<RegistrationUiState> = _state.asStateFlow()

    init {
        repository.localRegistrationRecords.onEach { records ->
            _state.update { it.copy(localRecords = records) }
        }.launchIn(viewModelScope)
    }

    fun updateUsername(value: String) = _state.update { it.copy(username = value) }
    fun updatePassword(value: String) = _state.update { it.copy(password = value) }
    fun updateRealName(value: String) = _state.update { it.copy(realName = value) }
    fun updatePhone(value: String) = _state.update { it.copy(phone = value) }
    fun updateEmail(value: String) = _state.update { it.copy(email = value) }
    fun updateIdCardNo(value: String) = _state.update { it.copy(idCardNo = value) }
    fun updateVoiceprintAudio(value: String) = _state.update { it.copy(voiceprintAudioBase64 = value) }
    fun updateFaceImage(value: String) = _state.update { it.copy(faceImageBase64 = value) }

    fun submit() {
        val s = _state.value
        if (s.username.length < 4 || s.password.length < 8 || s.realName.isBlank()) {
            _state.update { it.copy(error = "请填写完整注册信息（用户名>=4，密码>=8，姓名必填）") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, info = null) }
            val request = RegistrationSubmitRequest(
                username = s.username.trim(),
                password = s.password,
                realName = s.realName.trim(),
                phone = s.phone.ifBlank { null },
                email = s.email.ifBlank { null },
                idCardNo = s.idCardNo.ifBlank { null },
                voiceprintAudioBase64 = s.voiceprintAudioBase64.ifBlank { null },
                voiceprintAudioFormat = if (s.voiceprintAudioBase64.isBlank()) null else "wav",
                voiceprintSampleRate = if (s.voiceprintAudioBase64.isBlank()) null else 16000,
                faceImageBase64 = s.faceImageBase64.ifBlank { null },
                faceImageFormat = if (s.faceImageBase64.isBlank()) null else "jpeg"
            )
            when (val result = repository.submit(request)) {
                is ApiResult.Success -> _state.update {
                    val submittedAt = result.data.submittedAt ?: OffsetDateTime.now().toString()
                    it.copy(
                        loading = false,
                        username = result.data.username,
                        progressRecords = mergeRecords(
                            remote = it.progressRecords,
                            local = repositoryLocalRecordSnapshot(
                                result.data.registrationId,
                                result.data.userId,
                                result.data.username,
                                s.realName.trim(),
                                s.phone.ifBlank { null },
                                s.email.ifBlank { null },
                                result.data.status,
                                submittedAt
                            )
                        ),
                        info = "提交成功：registrationId=${result.data.registrationId}, status=${result.data.status}, traceId=${result.traceId ?: "-"}"
                    )
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = NetworkErrorMapper.toMessage(result.code, result.message, result.traceId))
                }
            }
        }
    }

    fun queryProgress() {
        val username = _state.value.username.trim()
        if (username.isBlank()) {
            _state.update { it.copy(error = "请输入用户名后再查询进度") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, info = null) }
            when (val result = repository.queryProgress(username)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        loading = false,
                        progressRecords = mergeRecords(result.data.records, it.localRecords.filter { record -> record.username == username }.map { record -> record.toProgressItem() }),
                        info = "查询完成：共${result.data.records.size}条，本机记录${it.localRecords.count { record -> record.username == username }}条，traceId=${result.traceId ?: "-"}"
                    )
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = NetworkErrorMapper.toMessage(result.code, result.message, result.traceId))
                }
            }
        }
    }

    fun loadLocalRecords(username: String? = null) {
        val scoped = if (username.isNullOrBlank()) {
            _state.value.localRecords
        } else {
            _state.value.localRecords.filter { it.username == username.trim() }
        }
        _state.update {
            it.copy(
                progressRecords = scoped.map { record -> record.toProgressItem() },
                info = if (scoped.isEmpty()) "本机暂无注册申请记录" else "已加载本机注册申请记录：${scoped.size}条",
                error = null
            )
        }
    }

    fun refreshLocalProgress() {
        val local = _state.value.localRecords
        if (local.isEmpty()) {
            loadLocalRecords()
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, info = null) }
            val remoteRecords = mutableListOf<RegistrationProgressItem>()
            val failures = mutableListOf<String>()
            local.map { it.username }.distinct().forEach { username ->
                when (val result = repository.queryProgress(username)) {
                    is ApiResult.Success -> remoteRecords.addAll(result.data.records)
                    is ApiResult.Failure -> failures.add(result.message)
                }
            }
            _state.update {
                val localItems = local.map { record -> record.toProgressItem() }
                it.copy(
                    loading = false,
                    progressRecords = mergeRecords(remoteRecords, localItems),
                    info = "已刷新本机注册申请记录：${local.size}条",
                    error = failures.firstOrNull()
                )
            }
        }
    }

    private fun repositoryLocalRecordSnapshot(
        registrationId: Long,
        userId: Long,
        username: String,
        realName: String,
        phone: String?,
        email: String?,
        status: String,
        submittedAt: String
    ): List<RegistrationProgressItem> {
        return listOf(
            RegistrationProgressItem(
                registrationId = registrationId,
                username = username,
                realName = realName,
                phone = phone,
                email = email,
                status = status,
                rejectReason = null,
                reviewedAt = null,
                createdAt = submittedAt
            )
        )
    }

    private fun mergeRecords(remote: List<RegistrationProgressItem>, local: List<RegistrationProgressItem>): List<RegistrationProgressItem> {
        return (remote + local)
            .distinctBy { it.registrationId }
            .sortedByDescending { it.createdAt }
    }
}

private fun LocalRegistrationRecord.toProgressItem(): RegistrationProgressItem {
    return RegistrationProgressItem(
        registrationId = registrationId,
        username = username,
        realName = realName,
        phone = phone,
        email = email,
        status = status,
        rejectReason = rejectReason,
        reviewedAt = null,
        createdAt = submittedAt
    )
}

data class RegistrationUiState(
    val username: String = "",
    val password: String = "",
    val realName: String = "",
    val phone: String = "",
    val email: String = "",
    val idCardNo: String = "",
    val voiceprintAudioBase64: String = "",
    val faceImageBase64: String = "",
    val loading: Boolean = false,
    val info: String? = null,
    val error: String? = null,
    val localRecords: List<LocalRegistrationRecord> = emptyList(),
    val progressRecords: List<RegistrationProgressItem> = emptyList()
)

class RegistrationViewModelFactory(
    private val repository: RegistrationRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegistrationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RegistrationViewModel(repository) as T
        }
        throw IllegalArgumentException("Unsupported ViewModel: $modelClass")
    }
}
