package com.hmiot.hmiot_backend.module.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record CertificateVerifyRequest(
        @NotBlank(message = "username不能为空") String username,
        @NotBlank(message = "sessionId不能为空") String sessionId,
        @NotBlank(message = "challengeId不能为空") String challengeId,
        @NotBlank(message = "signatureB64不能为空") String signatureB64,
        String terminalCode,
        Long dynamicBiometricAuthRecordId
) {
}
