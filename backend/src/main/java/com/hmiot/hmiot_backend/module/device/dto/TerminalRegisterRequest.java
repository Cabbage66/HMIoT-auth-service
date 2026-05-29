package com.hmiot.hmiot_backend.module.device.dto;

public record TerminalRegisterRequest(
        String terminalCode,
        String deviceFingerprint,
        String clientType,
        String clientVersion,
        String osName,
        String osVersion,
        String userAgent,
        String metadata
) {
}
