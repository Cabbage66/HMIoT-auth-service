package com.hmiot.hmiot_backend.module.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record FingerprintChallengeRequest(
        @NotBlank(message = "username不能为空") String username,
        @NotBlank(message = "terminalCode不能为空") String terminalCode,
        @NotBlank(message = "keyId不能为空") String keyId,
        String deviceFingerprint,
        String clientType,
        String clientVersion,
        String osName,
        String osVersion,
        String userAgent,
        String metadata,
        Long dynamicBiometricAuthRecordId
) {
}

