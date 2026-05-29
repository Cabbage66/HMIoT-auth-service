package com.hmiot.hmiot_backend.module.certificate.dto;

import jakarta.validation.constraints.NotBlank;

public record CertificateRevokeRequest(@NotBlank(message = "revokeReason不能为空") String revokeReason) {
}
