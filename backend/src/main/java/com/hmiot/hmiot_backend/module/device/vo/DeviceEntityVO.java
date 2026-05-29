package com.hmiot.hmiot_backend.module.device.vo;

import java.time.LocalDateTime;

public record DeviceEntityVO(
        Long id,
        Long entityId,
        String deviceCode,
        String deviceName,
        String deviceType,
        String deviceFingerprint,
        String certSubject,
        String status,
        String sourceSystem,
        String description,
        String metadata,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
