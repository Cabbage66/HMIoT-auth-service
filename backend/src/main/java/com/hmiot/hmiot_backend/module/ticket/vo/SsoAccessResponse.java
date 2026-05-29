package com.hmiot.hmiot_backend.module.ticket.vo;

public record SsoAccessResponse(
        boolean success,
        String message,
        String sourceSystem,
        String targetSystem,
        String appCode,
        String accessUrl,
        IdentityTicketVO ticket
) {
}
