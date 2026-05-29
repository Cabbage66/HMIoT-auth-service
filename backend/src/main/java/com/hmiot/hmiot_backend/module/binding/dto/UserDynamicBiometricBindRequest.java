package com.hmiot.hmiot_backend.module.binding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserDynamicBiometricBindRequest(
        @NotNull(message = "userId不能为空") Long userId,
        @NotBlank(message = "动态生物外部标识不能为空") String externalSubjectId,
        String biometricType,
        String modelSource,
        String reason
) {
}
