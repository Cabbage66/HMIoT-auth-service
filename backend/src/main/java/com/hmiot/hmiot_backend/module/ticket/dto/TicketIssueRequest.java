package com.hmiot.hmiot_backend.module.ticket.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record TicketIssueRequest(
        @NotBlank(message = "appCode不能为空") String appCode,
        String audienceSystem,
        Long terminalId,
        Integer trustLevel,
        BigDecimal trustScore,
        Long voiceprintAuthRecordId,
        Long dynamicBiometricAuthRecordId
) {
}
