package com.hmiot.hmiot_backend.module.registration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationRejectRequest(
        @NotBlank(message = "驳回原因不能为空")
        @Size(max = 255, message = "驳回原因长度不能超过255")
        String rejectReason
) {
}
