package com.hmiot.hmiot_backend.module.ticket.dto;

import jakarta.validation.constraints.NotBlank;

public record SsoAccessRequest(
        @NotBlank(message = "appCode不能为空") String appCode,
        @NotBlank(message = "targetSystem不能为空") String targetSystem
) {
}
