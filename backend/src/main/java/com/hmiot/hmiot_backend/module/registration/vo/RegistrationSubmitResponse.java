package com.hmiot.hmiot_backend.module.registration.vo;

import java.time.LocalDateTime;

public record RegistrationSubmitResponse(
        Long registrationId,
        Long userId,
        String username,
        String status,
        LocalDateTime submittedAt
) {
}
