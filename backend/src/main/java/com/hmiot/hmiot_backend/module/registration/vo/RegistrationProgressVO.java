package com.hmiot.hmiot_backend.module.registration.vo;

import java.time.LocalDateTime;

public record RegistrationProgressVO(
        Long registrationId,
        String username,
        String realName,
        String phone,
        String email,
        String status,
        String rejectReason,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt
) {
}
