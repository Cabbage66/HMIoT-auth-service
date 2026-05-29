package com.hmiot.hmiot_backend.module.ticket.vo;

public record SsoValidateResponse(
        boolean success,
        String message,
        String sourceSystem,
        String targetSystem,
        String appCode,
        TicketVerifyResponse ticketVerify
) {
}
