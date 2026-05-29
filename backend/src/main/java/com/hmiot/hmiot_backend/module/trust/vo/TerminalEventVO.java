package com.hmiot.hmiot_backend.module.trust.vo;

import java.time.LocalDateTime;
import java.util.Map;

public record TerminalEventVO(
        Long id,
        Long terminalId,
        Long userId,
        String eventType,
        String eventLevel,
        LocalDateTime eventTime,
        Map<String, Object> eventPayload,
        Map<String, Object> normalizedPayload,
        LocalDateTime reportedAt
) {
}
