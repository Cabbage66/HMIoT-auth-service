package com.hmiot.hmiot_backend.module.policy.vo;

import com.hmiot.hmiot_backend.module.app.vo.AppResourceVO;

import java.time.LocalDateTime;

public record AuthPolicyVO(
        Long id,
        String policyCode,
        String policyName,
        String sceneType,
        Long appResourceId,
        String userScope,
        String deviceScope,
        String authChain,
        Integer minTrustLevel,
        Integer sessionTtlSeconds,
        String actionOnFail,
        String status,
        Integer versionNo,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        AppResourceVO appResource
) {
}
