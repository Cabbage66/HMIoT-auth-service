package com.hmiot.hmiot_backend.module.device.vo;

import java.time.LocalDateTime;

public record IotDeviceStatusVO(
        Long id,
        Long deviceEntityId,
        String connectStatus,
        String trustStatus,
        LocalDateTime lastReportAt,
        String lastPayload,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
