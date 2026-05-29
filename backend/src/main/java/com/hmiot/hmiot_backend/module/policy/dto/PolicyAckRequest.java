package com.hmiot.hmiot_backend.module.policy.dto;

import jakarta.validation.constraints.NotBlank;

public record PolicyAckRequest(
        @NotBlank(message = "executeResult不能为空") String executeResult,
        String failReason
) {
}
