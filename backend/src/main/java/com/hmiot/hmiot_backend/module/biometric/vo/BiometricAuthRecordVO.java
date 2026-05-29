package com.hmiot.hmiot_backend.module.biometric.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BiometricAuthRecordVO(
        Long id,
        Long userId,
        String username,
        String realName,
        String biometricType,
        Long templateId,
        String requestId,
        BigDecimal score,
        BigDecimal threshold,
        String authResult,
        String failReason,
        String modelSource,
        String rawResponse,
        LocalDateTime createdAt
) {
}
