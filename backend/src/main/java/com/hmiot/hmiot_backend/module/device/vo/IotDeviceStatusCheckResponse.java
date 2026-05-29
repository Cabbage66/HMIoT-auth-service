package com.hmiot.hmiot_backend.module.device.vo;

import java.time.LocalDateTime;

public record IotDeviceStatusCheckResponse(
        Long deviceEntityId,
        String connectStatus,
        String trustStatus,
        boolean trusted,
        LocalDateTime lastReportAt,
        String failReason
) {
}
