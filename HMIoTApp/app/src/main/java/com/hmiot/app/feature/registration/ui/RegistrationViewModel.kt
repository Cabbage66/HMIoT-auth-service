package com.hmiot.app.feature.registration.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hmiot.app.core.network.NetworkErrorMapper
import com.hmiot.app.core.network.model.ApiResult
import com.hmiot.app.feature.registration.data.RegistrationProgressItem
import com.hmiot.app.feature.registration.data.RegistrationSubmitRequest
import com.hmiot.app.feature.registration.domain.RegistrationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegistrationViewModel(
    private val repository: RegistrationRepository
) : ViewModel() {
    private val _state = MutableStateFlow(RegistrationUiState())
    val state: StateFlow<RegistrationUiState> = _state.asStateFlow()

    fun updateUsername(value: String) = _state.update { it.copy(username = value) }
    fun updatePassword(value: String) = _state.update { it.copy(password = value) }
    fun updateRealName(value: String) = _state.update { it.copy(realName = value) }
    fun updatePhone(value: String) = _state.update { it.copy(phone = value) }
    fun updateEmail(value: String) = _state.update { it.copy(email = value) }
    fun updateIdCardNo(value: String) = _state.update { it.copy(idCardNo = value) }

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
                idCardNo = s.idCardNo.ifBlank { null }
            )
            when (val result = repository.submit(request)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        loading = false,
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
                        progressRecords = result.data.records,
                        info = "查询完成：共${result.data.records.size}条，traceId=${result.traceId ?: "-"}"
                    )
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = NetworkErrorMapper.toMessage(result.code, result.message, result.traceId))
                }
            }
        }
    }
}

data class RegistrationUiState(
    val username: String = "",
    val password: String = "",
    val realName: String = "",
    val phone: String = "",
    val email: String = "",
    val idCardNo: String = "",
    val loading: Boolean = false,
    val info: String? = null,
    val error: String? = null,
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
