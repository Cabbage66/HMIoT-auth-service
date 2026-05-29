package com.hmiot.hmiot_backend.module.auth.vo;

public record UserLoginResponse(
        String token,
        String tokenType,
        Long userId,
        String username,
        String realName,
        String authMethod,
        Long terminalId,
        String terminalCode,
        String clientType
) {
}
