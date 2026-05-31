package com.hmiot.hmiot_backend.module.biometric.vo;

import java.time.LocalDateTime;

public record FingerprintAuthRecordStatusVO(
        Long id,
        String biometricType,
        Long templateId,
        String authResult,
        String failReason,
        String modelSource,
        LocalDateTime createdAt
) {
    public static FingerprintAuthRecordStatusVO from(BiometricAuthRecordVO record) {
        return new FingerprintAuthRecordStatusVO(
                record.id(),
                record.biometricType(),
                record.templateId(),
                record.authResult(),
                record.failReason(),
                record.modelSource(),
                record.createdAt()
        );
    }
}

