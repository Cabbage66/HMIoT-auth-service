package com.hmiot.hmiot_backend.module.ticket.vo;

import com.hmiot.hmiot_backend.module.app.vo.AppResourceVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record IdentityTicketVO(
        String ticketId,
        String ticketPayload,
        Long userId,
        String username,
        String realName,
        Long terminalId,
        String terminalCode,
        String clientType,
        String issuerSystem,
        String audienceSystem,
        String appCode,
        Long appResourceId,
        Integer trustLevel,
        BigDecimal trustScore,
        String authMethod,
        String status,
        LocalDateTime issuedAt,
        LocalDateTime expiresAt,
        Long expiresInSeconds,
        AppResourceVO appResource
) {
}
