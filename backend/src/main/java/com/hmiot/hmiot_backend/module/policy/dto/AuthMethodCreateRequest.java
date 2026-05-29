package com.hmiot.hmiot_backend.module.policy.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthMethodCreateRequest(
        @NotBlank(message = "认证方式编码不能为空") String methodCode,
        @NotBlank(message = "认证方式名称不能为空") String methodName,
        @NotBlank(message = "认证方式类型不能为空") String methodType,
        String compareMode,
        Boolean enabled,
        Integer sortOrder,
        String description
) {
}
