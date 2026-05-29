package com.hmiot.hmiot_backend.module.policy.vo;

import com.hmiot.hmiot_backend.module.app.vo.AppResourceVO;

import java.util.List;

public record PolicyResolveResponse(
        String sceneType,
        Long policyId,
        String policyCode,
        String policyName,
        Integer versionNo,
        Long appResourceId,
        String appCode,
        List<String> authChain,
        Integer minTrustLevel,
        Integer sessionTtlSeconds,
        String actionOnFail,
        boolean matchedPolicy,
        String decision,
        AppResourceVO appResource
) {
}
