package com.hmiot.hmiot_backend.module.biometric.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FaceAuthRecordStatusVO(
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
    public static FaceAuthRecordStatusVO from(BiometricAuthRecordVO vo) {
        return new FaceAuthRecordStatusVO(
                vo.id(),
                vo.biometricType(),
                vo.templateId(),
                vo.score(),
                vo.threshold(),
                vo.authResult(),
                vo.failReason(),
                vo.modelSource(),
                vo.createdAt()
        );
    }
}

