package com.hmiot.hmiot_backend.module.biometric.dto;

import jakarta.validation.constraints.NotBlank;

public record BiometricTemplateReviewRequest(
        @NotBlank(message = "审核结果不能为空") String auditStatus,
        String rejectReason
) {
}
