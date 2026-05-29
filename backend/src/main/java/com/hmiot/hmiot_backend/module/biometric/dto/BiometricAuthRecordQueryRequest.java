package com.hmiot.hmiot_backend.module.biometric.dto;

import java.time.LocalDateTime;

public record BiometricAuthRecordQueryRequest(Long userId, String biometricType, String authResult, LocalDateTime startTime, LocalDateTime endTime, Long current, Long size) {
    public long pageCurrent() {
        return current == null || current < 1 ? 1 : current;
    }

    public long pageSize() {
        return size == null || size < 1 ? 10 : Math.min(size, 100);
    }
}
