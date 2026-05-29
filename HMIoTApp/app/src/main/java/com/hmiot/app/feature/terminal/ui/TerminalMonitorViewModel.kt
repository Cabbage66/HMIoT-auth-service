package com.hmiot.app.feature.terminal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hmiot.app.core.network.NetworkErrorMapper
import com.hmiot.app.core.network.model.ApiResult
import com.hmiot.app.core.storage.datastore.PendingTerminalEvent
import com.hmiot.app.feature.terminal.data.TerminalEventResponse
import com.hmiot.app.feature.terminal.data.TrustCheckResponse
import com.hmiot.app.feature.terminal.domain.TerminalMonitorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TerminalMonitorViewModel(
    private val repository: TerminalMonitorRepository
) : ViewModel() {
    private val _state = MutableStateFlow(TerminalMonitorUiState())
    val state: StateFlow<TerminalMonitorUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.pendingEvents.collect { events ->
                _state.update { it.copy(pending = events) }
            }
        }
    }

    fun collectAndQueue() {
        viewModelScope.launch {
            when (val result = repository.collectAndQueue()) {
                is ApiResult.Success -> _state.update { it.copy(info = "采集成功，待上报队列=${result.data}", error = null) }
                is ApiResult.Failure -> _state.update { it.copy(error = NetworkErrorMapper.toMessage(result.code, result.message, result.traceId)) }
            }
        }
    }

    fun flushQueue() {
        viewModelScope.launch {
            when (val result = repository.flushQueue()) {
                is ApiResult.Success -> _state.update { it.copy(info = "上报成功条数=${result.data}", error = null) }
                is ApiResult.Failure -> _state.update { it.copy(error = NetworkErrorMapper.toMessage(result.code, result.message, result.traceId)) }
            }
        }
    }

    fun loadRecent() {
        viewModelScope.launch {
            when (val result = repository.recentEvents()) {
                is ApiResult.Success -> _state.update { it.copy(recent = result.data, info = "最近事件=${result.data.size}", error = null) }
                is ApiResult.Failure -> _state.update { it.copy(error = NetworkErrorMapper.toMessage(result.code, result.message, result.traceId)) }
            }
        }
    }

    fun checkTrust() {
        viewModelScope.launch {
            when (val result = repository.trustCheck()) {
                is ApiResult.Success -> _state.update { it.copy(trust = result.data, info = "信任检查完成 trusted=${result.data.trusted}", error = null) }
                is ApiResult.Failure -> _state.update { it.copy(error = NetworkErrorMapper.toMessage(result.code, result.message, result.traceId)) }
            }
        }
    }
}

data class TerminalMonitorUiState(
    val pending: List<PendingTerminalEvent> = emptyList(),
    val recent: List<TerminalEventResponse> = emptyList(),
    val trust: TrustCheckResponse? = null,
    val info: String? = null,
    val error: String? = null
)

class TerminalMonitorViewModelFactory(
    private val repository: TerminalMonitorRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TerminalMonitorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TerminalMonitorViewModel(repository) as T
        }
        throw IllegalArgumentException("Unsupported ViewModel: $modelClass")
    }
}
