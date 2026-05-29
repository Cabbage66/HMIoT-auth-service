package com.hmiot.hmiot_backend.module.app.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AppResourceUpdateRequest(
        @NotBlank(message = "appName不能为空") String appName,
        String resourceType,
        String accessUrl,
        @NotNull(message = "securityLevel不能为空") @Min(value = 0, message = "securityLevel不能小于0") Integer securityLevel,
        @NotNull(message = "requiredTrustLevel不能为空") @Min(value = 0, message = "requiredTrustLevel不能小于0") Integer requiredTrustLevel,
        String status,
        String description,
        String metadata
) {
}
