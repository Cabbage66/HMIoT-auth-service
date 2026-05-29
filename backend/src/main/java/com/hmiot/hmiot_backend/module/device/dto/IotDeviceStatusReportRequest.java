package com.hmiot.hmiot_backend.module.device.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.Map;

public record IotDeviceStatusReportRequest(
        Long deviceEntityId,
        String deviceCode,
        String deviceFingerprint,
        @NotBlank(message = "connectStatus不能为空") String connectStatus,
        @NotBlank(message = "trustStatus不能为空") String trustStatus,
        LocalDateTime reportAt,
        String requestId,
        Map<String, Object> payload
) {
}
