package com.hmiot.hmiot_backend.module.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordLoginRequest(
        @NotBlank(message = "username不能为空") String username,
        @NotBlank(message = "password不能为空") String password,
        @NotBlank(message = "terminalCode不能为空") String terminalCode,
        String deviceFingerprint,
        String clientType,
        String clientVersion,
        String osName,
        String osVersion,
        String userAgent,
        String metadata,
        Long dynamicBiometricAuthRecordId
) {
}
