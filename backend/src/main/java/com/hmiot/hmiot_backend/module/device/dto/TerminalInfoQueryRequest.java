package com.hmiot.hmiot_backend.module.device.dto;

public record TerminalInfoQueryRequest(String keyword, String clientType, String status, Long deviceEntityId, Long current, Long size) {
    public long pageCurrent() {
        return current == null || current < 1 ? 1 : current;
    }

    public long pageSize() {
        return size == null || size < 1 ? 10 : Math.min(size, 100);
    }
}
