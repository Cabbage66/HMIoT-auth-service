package com.hmiot.hmiot_backend.module.user.vo;

import java.time.LocalDateTime;

public record UserVO(
        Long id,
        String username,
        String realName,
        String phone,
        String email,
        String idCardNo,
        String status,
        String registerSource,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        UserProfileVO profile
) {
}
