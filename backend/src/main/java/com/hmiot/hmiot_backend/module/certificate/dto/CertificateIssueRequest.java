package com.hmiot.hmiot_backend.module.certificate.dto;

import jakarta.validation.constraints.NotBlank;

public record CertificateIssueRequest(
        Long userId,
        String username,
        @NotBlank(message = "publicKeyPem不能为空") String publicKeyPem
) {
}
