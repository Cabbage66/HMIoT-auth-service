package com.hmiot.hmiot_backend.module.trust.dto;

public record TrustAssessmentTriggerRequest(
        String appCode,
        Long appResourceId,
        String requestId
) {
}
