package com.hmiot.hmiot_backend.module.certificate.vo;

import java.time.LocalDateTime;

public record UserCertificateVO(
        Long id,
        Long userId,
        String username,
        String realName,
        String serialNumber,
        String fingerprintSha256,
        String certificatePem,
        String issuer,
        String subjectDn,
        String status,
        LocalDateTime issuedAt,
        LocalDateTime expiresAt,
        LocalDateTime revokedAt,
        String revokeReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
