package com.hmiot.app.feature.trust.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hmiot.app.core.network.NetworkErrorMapper
import com.hmiot.app.core.network.model.ApiResult
import com.hmiot.app.core.storage.datastore.TrustGuardState
import com.hmiot.app.feature.trust.domain.TrustGuardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TrustCenterViewModel(
    private val repository: TrustGuardRepository
) : ViewModel() {
    private val _state = MutableStateFlow(TrustCenterUiState())
    val state: StateFlow<TrustCenterUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.state.collect { s ->
                _state.update { it.copy(guard = s) }
            }
        }
    }

    fun runTrustCheck() {
        viewModelScope.launch {
            when (val result = repository.evaluateAndUpdate()) {
                is ApiResult.Success -> _state.update { it.copy(info = "信任评估完成，blocked=${result.data.blocked}", error = null) }
                is ApiResult.Failure -> _state.update { it.copy(error = NetworkErrorMapper.toMessage(result.code, result.message, result.traceId)) }
            }
        }
    }

    fun clearBlock() {
        viewModelScope.launch {
            repository.clearBlock()
            _state.update { it.copy(info = "已手动解除阻断", error = null) }
        }
    }
}

data class TrustCenterUiState(
    val guard: TrustGuardState = TrustGuardState(false, "", ""),
    val info: String? = null,
    val error: String? = null
)

class TrustCenterViewModelFactory(
    private val repository: TrustGuardRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrustCenterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TrustCenterViewModel(repository) as T
        }
        throw IllegalArgumentException("Unsupported ViewModel: $modelClass")
    }
}
