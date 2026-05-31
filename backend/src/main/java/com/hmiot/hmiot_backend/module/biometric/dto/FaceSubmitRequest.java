package com.hmiot.hmiot_backend.module.biometric.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record FaceSubmitRequest(
        @NotBlank(message = "imageBase64不能为空")
        @Size(max = 6000000, message = "人脸图像数据超限")
        String imageBase64,
        String imageFormat,
        Map<String, Object> metadata
) {
}

