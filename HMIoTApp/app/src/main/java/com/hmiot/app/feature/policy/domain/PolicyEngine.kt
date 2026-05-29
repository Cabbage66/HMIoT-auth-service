package com.hmiot.app.feature.policy.domain

import com.hmiot.app.feature.policy.data.PolicyResolveResponse

class PolicyEngine {
    fun evaluateLogin(policy: PolicyResolveResponse, authMethod: String): PolicyDecision {
        if (policy.authChain.isEmpty()) {
            return PolicyDecision(false, "策略拒绝：空认证链")
        }
        val ok = policy.authChain.filter { it !in setOf("VOICEPRINT", "DYNAMIC_BIOMETRIC", "IOT_DEVICE") }
            .all { sameMethod(it, authMethod) }
        return if (ok) PolicyDecision(true, "登录策略通过") else PolicyDecision(false, "认证方式不满足策略")
    }

    fun evaluateAppAccess(policy: PolicyResolveResponse, authMethod: String, trustLevel: Int): PolicyDecision {
        val login = evaluateLogin(policy, authMethod)
        if (!login.allowed) return login
        if (trustLevel < policy.minTrustLevel) {
            return PolicyDecision(false, "信任等级不足，要求=${policy.minTrustLevel}, 当前=$trustLevel")
        }
        return PolicyDecision(true, "访问策略通过")
    }

    private fun sameMethod(required: String, actual: String): Boolean {
        if (required == actual) return true
        return (required == "CERTIFICATE" && actual == "CERTIFICATE_LOGIN") ||
            (required == "VOICEPRINT" && actual == "VOICEPRINT_LOGIN") ||
            (required == "FACE" && actual == "FACE_LOGIN") ||
            (required == "FINGERPRINT" && actual == "FINGERPRINT_LOGIN")
    }
}

data class PolicyDecision(
    val allowed: Boolean,
    val reason: String
)
