package com.hmiot.hmiot_backend.module.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record FingerprintLoginRequest(
        @NotBlank(message = "username不能为空") String username,
        @NotBlank(message = "sessionId不能为空") String sessionId,
        @NotBlank(message = "challengeId不能为空") String challengeId,
        @NotBlank(message = "keyId不能为空") String keyId,
        @NotBlank(message = "signatureBase64不能为空") String signatureBase64,
        String signAlgorithm,
        @NotBlank(message = "terminalCode不能为空") String terminalCode,
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

