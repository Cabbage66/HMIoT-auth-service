package com.hmiot.hmiot_backend.module.policy.controller;

import com.hmiot.hmiot_backend.common.response.ApiResponse;
import com.hmiot.hmiot_backend.module.audit.service.AuditLogService;
import com.hmiot.hmiot_backend.module.policy.dto.AuthMethodCreateRequest;
import com.hmiot.hmiot_backend.module.policy.dto.AuthMethodQueryRequest;
import com.hmiot.hmiot_backend.module.policy.dto.AuthMethodStatusRequest;
import com.hmiot.hmiot_backend.module.policy.dto.AuthMethodUpdateRequest;
import com.hmiot.hmiot_backend.module.policy.service.AuthMethodService;
import com.hmiot.hmiot_backend.module.policy.vo.AuthMethodVO;
import com.hmiot.hmiot_backend.module.user.vo.PageVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth-methods")
public class AuthMethodController {
    private final AuthMethodService authMethodService;
    private final AuditLogService auditLogService;

    public AuthMethodController(AuthMethodService authMethodService, AuditLogService auditLogService) {
        this.authMethodService = authMethodService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<PageVO<AuthMethodVO>> listMethods(AuthMethodQueryRequest request) {
        return ApiResponse.success(authMethodService.listMethods(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<AuthMethodVO> getMethod(@PathVariable Long id) {
        return ApiResponse.success(authMethodService.getMethod(id));
    }

    @PostMapping
    public ApiResponse<AuthMethodVO> createMethod(@Valid @RequestBody AuthMethodCreateRequest request, HttpServletRequest servletRequest) {
        try {
            AuthMethodVO method = authMethodService.createMethod(request);
            auditLogService.recordAdminSuccess("AUTH_METHOD_CREATE", "AUTH_METHOD", String.valueOf(method.id()), servletRequest.getRemoteAddr(), auditDetail(method));
            return ApiResponse.success(method);
        } catch (RuntimeException exception) {
            auditLogService.recordAdminFailure("AUTH_METHOD_CREATE", "AUTH_METHOD", null, servletRequest.getRemoteAddr(), exception.getMessage(), Map.of("methodCode", request.methodCode()));
            throw exception;
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<AuthMethodVO> updateMethod(@PathVariable Long id, @Valid @RequestBody AuthMethodUpdateRequest request, HttpServletRequest servletRequest) {
        try {
            AuthMethodVO method = authMethodService.updateMethod(id, request);
            auditLogService.recordAdminSuccess("AUTH_METHOD_UPDATE", "AUTH_METHOD", String.valueOf(id), servletRequest.getRemoteAddr(), auditDetail(method));
            return ApiResponse.success(method);
        } catch (RuntimeException exception) {
            auditLogService.recordAdminFailure("AUTH_METHOD_UPDATE", "AUTH_METHOD", String.valueOf(id), servletRequest.getRemoteAddr(), exception.getMessage(), Map.of("methodName", request.methodName()));
            throw exception;
        }
    }

    @PostMapping("/{id}/status")
    public ApiResponse<AuthMethodVO> updateStatus(@PathVariable Long id, @Valid @RequestBody AuthMethodStatusRequest request, HttpServletRequest servletRequest) {
        try {
            AuthMethodVO method = authMethodService.updateStatus(id, request);
            auditLogService.recordAdminSuccess("AUTH_METHOD_STATUS_CHANGE", "AUTH_METHOD", String.valueOf(id), servletRequest.getRemoteAddr(), auditDetail(method));
            return ApiResponse.success(method);
        } catch (RuntimeException exception) {
            auditLogService.recordAdminFailure("AUTH_METHOD_STATUS_CHANGE", "AUTH_METHOD", String.valueOf(id), servletRequest.getRemoteAddr(), exception.getMessage(), Map.of("enabled", request.enabled()));
            throw exception;
        }
    }

    private Map<String, Object> auditDetail(AuthMethodVO method) {
        return Map.of("methodCode", method.methodCode(), "methodName", method.methodName(), "enabled", method.enabled());
    }
}
