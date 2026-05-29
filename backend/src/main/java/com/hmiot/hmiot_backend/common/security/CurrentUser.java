package com.hmiot.hmiot_backend.common.security;

public record CurrentUser(Long userId, String username, String realName, String authMethod, Long terminalId, String terminalCode, String clientType) {
}
