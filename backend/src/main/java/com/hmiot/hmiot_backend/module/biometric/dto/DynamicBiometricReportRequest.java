package com.hmiot.hmiot_backend.module.biometric.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;

public record DynamicBiometricReportRequest(
        @NotBlank(message = "requestId不能为空") String requestId,
        @NotNull(message = "userId不能为空") Long userId,
        @NotBlank(message = "动态生物外部标识不能为空") String externalSubjectId,
        String biometricType,
        Boolean matched,
        BigDecimal score,
        BigDecimal threshold,
        String failReason,
        String modelSource,
        String modelVersion,
        Map<String, Object> rawResponse
) {
}
