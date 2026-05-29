package com.hmiot.app.feature.policy.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hmiot.app.core.network.NetworkErrorMapper
import com.hmiot.app.core.network.model.ApiResult
import com.hmiot.app.feature.policy.data.ClientPolicyBundleResponse
import com.hmiot.app.feature.policy.domain.PolicyEngine
import com.hmiot.app.feature.policy.domain.PolicyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PolicyCenterViewModel(
    private val repository: PolicyRepository,
    private val engine: PolicyEngine
) : ViewModel() {
    private val _state = MutableStateFlow(PolicyCenterUiState())
    val state: StateFlow<PolicyCenterUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.localBundle.collect { bundle ->
                _state.update { it.copy(bundle = bundle) }
            }
        }
    }

    fun syncPolicies() {
        viewModelScope.launch {
            when (val result = repository.syncPolicies()) {
                is ApiResult.Success -> _state.update { it.copy(info = "策略同步成功，策略条数=${result.data.policies.size}", error = null) }
                is ApiResult.Failure -> _state.update { it.copy(error = NetworkErrorMapper.toMessage(result.code, result.message, result.traceId)) }
            }
        }
    }

    fun evaluateLogin(authMethod: String) {
        val login = _state.value.bundle?.policies?.firstOrNull { it.sceneType == "LOGIN" }
        if (login == null) {
            _state.update { it.copy(error = "本地无登录策略，请先同步策略") }
            return
        }
        val decision = engine.evaluateLogin(login, authMethod)
        _state.update { it.copy(info = "登录决策: allowed=${decision.allowed}, reason=${decision.reason}", error = null) }
    }

    fun evaluateAccess(authMethod: String, trustLevel: Int) {
        val access = _state.value.bundle?.policies?.firstOrNull { it.sceneType == "APP_ACCESS" }
        if (access == null) {
            _state.update { it.copy(error = "本地无访问策略，请先同步策略") }
            return
        }
        val decision = engine.evaluateAppAccess(access, authMethod, trustLevel)
        _state.update { it.copy(info = "访问决策: allowed=${decision.allowed}, reason=${decision.reason}", error = null) }
    }
}

data class PolicyCenterUiState(
    val bundle: ClientPolicyBundleResponse? = null,
    val info: String? = null,
    val error: String? = null
)

class PolicyCenterViewModelFactory(
    private val repository: PolicyRepository,
    private val engine: PolicyEngine
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PolicyCenterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PolicyCenterViewModel(repository, engine) as T
        }
        throw IllegalArgumentException("Unsupported ViewModel: $modelClass")
    }
}
