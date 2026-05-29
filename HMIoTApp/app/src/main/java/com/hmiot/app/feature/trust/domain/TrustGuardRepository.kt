package com.hmiot.app.feature.trust.domain

import com.hmiot.app.core.network.model.ApiResult
import com.hmiot.app.core.storage.datastore.AppPreferenceStore
import com.hmiot.app.core.storage.datastore.TrustGuardState
import com.hmiot.app.feature.terminal.domain.TerminalMonitorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime

class TrustGuardRepository(
    private val store: AppPreferenceStore,
    private val terminalMonitorRepository: TerminalMonitorRepository
) {
    val state: Flow<TrustGuardState> = store.trustGuardState

    suspend fun evaluateAndUpdate(): ApiResult<TrustGuardState> {
        return when (val result = terminalMonitorRepository.trustCheck()) {
            is ApiResult.Success -> {
                val blocked = !result.data.trusted
                val reason = if (blocked) (result.data.failReason ?: "信任评估未通过") else "信任评估通过"
                val state = TrustGuardState(blocked = blocked, reason = reason, updatedAt = LocalDateTime.now().toString())
                store.updateTrustGuardState(state)
                ApiResult.Success(state, null)
            }
            is ApiResult.Failure -> result
        }
    }

    suspend fun clearBlock(reason: String = "手动解除阻断") {
        store.updateTrustGuardState(TrustGuardState(false, reason, LocalDateTime.now().toString()))
    }

    suspend fun currentState(): TrustGuardState = state.first()
}
