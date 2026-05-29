package com.hmiot.hmiot_backend.module.device.dto;

import jakarta.validation.constraints.NotBlank;

public record TerminalInfoCreateRequest(
        @NotBlank(message = "terminalCode不能为空") String terminalCode,
        Long deviceEntityId,
        String userAgent,
        String osName,
        String osVersion,
        @NotBlank(message = "clientType不能为空") String clientType,
        String clientVersion,
        String ipAddress,
        String status,
        String metadata
) {
}
