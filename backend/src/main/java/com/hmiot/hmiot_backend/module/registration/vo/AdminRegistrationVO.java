package com.hmiot.hmiot_backend.module.registration.vo;

import java.time.LocalDateTime;

public record AdminRegistrationVO(
        Long registrationId,
        Long userId,
        String username,
        String realName,
        String phone,
        String email,
        String idCardNo,
        String registerSource,
        String status,
        Long reviewerId,
        LocalDateTime reviewedAt,
        String rejectReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
