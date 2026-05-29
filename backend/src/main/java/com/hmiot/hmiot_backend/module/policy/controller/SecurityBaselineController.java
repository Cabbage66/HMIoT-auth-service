package com.hmiot.hmiot_backend.module.policy.controller;

import com.hmiot.hmiot_backend.common.response.ApiResponse;
import com.hmiot.hmiot_backend.module.audit.service.AuditLogService;
import com.hmiot.hmiot_backend.module.policy.dto.PolicyPublishRequest;
import com.hmiot.hmiot_backend.module.policy.dto.PolicyStatusRequest;
import com.hmiot.hmiot_backend.module.policy.dto.SecurityBaselineCreateRequest;
import com.hmiot.hmiot_backend.module.policy.dto.SecurityBaselineQueryRequest;
import com.hmiot.hmiot_backend.module.policy.dto.SecurityBaselineUpdateRequest;
import com.hmiot.hmiot_backend.module.policy.service.PolicyPublishService;
import com.hmiot.hmiot_backend.module.policy.service.SecurityBaselineService;
import com.hmiot.hmiot_backend.module.policy.vo.PolicyPublishRecordVO;
import com.hmiot.hmiot_backend.module.policy.vo.SecurityBaselineVO;
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
@RequestMapping("/api/v1/security-baselines")
public class SecurityBaselineController {
    private final SecurityBaselineService securityBaselineService;
    private final PolicyPublishService policyPublishService;
    private final AuditLogService auditLogService;

    public SecurityBaselineController(SecurityBaselineService securityBaselineService, PolicyPublishService policyPublishService, AuditLogService auditLogService) {
        this.securityBaselineService = securityBaselineService;
        this.policyPublishService = policyPublishService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<PageVO<SecurityBaselineVO>> listBaselines(SecurityBaselineQueryRequest request) {
        return ApiResponse.success(securityBaselineService.listBaselines(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<SecurityBaselineVO> getBaseline(@PathVariable Long id) {
        return ApiResponse.success(securityBaselineService.getBaseline(id));
    }

    @PostMapping
    public ApiResponse<SecurityBaselineVO> createBaseline(@Valid @RequestBody SecurityBaselineCreateRequest request, HttpServletRequest servletRequest) {
        try {
            SecurityBaselineVO baseline = securityBaselineService.createBaseline(request);
            auditLogService.recordAdminSuccess("SECURITY_BASELINE_CREATE", "SECURITY_BASELINE", String.valueOf(baseline.id()), servletRequest.getRemoteAddr(), auditDetail(baseline));
            return ApiResponse.success(baseline);
        } catch (RuntimeException exception) {
            auditLogService.recordAdminFailure("SECURITY_BASELINE_CREATE", "SECURITY_BASELINE", null, servletRequest.getRemoteAddr(), exception.getMessage(), Map.of("baselineCode", request.baselineCode(), "targetType", request.targetType()));
            throw exception;
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<SecurityBaselineVO> updateBaseline(@PathVariable Long id, @Valid @RequestBody SecurityBaselineUpdateRequest request, HttpServletRequest servletRequest) {
        try {
            SecurityBaselineVO baseline = securityBaselineService.updateBaseline(id, request);
            auditLogService.recordAdminSuccess("SECURITY_BASELINE_UPDATE", "SECURITY_BASELINE", String.valueOf(id), servletRequest.getRemoteAddr(), auditDetail(baseline));
            return ApiResponse.success(baseline);
        } catch (RuntimeException exception) {
            auditLogService.recordAdminFailure("SECURITY_BASELINE_UPDATE", "SECURITY_BASELINE", String.valueOf(id), servletRequest.getRemoteAddr(), exception.getMessage(), Map.of("baselineName", request.baselineName(), "targetType", request.targetType()));
            throw exception;
        }
    }

    @PostMapping("/{id}/status")
    public ApiResponse<SecurityBaselineVO> updateStatus(@PathVariable Long id, @Valid @RequestBody PolicyStatusRequest request, HttpServletRequest servletRequest) {
        try {
            SecurityBaselineVO baseline = securityBaselineService.updateStatus(id, request);
            auditLogService.recordAdminSuccess("SECURITY_BASELINE_STATUS_CHANGE", "SECURITY_BASELINE", String.valueOf(id), servletRequest.getRemoteAddr(), auditDetail(baseline));
            return ApiResponse.success(baseline);
        } catch (RuntimeException exception) {
            auditLogService.recordAdminFailure("SECURITY_BASELINE_STATUS_CHANGE", "SECURITY_BASELINE", String.valueOf(id), servletRequest.getRemoteAddr(), exception.getMessage(), Map.of("status", request.status()));
            throw exception;
        }
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<PolicyPublishRecordVO> publish(@PathVariable Long id, @Valid @RequestBody PolicyPublishRequest request, HttpServletRequest servletRequest) {
        try {
            PolicyPublishRecordVO record = policyPublishService.publishSecurityBaseline(id, request);
            auditLogService.recordAdminSuccess("SECURITY_BASELINE_PUBLISH", "SECURITY_BASELINE", String.valueOf(id), servletRequest.getRemoteAddr(), Map.of("publishRecordId", record.id(), "targetType", record.targetType(), "targetId", record.targetId() == null ? "" : record.targetId()));
            return ApiResponse.success(record);
        } catch (RuntimeException exception) {
            auditLogService.recordAdminFailure("SECURITY_BASELINE_PUBLISH", "SECURITY_BASELINE", String.valueOf(id), servletRequest.getRemoteAddr(), exception.getMessage(), Map.of("targetType", request.targetType(), "targetId", request.targetId() == null ? "" : request.targetId()));
            throw exception;
        }
    }

    private Map<String, Object> auditDetail(SecurityBaselineVO baseline) {
        return Map.of("baselineCode", baseline.baselineCode(), "baselineName", baseline.baselineName(), "targetType", baseline.targetType(), "riskLevel", baseline.riskLevel(), "status", baseline.status());
    }
}
