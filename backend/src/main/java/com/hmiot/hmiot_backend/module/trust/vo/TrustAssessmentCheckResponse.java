package com.hmiot.hmiot_backend.module.trust.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TrustAssessmentCheckResponse(
        Long assessmentResultId,
        String requestId,
        boolean trusted,
        BigDecimal trustScore,
        Integer trustLevel,
        String riskLevel,
        String decision,
        String action,
        String failReason,
        LocalDateTime assessedAt
) {
}
