package com.hmiot.hmiot_backend.module.binding.dto;

import jakarta.validation.constraints.NotNull;

public record UserTerminalBindRequest(
        @NotNull(message = "userId不能为空") Long userId,
        @NotNull(message = "terminalId不能为空") Long terminalId,
        String reason
) {
}
