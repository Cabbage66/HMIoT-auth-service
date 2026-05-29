package com.hmiot.hmiot_backend.module.biometric.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VoiceprintAuthRecordStatusVO(
        Long id,
        String biometricType,
        Long templateId,
        BigDecimal score,
        BigDecimal threshold,
        String authResult,
        String failReason,
        String modelSource,
        LocalDateTime createdAt
) {
    public static VoiceprintAuthRecordStatusVO from(BiometricAuthRecordVO record) {
        return new VoiceprintAuthRecordStatusVO(
                record.id(),
                record.biometricType(),
                record.templateId(),
                record.score(),
                record.threshold(),
                record.authResult(),
                record.failReason(),
                record.modelSource(),
                record.createdAt()
        );
    }
}
