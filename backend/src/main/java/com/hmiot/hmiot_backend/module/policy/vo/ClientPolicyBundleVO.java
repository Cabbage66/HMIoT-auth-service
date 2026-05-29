package com.hmiot.hmiot_backend.module.policy.vo;

import java.util.List;

public record ClientPolicyBundleVO(
        Long userId,
        Long terminalId,
        String terminalCode,
        String clientType,
        List<PolicyResolveResponse> policies,
        List<SecurityBaselineVO> securityBaselines,
        List<PolicyPublishRecordVO> pendingPublishRecords
) {
}
