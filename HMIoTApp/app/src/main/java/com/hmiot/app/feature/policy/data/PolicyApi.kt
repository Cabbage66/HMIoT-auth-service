package com.hmiot.app.feature.policy.data

import com.hmiot.app.core.network.model.ApiEnvelope
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PolicyApi {
    @GET("policies/client")
    suspend fun clientPolicies(
        @Query("sceneType") sceneType: String? = null,
        @Query("appCode") appCode: String? = null
    ): ApiEnvelope<ClientPolicyBundleResponse>

    @POST("policies/publish-records/{id}/ack")
    suspend fun ack(
        @Path("id") id: Long,
        @Body request: PolicyAckRequest
    ): ApiEnvelope<PolicyPublishRecordResponse>
}

data class ClientPolicyBundleResponse(
    val userId: Long,
    val terminalId: Long,
    val terminalCode: String,
    val clientType: String,
    val policies: List<PolicyResolveResponse>,
    val securityBaselines: List<SecurityBaselineResponse>,
    val pendingPublishRecords: List<PolicyPublishRecordResponse>
)

data class PolicyResolveResponse(
    val sceneType: String,
    val policyId: Long?,
    val policyCode: String?,
    val policyName: String?,
    val versionNo: Int?,
    val appResourceId: Long?,
    val appCode: String?,
    val authChain: List<String>,
    val minTrustLevel: Int,
    val sessionTtlSeconds: Int,
    val actionOnFail: String,
    val matchedPolicy: Boolean,
    val decision: String
)

data class SecurityBaselineResponse(
    val id: Long,
    val baselineCode: String,
    val baselineName: String,
    val terminalType: String?,
    val baselineRules: String?,
    val riskLevel: String?,
    val actionOnRisk: String?,
    val status: String
)

data class PolicyPublishRecordResponse(
    val id: Long,
    val policyType: String,
    val policyId: Long,
    val versionNo: Int,
    val targetType: String,
    val targetId: Long?,
    val publishStatus: String,
    val executeResult: String?,
    val failReason: String?
)

data class PolicyAckRequest(
    val executeResult: String,
    val failReason: String? = null
)
