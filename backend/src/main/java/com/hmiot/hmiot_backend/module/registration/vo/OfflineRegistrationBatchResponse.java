package com.hmiot.hmiot_backend.module.registration.vo;

import java.util.List;

public record OfflineRegistrationBatchResponse(
        int total,
        int successCount,
        int failCount,
        List<OfflineRegistrationBatchItemVO> items
) {
}
