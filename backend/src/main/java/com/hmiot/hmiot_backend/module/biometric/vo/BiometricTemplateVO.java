package com.hmiot.hmiot_backend.module.biometric.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BiometricTemplateVO(
        Long id,
        Long userId,
        String username,
        String realName,
        String biometricType,
        String templateDigest,
        String templateRef,
        String modelSource,
        String modelVersion,
        BigDecimal threshold,
        String auditStatus,
        String templateStatus,
        Integer versionNo,
        LocalDateTime submittedAt,
        Long reviewedBy,
        LocalDateTime reviewedAt,
        LocalDateTime revokedAt,
        String rejectReason,
        String metadata,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
