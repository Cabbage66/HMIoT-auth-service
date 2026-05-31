package com.hmiot.hmiot_backend.module.biometric.vo;

public record FingerprintVerifyResponse(
        Long recordId,
        boolean verified,
        String failReason
) {
}

