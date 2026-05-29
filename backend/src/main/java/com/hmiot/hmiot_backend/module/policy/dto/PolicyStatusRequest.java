package com.hmiot.hmiot_backend.module.policy.dto;

import jakarta.validation.constraints.NotBlank;

public record PolicyStatusRequest(@NotBlank(message = "status不能为空") String status) {
}
