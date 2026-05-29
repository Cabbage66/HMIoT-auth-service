package com.hmiot.hmiot_backend.module.biometric.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DynamicBiometricAuthRecordStatusVO(
        Long id,
        String biometricType,
        BigDecimal score,
        BigDecimal threshold,
        String authResult,
        String failReason,
        String modelSource,
        LocalDateTime createdAt
) {
    public static DynamicBiometricAuthRecordStatusVO from(BiometricAuthRecordVO record) {
        return new DynamicBiometricAuthRecordStatusVO(
                record.id(),
                record.biometricType(),
                record.score(),
                record.threshold(),
                record.authResult(),
                record.failReason(),
                record.modelSource(),
                record.createdAt()
        );
    }
}
