package com.hmiot.hmiot_backend.module.biometric.vo;

import java.math.BigDecimal;

public record DynamicBiometricVerifyResponse(
        Long recordId,
        String requestId,
        boolean matched,
        BigDecimal score,
        BigDecimal threshold,
        String failReason
) {
}
