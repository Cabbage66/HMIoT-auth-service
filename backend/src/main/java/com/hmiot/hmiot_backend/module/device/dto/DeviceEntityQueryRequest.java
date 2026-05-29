package com.hmiot.hmiot_backend.module.device.dto;

public record DeviceEntityQueryRequest(String keyword, String deviceType, String status, String sourceSystem, Long current, Long size) {
    public long pageCurrent() {
        return current == null || current < 1 ? 1 : current;
    }

    public long pageSize() {
        return size == null || size < 1 ? 10 : Math.min(size, 100);
    }
}
