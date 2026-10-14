package com.hmiot.app.feature.release.domain

import com.hmiot.app.core.network.model.ApiResult
import com.hmiot.app.feature.access.domain.AccessRepository
import com.hmiot.app.feature.policy.domain.PolicyRepository
import com.hmiot.app.feature.terminal.domain.TerminalMonitorRepository
import com.hmiot.app.feature.trust.domain.TrustGuardRepository

class ReleaseValidationRepository(
    private val policyRepository: PolicyRepository,
    private val accessRepository: AccessRepository,
    private val terminalMonitorRepository: TerminalMonitorRepository,
    private val trustGuardRepository: TrustGuardRepository
) {
    suspend fun runAllChecks(): List<ReleaseCheckItem> {
        val items = mutableListOf<ReleaseCheckItem>()

        items += when (policyRepository.syncPolicies()) {
            is ApiResult.Success -> ReleaseCheckItem("策略同步", true, "成功")
            is ApiResult.Failure -> ReleaseCheckItem("策略同步", false, "失败")
        }

        val apps = when (val appResult = accessRepository.listApps()) {
            is ApiResult.Success -> {
                items += ReleaseCheckItem("应用资源加载", true, "应用数=${appResult.data.size}")
                appResult.data
            }
            is ApiResult.Failure -> {
                items += ReleaseCheckItem("应用资源加载", false, "失败")
                emptyList()
            }
        }

        items += when (val trust = trustGuardRepository.evaluateAndUpdate("RELEASE_VALIDATION")) {
            is ApiResult.Success -> ReleaseCheckItem("信任评估", true, "blocked=${trust.data.blocked}")
            is ApiResult.Failure -> ReleaseCheckItem("信任评估", false, "失败")
        }

        items += when (val flush = terminalMonitorRepository.flushQueue()) {
            is ApiResult.Success -> ReleaseCheckItem("终端事件上报", true, "成功条数=${flush.data}")
            is ApiResult.Failure -> ReleaseCheckItem("终端事件上报", false, "失败")
        }

        if (apps.isNotEmpty()) {
            items += when (val access = accessRepository.ssoAccess(apps.first())) {
                is ApiResult.Success -> ReleaseCheckItem("SSO访问演练", access.data.success, access.data.message)
                is ApiResult.Failure -> ReleaseCheckItem("SSO访问演练", false, "失败")
            }
        } else {
            items += ReleaseCheckItem("SSO访问演练", false, "无可用应用")
        }
        return items
    }
}

data class ReleaseCheckItem(
    val name: String,
    val passed: Boolean,
    val detail: String
)
