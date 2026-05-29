package com.hmiot.hmiot_backend.module.device.dto;

import jakarta.validation.constraints.NotBlank;

public record DeviceEntityStatusRequest(@NotBlank(message = "status不能为空") String status) {
}
