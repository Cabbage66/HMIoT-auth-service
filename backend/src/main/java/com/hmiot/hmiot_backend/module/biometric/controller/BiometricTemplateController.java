package com.hmiot.hmiot_backend.module.biometric.controller;

import com.hmiot.hmiot_backend.common.response.ApiResponse;
import com.hmiot.hmiot_backend.module.audit.service.AuditLogService;
import com.hmiot.hmiot_backend.module.biometric.dto.BiometricAuthRecordQueryRequest;
import com.hmiot.hmiot_backend.module.biometric.dto.BiometricTemplateQueryRequest;
import com.hmiot.hmiot_backend.module.biometric.dto.BiometricTemplateReviewRequest;
import com.hmiot.hmiot_backend.module.biometric.dto.BiometricTemplateRevokeRequest;
import com.hmiot.hmiot_backend.module.biometric.service.BiometricTemplateService;
import com.hmiot.hmiot_backend.module.biometric.vo.BiometricAuthRecordVO;
import com.hmiot.hmiot_backend.module.biometric.vo.BiometricTemplateVO;
import com.hmiot.hmiot_backend.module.user.vo.PageVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/biometric-templates")
public class BiometricTemplateController {
    private final BiometricTemplateService biometricTemplateService;
    private final AuditLogService auditLogService;

    public BiometricTemplateController(BiometricTemplateService biometricTemplateService, AuditLogService auditLogService) {
        this.biometricTemplateService = biometricTemplateService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<PageVO<BiometricTemplateVO>> listTemplates(BiometricTemplateQueryRequest request) {
        return ApiResponse.success(biometricTemplateService.listTemplates(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<BiometricTemplateVO> getTemplate(@PathVariable Long id) {
        return ApiResponse.success(biometricTemplateService.getTemplate(id));
    }

    @PostMapping("/{id}/review")
    public ApiResponse<BiometricTemplateVO> reviewTemplate(@PathVariable Long id, @Valid @RequestBody BiometricTemplateReviewRequest request, HttpServletRequest servletRequest) {
        try {
            BiometricTemplateVO template = biometricTemplateService.reviewTemplate(id, request);
            auditLogService.recordAdminSuccess("BIOMETRIC_TEMPLATE_REVIEW", "BIOMETRIC_TEMPLATE", String.valueOf(id), servletRequest.getRemoteAddr(), auditDetail(template));
            return ApiResponse.success(template);
        } catch (RuntimeException exception) {
            auditLogService.recordAdminFailure("BIOMETRIC_TEMPLATE_REVIEW", "BIOMETRIC_TEMPLATE", String.valueOf(id), servletRequest.getRemoteAddr(), exception.getMessage(), Map.of("auditStatus", request.auditStatus(), "rejectReason", request.rejectReason() == null ? "" : request.rejectReason()));
            throw exception;
        }
    }

    @PostMapping("/{id}/revoke")
    public ApiResponse<BiometricTemplateVO> revokeTemplate(@PathVariable Long id, @Valid @RequestBody BiometricTemplateRevokeRequest request, HttpServletRequest servletRequest) {
        try {
            BiometricTemplateVO template = biometricTemplateService.revokeTemplate(id, request);
            auditLogService.recordAdminSuccess("BIOMETRIC_TEMPLATE_REVOKE", "BIOMETRIC_TEMPLATE", String.valueOf(id), servletRequest.getRemoteAddr(), auditDetail(template));
            return ApiResponse.success(template);
        } catch (RuntimeException exception) {
            auditLogService.recordAdminFailure("BIOMETRIC_TEMPLATE_REVOKE", "BIOMETRIC_TEMPLATE", String.valueOf(id), servletRequest.getRemoteAddr(), exception.getMessage(), Map.of("revokeReason", request.revokeReason()));
            throw exception;
        }
    }

    private Map<String, Object> auditDetail(BiometricTemplateVO template) {
        return Map.of(
                "userId", template.userId(),
                "biometricType", template.biometricType(),
                "auditStatus", template.auditStatus(),
                "templateStatus", template.templateStatus(),
                "versionNo", template.versionNo()
        );
    }
}
