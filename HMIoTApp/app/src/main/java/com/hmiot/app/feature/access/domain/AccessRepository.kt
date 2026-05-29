package com.hmiot.app.feature.access.domain

import com.hmiot.app.core.network.model.ApiEnvelope
import com.hmiot.app.core.network.model.ApiResult
import com.hmiot.app.feature.access.data.AccessApi
import com.hmiot.app.feature.access.data.AppResource
import com.hmiot.app.feature.access.data.IdentityTicket
import com.hmiot.app.feature.access.data.SsoAccessResponse
import com.hmiot.app.feature.access.data.TicketIssueRequest
import com.hmiot.app.feature.access.data.TrustCheckRequest
import com.hmiot.app.feature.access.data.TrustCheckResponse
import com.hmiot.app.feature.policy.data.PolicyResolveResponse
import com.hmiot.app.feature.policy.domain.PolicyDecision
import com.hmiot.app.feature.policy.domain.PolicyEngine
import com.hmiot.app.feature.policy.domain.PolicyRepository
import com.hmiot.app.feature.trust.domain.TrustGuardRepository
import retrofit2.HttpException

class AccessRepository(
    private val api: AccessApi,
    private val policyRepository: PolicyRepository,
    private val policyEngine: PolicyEngine,
    private val trustGuardRepository: TrustGuardRepository
) {
    suspend fun listApps(): ApiResult<List<AppResource>> =
        runApi { api.apps() }.map { it.records }

    suspend fun checkTrust(app: AppResource): ApiResult<TrustCheckResponse> =
        runApi { api.trustCheck(TrustCheckRequest(appCode = app.appCode, appResourceId = app.id)) }

    suspend fun issueTicket(app: AppResource, trustLevel: Int): ApiResult<IdentityTicket> =
        runApi { api.issueTicket(TicketIssueRequest(appCode = app.appCode, audienceSystem = "APP_SYSTEM", trustLevel = trustLevel)) }

    suspend fun ssoAccess(app: AppResource): ApiResult<SsoAccessResponse> =
        runApi { api.ssoAccess(com.hmiot.app.feature.access.data.SsoAccessRequest(appCode = app.appCode, targetSystem = "BUSINESS_APP")) }

    suspend fun evaluatePolicy(app: AppResource, authMethod: String, trustLevel: Int): PolicyDecision {
        val guard = trustGuardRepository.currentState()
        if (guard.blocked) {
            return PolicyDecision(false, "风险阻断中: ${guard.reason}")
        }
        val bundle = policyRepository.localBundleValue()
        val accessPolicy: PolicyResolveResponse = bundle?.policies?.firstOrNull { it.sceneType == "APP_ACCESS" && (it.appCode == null || it.appCode == app.appCode) }
            ?: return PolicyDecision(false, "未找到本地访问策略，请先同步策略")
        return policyEngine.evaluateAppAccess(accessPolicy, authMethod, trustLevel)
    }

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

private fun <T, R> ApiResult<T>.map(mapper: (T) -> R): ApiResult<R> {
    return when (this) {
        is ApiResult.Success -> ApiResult.Success(mapper(this.data), this.traceId)
        is ApiResult.Failure -> this
    }
}
