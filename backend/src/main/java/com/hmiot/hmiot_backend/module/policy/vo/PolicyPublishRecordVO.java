package com.hmiot.hmiot_backend.module.policy.vo;

import java.time.LocalDateTime;

public record PolicyPublishRecordVO(
        Long id,
        String policyType,
        Long policyId,
        Integer versionNo,
        String targetType,
        Long targetId,
        String publishStatus,
        String executeResult,
        String failReason,
        Long publishedBy,
        LocalDateTime publishedAt,
        LocalDateTime ackAt
) {
}
