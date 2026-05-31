package com.hmiot.hmiot_backend.module.biometric.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record FingerprintSubmitRequest(
        @NotBlank(message = "sessionId不能为空") String sessionId,
        @NotBlank(message = "challengeId不能为空") String challengeId,
        @NotBlank(message = "keyId不能为空") String keyId,
        @NotBlank(message = "publicKeyBase64不能为空") String publicKeyBase64,
        @NotBlank(message = "signatureBase64不能为空") String signatureBase64,
        String signAlgorithm,
        Map<String, Object> metadata
) {
}

