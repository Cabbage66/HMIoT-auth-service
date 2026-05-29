package com.hmiot.hmiot_backend.module.registration.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OfflineRegistrationBatchRequest(
        @NotEmpty(message = "批量注册条目不能为空")
        List<@Valid OfflineRegistrationCreateRequest> items
) {
}
