package com.hmiot.hmiot_backend.module.policy.dto;

import jakarta.validation.constraints.NotNull;

public record AuthMethodStatusRequest(@NotNull(message = "enabled不能为空") Boolean enabled) {
}
