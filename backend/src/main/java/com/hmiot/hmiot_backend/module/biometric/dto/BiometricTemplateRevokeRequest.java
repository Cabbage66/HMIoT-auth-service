package com.hmiot.hmiot_backend.module.biometric.dto;

import jakarta.validation.constraints.NotBlank;

public record BiometricTemplateRevokeRequest(@NotBlank(message = "撤销原因不能为空") String revokeReason) {
}
