package com.hmiot.hmiot_backend.module.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmiot.hmiot_backend.common.security.CurrentAdmin;
import com.hmiot.hmiot_backend.common.security.CurrentUser;
import com.hmiot.hmiot_backend.module.audit.entity.AuditLog;
import com.hmiot.hmiot_backend.module.audit.mapper.AuditLogMapper;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuditLogService {
    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogMapper auditLogMapper, ObjectMapper objectMapper) {
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
    }

    public void recordAdminSuccess(String operationType, String objectType, String objectId, String requestIp, Map<String, Object> detail) {
        CurrentAdmin currentAdmin = currentAdmin();
        AuditLog auditLog = new AuditLog();
        auditLog.setActorType("ADMIN");
        auditLog.setActorId(currentAdmin == null ? null : currentAdmin.adminId());
        auditLog.setActorName(currentAdmin == null ? null : currentAdmin.username());
        auditLog.setOperationType(operationType);
        auditLog.setObjectType(objectType);
        auditLog.setObjectId(objectId);
        auditLog.setOperationResult("SUCCESS");
        auditLog.setRequestIp(requestIp);
        auditLog.setTraceId(MDC.get("traceId"));
        auditLog.setDetail(toJson(detail));
        auditLogMapper.insert(auditLog);
    }

    public void recordAdminFailure(String operationType, String objectType, String objectId, String requestIp, String failReason, Map<String, Object> detail) {
        CurrentAdmin currentAdmin = currentAdmin();
        AuditLog auditLog = new AuditLog();
        auditLog.setActorType("ADMIN");
        auditLog.setActorId(currentAdmin == null ? null : currentAdmin.adminId());
        auditLog.setActorName(currentAdmin == null ? null : currentAdmin.username());
        auditLog.setOperationType(operationType);
        auditLog.setObjectType(objectType);
        auditLog.setObjectId(objectId);
        auditLog.setOperationResult("FAIL");
        auditLog.setFailReason(failReason);
        auditLog.setRequestIp(requestIp);
        auditLog.setTraceId(MDC.get("traceId"));
        auditLog.setDetail(toJson(detail));
        auditLogMapper.insert(auditLog);
    }

    public void recordLoginSuccess(Long adminId, String username, String requestIp) {
        AuditLog auditLog = new AuditLog();
        auditLog.setActorType("ADMIN");
        auditLog.setActorId(adminId);
        auditLog.setActorName(username);
        auditLog.setOperationType("ADMIN_LOGIN");
        auditLog.setObjectType("SYS_ADMIN");
        auditLog.setObjectId(String.valueOf(adminId));
        auditLog.setOperationResult("SUCCESS");
        auditLog.setRequestIp(requestIp);
        auditLog.setTraceId(MDC.get("traceId"));
        auditLogMapper.insert(auditLog);
    }

    public void recordUserLoginSuccess(Long userId, String username, String operationType, String requestIp, Map<String, Object> detail) {
        AuditLog auditLog = new AuditLog();
        auditLog.setActorType("USER");
        auditLog.setActorId(userId);
        auditLog.setActorName(username);
        auditLog.setOperationType(operationType);
        auditLog.setObjectType("USER_ACCOUNT");
        auditLog.setObjectId(String.valueOf(userId));
        auditLog.setOperationResult("SUCCESS");
        auditLog.setRequestIp(requestIp);
        auditLog.setTraceId(MDC.get("traceId"));
        auditLog.setDetail(toJson(detail));
        auditLogMapper.insert(auditLog);
    }

    public void recordUserLoginFailure(Long userId, String username, String operationType, String requestIp, String failReason, Map<String, Object> detail) {
        AuditLog auditLog = new AuditLog();
        auditLog.setActorType("USER");
        auditLog.setActorId(userId);
        auditLog.setActorName(username);
        auditLog.setOperationType(operationType);
        auditLog.setObjectType("USER_ACCOUNT");
        auditLog.setObjectId(userId == null ? null : String.valueOf(userId));
        auditLog.setOperationResult("FAIL");
        auditLog.setFailReason(failReason);
        auditLog.setRequestIp(requestIp);
        auditLog.setTraceId(MDC.get("traceId"));
        auditLog.setDetail(toJson(detail));
        auditLogMapper.insert(auditLog);
    }

    public void recordUserOperationSuccess(Long userId, String username, String operationType, String objectType, String objectId, String requestIp, Map<String, Object> detail) {
        AuditLog auditLog = new AuditLog();
        auditLog.setActorType("USER");
        auditLog.setActorId(userId);
        auditLog.setActorName(username);
        auditLog.setOperationType(operationType);
        auditLog.setObjectType(objectType);
        auditLog.setObjectId(objectId);
        auditLog.setOperationResult("SUCCESS");
        auditLog.setRequestIp(requestIp);
        auditLog.setTraceId(MDC.get("traceId"));
        auditLog.setDetail(toJson(detail));
        auditLogMapper.insert(auditLog);
    }

    public void recordUserOperationFailure(Long userId, String username, String operationType, String objectType, String objectId, String requestIp, String failReason, Map<String, Object> detail) {
        AuditLog auditLog = new AuditLog();
        auditLog.setActorType("USER");
        auditLog.setActorId(userId);
        auditLog.setActorName(username);
        auditLog.setOperationType(operationType);
        auditLog.setObjectType(objectType);
        auditLog.setObjectId(objectId);
        auditLog.setOperationResult("FAIL");
        auditLog.setFailReason(failReason);
        auditLog.setRequestIp(requestIp);
        auditLog.setTraceId(MDC.get("traceId"));
        auditLog.setDetail(toJson(detail));
        auditLogMapper.insert(auditLog);
    }

    public void recordSystemOperation(String operationType, String objectType, String objectId, String requestIp, String failReason, Map<String, Object> detail) {
        AuditLog auditLog = new AuditLog();
        auditLog.setActorType("SYSTEM");
        auditLog.setActorId(null);
        auditLog.setActorName(currentSystemActorName());
        auditLog.setOperationType(operationType);
        auditLog.setObjectType(objectType);
        auditLog.setObjectId(objectId);
        auditLog.setOperationResult(failReason == null ? "SUCCESS" : "FAIL");
        auditLog.setFailReason(failReason);
        auditLog.setRequestIp(requestIp);
        auditLog.setTraceId(MDC.get("traceId"));
        auditLog.setDetail(toJson(detail));
        auditLogMapper.insert(auditLog);
    }

    private CurrentAdmin currentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentAdmin currentAdmin)) {
            return null;
        }
        return currentAdmin;
    }

    private String currentSystemActorName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return "EXTERNAL_SYSTEM";
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CurrentAdmin currentAdmin) {
            return currentAdmin.username();
        }
        if (principal instanceof CurrentUser currentUser) {
            return currentUser.username();
        }
        return "EXTERNAL_SYSTEM";
    }

    private String toJson(Map<String, Object> detail) {
        if (detail == null || detail.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }
}
