package com.hmiot.hmiot_backend.module.auth.vo;

import java.time.LocalDateTime;

public record CertificateChallengeResponse(
        String sessionId,
        String challengeId,
        String challenge,
        Integer expiresInSeconds,
        LocalDateTime expiresAt
) {
}
