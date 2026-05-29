package com.hmiot.app.feature.policy.domain

import com.google.gson.Gson
import com.hmiot.app.core.network.model.ApiEnvelope
import com.hmiot.app.core.network.model.ApiResult
import com.hmiot.app.core.storage.datastore.AppPreferenceStore
import com.hmiot.app.feature.policy.data.ClientPolicyBundleResponse
import com.hmiot.app.feature.policy.data.PolicyAckRequest
import com.hmiot.app.feature.policy.data.PolicyApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import retrofit2.HttpException

class PolicyRepository(
    private val api: PolicyApi,
    private val store: AppPreferenceStore,
    private val gson: Gson = Gson()
) {
    val localBundle: Flow<ClientPolicyBundleResponse?> = store.localPolicyBundle.map { json ->
        if (json.isNullOrBlank()) null else runCatching { gson.fromJson(json, ClientPolicyBundleResponse::class.java) }.getOrNull()
    }

    suspend fun syncPolicies(sceneType: String? = null, appCode: String? = null): ApiResult<ClientPolicyBundleResponse> {
        val result = runApi { api.clientPolicies(sceneType, appCode) }
        if (result is ApiResult.Success) {
            store.updateLocalPolicyBundle(gson.toJson(result.data))
            for (r in result.data.pendingPublishRecords) {
                runApi { api.ack(r.id, PolicyAckRequest(executeResult = "SUCCESS")) }
            }
        }
        return result
    }

    suspend fun clearLocal() {
        store.updateLocalPolicyBundle(null)
    }

    suspend fun localBundleValue(): ClientPolicyBundleResponse? = localBundle.first()

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
