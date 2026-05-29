package com.hmiot.hmiot_backend.module.trust.vo;

public record UebaHealthResponse(
        boolean healthy,
        String targetSystem,
        String modelVersion
) {
}
