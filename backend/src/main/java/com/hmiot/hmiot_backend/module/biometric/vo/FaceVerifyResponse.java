package com.hmiot.hmiot_backend.module.biometric.vo;

import java.math.BigDecimal;

public record FaceVerifyResponse(
        Long recordId,
        boolean verified,
        BigDecimal score,
        BigDecimal threshold,
        String failReason
) {
}

