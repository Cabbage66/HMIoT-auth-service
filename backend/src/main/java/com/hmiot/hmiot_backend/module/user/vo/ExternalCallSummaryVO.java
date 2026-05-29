package com.hmiot.hmiot_backend.module.user.vo;

public record ExternalCallSummaryVO(
        Long callId,
        String integrationType,
        String targetSystem,
        String businessKey,
        String callResult
) {
}
