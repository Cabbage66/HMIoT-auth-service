package com.hmiot.app.feature.access.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hmiot.app.core.network.NetworkErrorMapper
import com.hmiot.app.core.network.model.ApiResult
import com.hmiot.app.feature.access.data.AppResource
import com.hmiot.app.feature.access.domain.AccessRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccessCenterViewModel(
    private val repository: AccessRepository
) : ViewModel() {
    private val _state = MutableStateFlow(AccessCenterUiState())
    val state: StateFlow<AccessCenterUiState> = _state.asStateFlow()

    fun loadApps() {
        viewModelScope.launch {
            when (val result = repository.listApps()) {
                is ApiResult.Success -> _state.update { it.copy(apps = result.data, info = "应用数=${result.data.size}", error = null) }
                is ApiResult.Failure -> _state.update { it.copy(error = NetworkErrorMapper.toMessage(result.code, result.message, result.traceId)) }
            }
        }
    }

    fun startAccess(app: AppResource) {
        viewModelScope.launch {
            val trust = repository.checkTrust(app)
            val trustLevel = (trust as? ApiResult.Success)?.data?.trustLevel ?: 0
            val policyDecision = repository.evaluatePolicy(app, authMethod = "CERTIFICATE_LOGIN", trustLevel = trustLevel)
            if (!policyDecision.allowed) {
                _state.update { it.copy(info = "策略不满足，需二次认证: ${policyDecision.reason}", error = null) }
                return@launch
            }
            when (val access = repository.ssoAccess(app)) {
                is ApiResult.Success -> {
                    if (access.data.success) {
                        _state.update {
                            it.copy(
                                info = "访问成功: app=${app.appCode}, ticket=${access.data.ticket?.ticketId ?: "-"}, url=${access.data.accessUrl ?: "-"}",
                                error = null
                            )
                        }
                    } else {
                        _state.update { it.copy(info = "访问失败，建议触发二次认证: ${access.data.message}", error = null) }
                    }
                }
                is ApiResult.Failure -> _state.update { it.copy(error = NetworkErrorMapper.toMessage(access.code, access.message, access.traceId)) }
            }
        }
    }
}

data class AccessCenterUiState(
    val apps: List<AppResource> = emptyList(),
    val info: String? = null,
    val error: String? = null
)

class AccessCenterViewModelFactory(
    private val repository: AccessRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AccessCenterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AccessCenterViewModel(repository) as T
        }
        throw IllegalArgumentException("Unsupported ViewModel: $modelClass")
    }
}
