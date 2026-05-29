package com.hmiot.hmiot_backend.common.security;

public record CurrentAdmin(Long adminId, String username, String displayName, String roleCode) {
}
