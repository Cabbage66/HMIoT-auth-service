package com.hmiot.hmiot_backend.module.ticket.vo;

import com.hmiot.hmiot_backend.module.app.vo.AppResourceVO;

import java.math.BigDecimal;

public record TicketVerifyResponse(
        boolean verified,
        String resultCode,
        String message,
        String ticketId,
        Long userId,
        String username,
        String realName,
        String issuerSystem,
        String audienceSystem,
        String verifierSystem,
        String appCode,
        Integer trustLevel,
        BigDecimal trustScore,
        AppResourceVO appResource
) {
}
