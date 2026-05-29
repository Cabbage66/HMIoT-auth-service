package com.hmiot.hmiot_backend.module.ticket.dto;

import jakarta.validation.constraints.NotBlank;

public record TicketVerifyRequest(
        @NotBlank(message = "ticketId不能为空") String ticketId,
        @NotBlank(message = "ticketPayload不能为空") String ticketPayload,
        @NotBlank(message = "appCode不能为空") String appCode,
        @NotBlank(message = "verifierSystem不能为空") String verifierSystem,
        Integer requiredTrustLevel
) {
}
