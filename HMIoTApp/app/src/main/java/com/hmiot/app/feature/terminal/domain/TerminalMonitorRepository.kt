package com.hmiot.app.feature.terminal.domain

import com.hmiot.app.core.network.model.ApiEnvelope
import com.hmiot.app.core.network.model.ApiResult
import com.hmiot.app.core.storage.datastore.AppPreferenceStore
import com.hmiot.app.core.storage.datastore.PendingTerminalEvent
import com.hmiot.app.feature.terminal.data.TerminalEventReportRequest
import com.hmiot.app.feature.terminal.data.TerminalEventResponse
import com.hmiot.app.feature.terminal.data.TerminalMonitorApi
import com.hmiot.app.feature.terminal.data.TrustCheckRequest
import com.hmiot.app.feature.terminal.data.TrustCheckResponse
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException

class TerminalMonitorRepository(
    private val api: TerminalMonitorApi,
    private val store: AppPreferenceStore,
    private val collector: TerminalStatusCollector,
    private val cleaner: TerminalPayloadCleaner,
    private val fileLogger: TerminalSecurityFileLogger
) {
    val pendingEvents: Flow<List<PendingTerminalEvent>> = store.pendingTerminalEvents

    suspend fun collectAndQueue(triggerScene: String = "PERIODIC", appCode: String? = null): ApiResult<Int> {
        val clean = cleaner.clean(collector.collect(triggerScene, appCode))
        val localLogPath = fileLogger.append(clean)
        val logged = if (localLogPath.isBlank()) clean else clean.copy(eventPayload = clean.eventPayload + ("clientLogPath" to localLogPath))
        val existing = store.pendingTerminalEventsSnapshot()
        val updated = (existing + PendingTerminalEvent(logged.eventType, logged.eventLevel, logged.eventTime, logged.eventPayload)).takeLast(200)
        store.updatePendingTerminalEvents(updated)
        return ApiResult.Success(updated.size, null)
    }

    suspend fun flushQueue(): ApiResult<Int> {
        val pending = store.pendingTerminalEventsSnapshot()
        if (pending.isEmpty()) return ApiResult.Success(0, null)
        val remain = mutableListOf<PendingTerminalEvent>()
        var success = 0
        for (e in pending) {
            when (runApi {
                api.report(
                    TerminalEventReportRequest(
                        eventType = e.eventType,
                        eventLevel = e.eventLevel,
                        eventTime = e.eventTime,
                        eventPayload = e.eventPayload
                    )
                )
            }) {
                is ApiResult.Success -> success++
                is ApiResult.Failure -> remain.add(e)
            }
        }
        store.updatePendingTerminalEvents(remain)
        return ApiResult.Success(success, null)
    }

    suspend fun recentEvents(): ApiResult<List<TerminalEventResponse>> = runApi { api.recent() }
    suspend fun trustCheck(): ApiResult<TrustCheckResponse> = runApi { api.trustCheck(TrustCheckRequest()) }
    fun collectSecuritySnapshot(): TerminalSecuritySnapshot = collector.collectSecuritySnapshot()
    fun localLogFilePath(): String = fileLogger.fixedLogPath()

    private suspend fun <T> runApi(block: suspend () -> ApiEnvelope<T>): ApiResult<T> {
        return try {
            val envelope = block()
            if (envelope.code == "0" && envelope.data != null) {
                ApiResult.Success(envelope.data, envelope.traceId)
            } else {
                ApiResult.Failure(envelope.code, envelope.message, envelope.traceId)
            }
        } catch (http: HttpException) {
            ApiResult.Failure(http.code().toString(), "HTTP ${http.code()}", null)
        } catch (ex: Exception) {
            ApiResult.Failure("NETWORK_ERROR", ex.message ?: "网络异常", null)
        }
    }
}
