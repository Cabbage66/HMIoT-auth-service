package com.hmiot.hmiot_backend.module.app.dto;

import jakarta.validation.constraints.NotBlank;

public record AppResourceStatusRequest(@NotBlank(message = "status不能为空") String status) {
}
