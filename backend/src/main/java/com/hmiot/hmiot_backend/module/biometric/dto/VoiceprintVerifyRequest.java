package com.hmiot.hmiot_backend.module.biometric.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record VoiceprintVerifyRequest(
        @NotBlank(message = "音频不能为空") String audioBase64,
        String audioFormat,
        Integer sampleRate,
        Map<String, Object> metadata
) {
}
