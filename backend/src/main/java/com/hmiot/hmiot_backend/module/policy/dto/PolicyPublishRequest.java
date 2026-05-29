package com.hmiot.hmiot_backend.module.policy.dto;

import jakarta.validation.constraints.NotBlank;

public record PolicyPublishRequest(
        @NotBlank(message = "targetType不能为空") String targetType,
        Long targetId
) {
}
