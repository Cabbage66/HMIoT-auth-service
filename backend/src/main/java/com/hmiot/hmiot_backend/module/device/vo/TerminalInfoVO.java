package com.hmiot.hmiot_backend.module.device.vo;

import java.time.LocalDateTime;

public record TerminalInfoVO(
        Long id,
        String terminalCode,
        Long deviceEntityId,
        String userAgent,
        String osName,
        String osVersion,
        String clientType,
        String clientVersion,
        String ipAddress,
        String status,
        LocalDateTime lastSeenAt,
        String metadata,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        DeviceEntityVO device
) {
}
