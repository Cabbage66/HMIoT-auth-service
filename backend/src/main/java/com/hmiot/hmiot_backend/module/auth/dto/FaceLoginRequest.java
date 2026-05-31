package com.hmiot.hmiot_backend.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FaceLoginRequest(
        @NotBlank(message = "username不能为空") String username,
        @NotBlank(message = "imageBase64不能为空")
        @Size(max = 6000000, message = "人脸图像数据超限")
        String imageBase64,
        String imageFormat,
        @NotBlank(message = "terminalCode不能为空") String terminalCode,
        String deviceFingerprint,
        String clientType,
        String clientVersion,
        String osName,
        String osVersion,
        String userAgent,
        String metadata,
        Long dynamicBiometricAuthRecordId
) {
}

