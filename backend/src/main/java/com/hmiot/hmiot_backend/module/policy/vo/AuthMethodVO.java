package com.hmiot.hmiot_backend.module.policy.vo;

import java.time.LocalDateTime;

public record AuthMethodVO(
        Long id,
        String methodCode,
        String methodName,
        String methodType,
        String compareMode,
        Boolean enabled,
        Integer sortOrder,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
