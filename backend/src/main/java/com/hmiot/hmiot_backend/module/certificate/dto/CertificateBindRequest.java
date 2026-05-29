package com.hmiot.hmiot_backend.module.certificate.dto;

import jakarta.validation.constraints.NotBlank;

public record CertificateBindRequest(
        Long userId,
        String username,
        @NotBlank(message = "serialNumber不能为空") String serialNumber,
        @NotBlank(message = "fingerprintSha256不能为空") String fingerprintSha256,
        @NotBlank(message = "certificatePem不能为空") String certificatePem,
        String issuer,
        String subjectDn
) {
}
