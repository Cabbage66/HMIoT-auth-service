package com.hmiot.hmiot_backend.module.biometric.vo;

import java.time.LocalDateTime;

public record FingerprintTemplateStatusVO(
        Long id,
        String biometricType,
        String modelSource,
        String modelVersion,
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
    public static FingerprintTemplateStatusVO from(BiometricTemplateVO template) {
        return new FingerprintTemplateStatusVO(
                template.id(),
                template.biometricType(),
                template.modelSource(),
                template.modelVersion(),
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

