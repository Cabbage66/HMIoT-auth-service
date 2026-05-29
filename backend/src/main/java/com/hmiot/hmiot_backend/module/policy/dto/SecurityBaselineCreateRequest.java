package com.hmiot.hmiot_backend.module.policy.dto;

import jakarta.validation.constraints.NotBlank;

public record SecurityBaselineCreateRequest(
        @NotBlank(message = "基线编码不能为空") String baselineCode,
        @NotBlank(message = "基线名称不能为空") String baselineName,
        @NotBlank(message = "目标类型不能为空") String targetType,
        String targetScope,
        @NotBlank(message = "规则配置不能为空") String ruleConfig,
        Integer riskLevel,
        String actionOnHit,
        String status
) {
}
