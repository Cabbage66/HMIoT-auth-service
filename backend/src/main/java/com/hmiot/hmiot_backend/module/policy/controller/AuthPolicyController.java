package com.hmiot.hmiot_backend.module.policy.controller;

import com.hmiot.hmiot_backend.common.response.ApiResponse;
import com.hmiot.hmiot_backend.module.audit.service.AuditLogService;
import com.hmiot.hmiot_backend.module.policy.dto.AuthPolicyCreateRequest;
import com.hmiot.hmiot_backend.module.policy.dto.AuthPolicyQueryRequest;
import com.hmiot.hmiot_backend.module.policy.dto.AuthPolicyUpdateRequest;
import com.hmiot.hmiot_backend.module.policy.dto.PolicyPublishRequest;
import com.hmiot.hmiot_backend.module.policy.dto.PolicyResolveRequest;
import com.hmiot.hmiot_backend.module.policy.dto.PolicyStatusRequest;
import com.hmiot.hmiot_backend.module.policy.service.AuthPolicyService;
import com.hmiot.hmiot_backend.module.policy.service.PolicyExecutionService;
import com.hmiot.hmiot_backend.module.policy.service.PolicyPublishService;
import com.hmiot.hmiot_backend.module.policy.vo.AuthPolicyVO;
import com.hmiot.hmiot_backend.module.policy.vo.PolicyPublishRecordVO;
import com.hmiot.hmiot_backend.module.policy.vo.PolicyResolveResponse;
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
@RequestMapping("/api/v1/auth-policies")
public class AuthPolicyController {
    private final AuthPolicyService authPolicyService;
    private final PolicyExecutionService policyExecutionService;
    private final PolicyPublishService policyPublishService;
    private final AuditLogService auditLogService;

    public AuthPolicyController(AuthPolicyService authPolicyService, PolicyExecutionService policyExecutionService, PolicyPublishService policyPublishService, AuditLogService auditLogService) {
        this.authPolicyService = authPolicyService;
        this.policyExecutionService = policyExecutionService;
        this.policyPublishService = policyPublishService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<PageVO<AuthPolicyVO>> listPolicies(AuthPolicyQueryRequest request) {
        return ApiResponse.success(authPolicyService.listPolicies(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<AuthPolicyVO> getPolicy(@PathVariable Long id) {
        return ApiResponse.success(authPolicyService.getPolicy(id));
    }

    @PostMapping
    public ApiResponse<AuthPolicyVO> createPolicy(@Valid @RequestBody AuthPolicyCreateRequest request, HttpServletRequest servletRequest) {
        try {
            AuthPolicyVO policy = authPolicyService.createPolicy(request);
            auditLogService.recordAdminSuccess("AUTH_POLICY_CREATE", "AUTH_POLICY", String.valueOf(policy.id()), servletRequest.getRemoteAddr(), auditDetail(policy));
            return ApiResponse.success(policy);
        } catch (RuntimeException exception) {
            auditLogService.recordAdminFailure("AUTH_POLICY_CREATE", "AUTH_POLICY", null, servletRequest.getRemoteAddr(), exception.getMessage(), Map.of("policyCode", request.policyCode(), "sceneType", request.sceneType()));
            throw exception;
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<AuthPolicyVO> updatePolicy(@PathVariable Long id, @Valid @RequestBody AuthPolicyUpdateRequest request, HttpServletRequest servletRequest) {
        try {
            AuthPolicyVO policy = authPolicyService.updatePolicy(id, request);
            auditLogService.recordAdminSuccess("AUTH_POLICY_UPDATE", "AUTH_POLICY", String.valueOf(id), servletRequest.getRemoteAddr(), auditDetail(policy));
            return ApiResponse.success(policy);
        } catch (RuntimeException exception) {
            auditLogService.recordAdminFailure("AUTH_POLICY_UPDATE", "AUTH_POLICY", String.valueOf(id), servletRequest.getRemoteAddr(), exception.getMessage(), Map.of("policyName", request.policyName(), "sceneType", request.sceneType()));
            throw exception;
        }
    }

    @PostMapping("/{id}/status")
    public ApiResponse<AuthPolicyVO> updateStatus(@PathVariable Long id, @Valid @RequestBody PolicyStatusRequest request, HttpServletRequest servletRequest) {
        try {
            AuthPolicyVO policy = authPolicyService.updateStatus(id, request);
            auditLogService.recordAdminSuccess("AUTH_POLICY_STATUS_CHANGE", "AUTH_POLICY", String.valueOf(id), servletRequest.getRemoteAddr(), auditDetail(policy));
            return ApiResponse.success(policy);
        } catch (RuntimeException exception) {
            auditLogService.recordAdminFailure("AUTH_POLICY_STATUS_CHANGE", "AUTH_POLICY", String.valueOf(id), servletRequest.getRemoteAddr(), exception.getMessage(), Map.of("status", request.status()));
            throw exception;
        }
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<PolicyPublishRecordVO> publish(@PathVariable Long id, @Valid @RequestBody PolicyPublishRequest request, HttpServletRequest servletRequest) {
        try {
            PolicyPublishRecordVO record = policyPublishService.publishAuthPolicy(id, request);
            auditLogService.recordAdminSuccess("AUTH_POLICY_PUBLISH", "AUTH_POLICY", String.valueOf(id), servletRequest.getRemoteAddr(), Map.of("publishRecordId", record.id(), "targetType", record.targetType(), "targetId", record.targetId() == null ? "" : record.targetId()));
            return ApiResponse.success(record);
        } catch (RuntimeException exception) {
            auditLogService.recordAdminFailure("AUTH_POLICY_PUBLISH", "AUTH_POLICY", String.valueOf(id), servletRequest.getRemoteAddr(), exception.getMessage(), Map.of("targetType", request.targetType(), "targetId", request.targetId() == null ? "" : request.targetId()));
            throw exception;
        }
    }

    @PostMapping("/resolve")
    public ApiResponse<PolicyResolveResponse> resolve(@Valid @RequestBody PolicyResolveRequest request) {
        return ApiResponse.success(policyExecutionService.resolvePolicy(request));
    }

    private Map<String, Object> auditDetail(AuthPolicyVO policy) {
        return Map.of("policyCode", policy.policyCode(), "policyName", policy.policyName(), "sceneType", policy.sceneType(), "status", policy.status(), "versionNo", policy.versionNo());
    }
}
