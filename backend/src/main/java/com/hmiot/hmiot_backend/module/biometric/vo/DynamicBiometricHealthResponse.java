package com.hmiot.hmiot_backend.module.biometric.vo;

public record DynamicBiometricHealthResponse(boolean healthy, String targetSystem, String modelVersion) {
}
