package com.hmiot.hmiot_backend.module.trust.dto;

public record TrustAssessmentQueryRequest(
        Long userId,
        Long terminalId,
        Long appResourceId,
        String riskLevel,
        String decision,
        Long pageCurrent,
        Long pageSize
) {
    public Long pageCurrent() {
        return pageCurrent == null || pageCurrent < 1 ? 1L : pageCurrent;
    }

    public Long pageSize() {
        return pageSize == null || pageSize < 1 ? 10L : Math.min(pageSize, 100L);
    }
}
