package com.hmiot.hmiot_backend.module.biometric.dto;

public record FingerprintChallengeRequest(
        String purpose,
        String keyId
) {
}

