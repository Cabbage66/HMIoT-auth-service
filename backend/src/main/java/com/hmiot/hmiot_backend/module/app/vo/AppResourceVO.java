package com.hmiot.hmiot_backend.module.app.vo;

import java.time.LocalDateTime;

public record AppResourceVO(
        Long id,
        String appCode,
        String appName,
        String resourceType,
        String accessUrl,
        Integer securityLevel,
        Integer requiredTrustLevel,
        String status,
        String description,
        String metadata,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
