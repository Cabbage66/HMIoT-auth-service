package com.hmiot.app.feature.release.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hmiot.app.feature.release.domain.ReleaseCheckItem
import com.hmiot.app.feature.release.domain.ReleaseValidationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReleaseCenterViewModel(
    private val repository: ReleaseValidationRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ReleaseCenterUiState())
    val state: StateFlow<ReleaseCenterUiState> = _state.asStateFlow()

    fun runChecks() {
        viewModelScope.launch {
            _state.update { it.copy(running = true, summary = null) }
            val items = repository.runAllChecks()
            val pass = items.count { it.passed }
            _state.update {
                it.copy(
                    running = false,
                    items = items,
                    summary = "通过 $pass/${items.size}"
                )
            }
        }
    }
}

data class ReleaseCenterUiState(
    val running: Boolean = false,
    val items: List<ReleaseCheckItem> = emptyList(),
    val summary: String? = null
)

class ReleaseCenterViewModelFactory(
    private val repository: ReleaseValidationRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReleaseCenterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReleaseCenterViewModel(repository) as T
        }
        throw IllegalArgumentException("Unsupported ViewModel: $modelClass")
    }
}
