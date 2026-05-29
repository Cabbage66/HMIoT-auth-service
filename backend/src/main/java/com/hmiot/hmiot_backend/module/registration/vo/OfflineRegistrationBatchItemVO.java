package com.hmiot.hmiot_backend.module.registration.vo;

public record OfflineRegistrationBatchItemVO(
        String username,
        boolean success,
        String message,
        Long registrationId,
        Long userId
) {
}
