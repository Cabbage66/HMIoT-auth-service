package com.hmiot.hmiot_backend.module.biometric.dto;

public record DynamicBiometricQueryRequest(
        String requestId,
        String biometricType
) {
}
