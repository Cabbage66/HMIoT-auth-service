package com.hmiot.hmiot_backend.module.trust.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record TrustAssessmentResultVO(
        Long id,
        Long assessmentRequestId,
        String requestId,
        Long userId,
        Long terminalId,
        Long appResourceId,
        String modelSource,
        String modelVersion,
        BigDecimal trustScore,
        Integer trustLevel,
        String riskLevel,
        Boolean anomaly,
        Map<String, Object> anomalyIndicators,
        Map<String, Object> alerts,
        String decision,
        String rejectReason,
        Map<String, Object> inputPayload,
        Map<String, Object> rawResponse,
        LocalDateTime requestTime,
        LocalDateTime assessedAt
) {
}
