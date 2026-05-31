package com.hmiot.hmiot_backend.module.biometric.vo;

public record FaceHealthResponse(
        boolean healthy,
        String targetSystem,
        String modelVersion
) {
}

