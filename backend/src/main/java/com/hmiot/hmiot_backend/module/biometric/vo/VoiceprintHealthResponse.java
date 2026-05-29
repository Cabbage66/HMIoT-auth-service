package com.hmiot.hmiot_backend.module.biometric.vo;

public record VoiceprintHealthResponse(boolean healthy, String targetSystem, String modelVersion) {
}
