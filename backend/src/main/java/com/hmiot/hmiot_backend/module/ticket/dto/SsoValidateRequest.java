package com.hmiot.hmiot_backend.module.ticket.dto;

import jakarta.validation.constraints.NotBlank;

public record SsoValidateRequest(
        @NotBlank(message = "ticketId不能为空") String ticketId,
        @NotBlank(message = "ticketPayload不能为空") String ticketPayload,
        @NotBlank(message = "sourceSystem不能为空") String sourceSystem,
        @NotBlank(message = "targetSystem不能为空") String targetSystem,
        @NotBlank(message = "appCode不能为空") String appCode,
        Integer requiredTrustLevel
) {
}
