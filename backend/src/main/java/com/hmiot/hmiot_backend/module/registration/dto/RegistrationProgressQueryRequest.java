package com.hmiot.hmiot_backend.module.registration.dto;

import jakarta.validation.constraints.NotBlank;

public record RegistrationProgressQueryRequest(
        @NotBlank(message = "username不能为空")
        String username,
        Long current,
        Long size
) {
    public long pageCurrent() {
        return current == null || current < 1 ? 1 : current;
    }

    public long pageSize() {
        return size == null || size < 1 ? 10 : Math.min(size, 50);
    }
}
