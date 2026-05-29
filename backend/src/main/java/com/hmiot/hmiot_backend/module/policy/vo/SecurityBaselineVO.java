package com.hmiot.hmiot_backend.module.policy.vo;

import java.time.LocalDateTime;

public record SecurityBaselineVO(
        Long id,
        String baselineCode,
        String baselineName,
        String targetType,
        String targetScope,
        String ruleConfig,
        Integer riskLevel,
        String actionOnHit,
        String status,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
