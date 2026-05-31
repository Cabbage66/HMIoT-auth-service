package com.hmiot.hmiot_backend.module.biometric.vo;

public record FingerprintHealthResponse(
        boolean healthy,
        String targetSystem,
        String modelVersion
) {
}

