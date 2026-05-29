package com.hmiot.hmiot_backend.module.policy.dto;

import jakarta.validation.constraints.NotBlank;

public record PolicyResolveRequest(
        @NotBlank(message = "sceneType不能为空") String sceneType,
        String appCode,
        Long appResourceId,
        Long userId,
        Long terminalId,
        String clientType,
        Integer currentTrustLevel,
        String authMethod
) {
}
