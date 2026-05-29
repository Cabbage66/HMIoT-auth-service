package com.hmiot.hmiot_backend.module.biometric.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VoiceprintTemplateStatusVO(
        Long id,
        String biometricType,
        String modelSource,
        String modelVersion,
        BigDecimal threshold,
        String auditStatus,
        String templateStatus,
        Integer versionNo,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,
        LocalDateTime revokedAt,
        String rejectReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static VoiceprintTemplateStatusVO from(BiometricTemplateVO template) {
        return new VoiceprintTemplateStatusVO(
                template.id(),
                template.biometricType(),
                template.modelSource(),
                template.modelVersion(),
                template.threshold(),
                template.auditStatus(),
                template.templateStatus(),
                template.versionNo(),
                template.submittedAt(),
                template.reviewedAt(),
                template.revokedAt(),
                template.rejectReason(),
                template.createdAt(),
                template.updatedAt()
        );
    }
}
