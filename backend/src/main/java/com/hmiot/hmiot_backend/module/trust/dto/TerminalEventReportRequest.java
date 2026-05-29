package com.hmiot.hmiot_backend.module.trust.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.Map;

public record TerminalEventReportRequest(
        @NotBlank(message = "事件类型不能为空") String eventType,
        String eventLevel,
        LocalDateTime eventTime,
        Map<String, Object> eventPayload
) {
}
