package com.hmiot.hmiot_backend.module.biometric.vo;

import java.time.LocalDateTime;

public record FingerprintChallengeResponse(
        String sessionId,
        String challengeId,
        String challenge,
        Integer expiresInSeconds,
        LocalDateTime expiresAt,
        String keyId
) {
}

