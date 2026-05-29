package com.hmiot.hmiot_backend.module.registration.dto;

public record AdminRegistrationQueryRequest(
        String keyword,
        String status,
        Long current,
        Long size
) {
    public long pageCurrent() {
        return current == null || current < 1 ? 1 : current;
    }

    public long pageSize() {
        return size == null || size < 1 ? 10 : Math.min(size, 100);
    }
}
