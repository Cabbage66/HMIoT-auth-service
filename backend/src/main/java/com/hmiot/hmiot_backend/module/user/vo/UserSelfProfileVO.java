package com.hmiot.hmiot_backend.module.user.vo;

public record UserSelfProfileVO(
        Long userId,
        String username,
        String realName,
        String phone,
        String email,
        String idCardNo
) {
}
