package com.hmiot.hmiot_backend.module.user.vo;

import java.time.LocalDateTime;

public record UserProfileVO(
        String department,
        String positionName,
        String attributeVersion,
        String externalSource,
        String externalUserId,
        String externalAttributes,
        LocalDateTime syncedAt
) {
}
