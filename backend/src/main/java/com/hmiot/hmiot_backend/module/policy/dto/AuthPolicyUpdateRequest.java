package com.hmiot.hmiot_backend.module.policy.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthPolicyUpdateRequest(
        @NotBlank(message = "策略名称不能为空") String policyName,
        @NotBlank(message = "场景类型不能为空") String sceneType,
        Long appResourceId,
        String userScope,
        String deviceScope,
        @NotBlank(message = "认证链路不能为空") String authChain,
        Integer minTrustLevel,
        Integer sessionTtlSeconds,
        String actionOnFail,
        String status
) {
}
