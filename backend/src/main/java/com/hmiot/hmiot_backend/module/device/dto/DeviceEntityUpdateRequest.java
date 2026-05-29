package com.hmiot.hmiot_backend.module.device.dto;

import jakarta.validation.constraints.NotBlank;

public record DeviceEntityUpdateRequest(
        Long entityId,
        @NotBlank(message = "deviceName不能为空") String deviceName,
        @NotBlank(message = "deviceType不能为空") String deviceType,
        String deviceFingerprint,
        String certSubject,
        String status,
        String sourceSystem,
        String description,
        String metadata
) {
}
