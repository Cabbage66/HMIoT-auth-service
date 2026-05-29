package com.hmiot.hmiot_backend.module.auth.vo;

public record AdminLoginResponse(
        String token,
        String tokenType,
        Long adminId,
        String username,
        String displayName,
        String roleCode
) {
}
