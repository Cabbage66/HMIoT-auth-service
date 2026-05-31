package com.hmiot.hmiot_backend.module.biometric.vo;

import java.time.LocalDateTime;

public record FaceTemplateStatusVO(
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
    public static FaceTemplateStatusVO from(BiometricTemplateVO vo) {
        return new FaceTemplateStatusVO(
                vo.id(),
                vo.biometricType(),
                vo.modelSource(),
                vo.modelVersion(),
                vo.auditStatus(),
                vo.templateStatus(),
                vo.versionNo(),
                vo.submittedAt(),
                vo.reviewedAt(),
                vo.revokedAt(),
                vo.rejectReason(),
                vo.createdAt(),
                vo.updatedAt()
        );
    }
}

