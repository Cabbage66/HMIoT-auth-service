package com.hmiot.hmiot_backend.module.policy.dto;

public record PolicyPublishRecordQueryRequest(String policyType, Long policyId, String targetType, Long targetId, String publishStatus, Long current, Long size) {
    public long pageCurrent() {
        return current == null || current < 1 ? 1 : current;
    }

    public long pageSize() {
        return size == null || size < 1 ? 10 : Math.min(size, 100);
    }
}
