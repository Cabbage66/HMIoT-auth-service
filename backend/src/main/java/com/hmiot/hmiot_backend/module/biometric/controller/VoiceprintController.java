package com.hmiot.hmiot_backend.module.biometric.controller;

import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.common.response.ApiResponse;
import com.hmiot.hmiot_backend.common.security.CurrentUser;
import com.hmiot.hmiot_backend.module.audit.service.AuditLogService;
import com.hmiot.hmiot_backend.module.biometric.dto.BiometricAuthRecordQueryRequest;
import com.hmiot.hmiot_backend.module.biometric.dto.VoiceprintEnrollRequest;
import com.hmiot.hmiot_backend.module.biometric.dto.VoiceprintVerifyRequest;
import com.hmiot.hmiot_backend.module.biometric.service.BiometricTemplateService;
import com.hmiot.hmiot_backend.module.biometric.service.VoiceprintAuthService;
import com.hmiot.hmiot_backend.module.biometric.vo.VoiceprintAuthRecordStatusVO;
import com.hmiot.hmiot_backend.module.biometric.vo.VoiceprintHealthResponse;
import com.hmiot.hmiot_backend.module.biometric.vo.VoiceprintTemplateStatusVO;
import com.hmiot.hmiot_backend.module.biometric.vo.VoiceprintVerifyResponse;
import com.hmiot.hmiot_backend.module.user.vo.PageVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/voiceprints")
public class VoiceprintController {
    private final VoiceprintAuthService voiceprintAuthService;
    private final BiometricTemplateService biometricTemplateService;
    private final AuditLogService auditLogService;

    public VoiceprintController(VoiceprintAuthService voiceprintAuthService, BiometricTemplateService biometricTemplateService, AuditLogService auditLogService) {
        this.voiceprintAuthService = voiceprintAuthService;
        this.biometricTemplateService = biometricTemplateService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/health")
    public ApiResponse<VoiceprintHealthResponse> health() {
        return ApiResponse.success(voiceprintAuthService.health());
    }

    @PostMapping("/enroll")
    public ApiResponse<VoiceprintTemplateStatusVO> enroll(@Valid @RequestBody VoiceprintEnrollRequest request, HttpServletRequest servletRequest) {
        CurrentUser currentUser = currentUser();
        try {
            VoiceprintTemplateStatusVO template = VoiceprintTemplateStatusVO.from(voiceprintAuthService.enroll(currentUser, request));
            auditLogService.recordUserOperationSuccess(currentUser.userId(), currentUser.username(), "VOICEPRINT_ENROLL", "BIOMETRIC_TEMPLATE", String.valueOf(template.id()), servletRequest.getRemoteAddr(), templateAuditDetail(template));
            return ApiResponse.success(template);
        } catch (RuntimeException exception) {
            auditLogService.recordUserOperationFailure(currentUser.userId(), currentUser.username(), "VOICEPRINT_ENROLL", "BIOMETRIC_TEMPLATE", null, servletRequest.getRemoteAddr(), exception.getMessage(), requestAuditDetail(request.audioFormat(), request.sampleRate()));
            throw exception;
        }
    }

    @PostMapping("/recollect")
    public ApiResponse<VoiceprintTemplateStatusVO> recollect(@Valid @RequestBody VoiceprintEnrollRequest request, HttpServletRequest servletRequest) {
        CurrentUser currentUser = currentUser();
        try {
            VoiceprintTemplateStatusVO template = VoiceprintTemplateStatusVO.from(voiceprintAuthService.recollect(currentUser, request));
            auditLogService.recordUserOperationSuccess(currentUser.userId(), currentUser.username(), "VOICEPRINT_RECOLLECT", "BIOMETRIC_TEMPLATE", String.valueOf(template.id()), servletRequest.getRemoteAddr(), templateAuditDetail(template));
            return ApiResponse.success(template);
        } catch (RuntimeException exception) {
            auditLogService.recordUserOperationFailure(currentUser.userId(), currentUser.username(), "VOICEPRINT_RECOLLECT", "BIOMETRIC_TEMPLATE", null, servletRequest.getRemoteAddr(), exception.getMessage(), requestAuditDetail(request.audioFormat(), request.sampleRate()));
            throw exception;
        }
    }

    @GetMapping("/me")
    public ApiResponse<List<VoiceprintTemplateStatusVO>> myTemplates() {
        CurrentUser currentUser = currentUser();
        return ApiResponse.success(voiceprintAuthService.myTemplates(currentUser).stream().map(VoiceprintTemplateStatusVO::from).toList());
    }

    @PostMapping("/verify")
    public ApiResponse<VoiceprintVerifyResponse> verify(@Valid @RequestBody VoiceprintVerifyRequest request, HttpServletRequest servletRequest) {
        CurrentUser currentUser = currentUser();
        try {
            VoiceprintVerifyResponse response = voiceprintAuthService.verify(currentUser, request);
            auditLogService.recordUserOperationSuccess(currentUser.userId(), currentUser.username(), "VOICEPRINT_VERIFY", "BIOMETRIC_AUTH_RECORD", String.valueOf(response.recordId()), servletRequest.getRemoteAddr(), Map.of(
                    "recordId", response.recordId(),
                    "verified", response.verified(),
                    "score", response.score(),
                    "threshold", response.threshold(),
                    "failReason", response.failReason() == null ? "" : response.failReason()
            ));
            return ApiResponse.success(response);
        } catch (RuntimeException exception) {
            auditLogService.recordUserOperationFailure(currentUser.userId(), currentUser.username(), "VOICEPRINT_VERIFY", "BIOMETRIC_AUTH_RECORD", null, servletRequest.getRemoteAddr(), exception.getMessage(), requestAuditDetail(request.audioFormat(), request.sampleRate()));
            throw exception;
        }
    }

    @GetMapping("/auth-records/me")
    public ApiResponse<PageVO<VoiceprintAuthRecordStatusVO>> myAuthRecords(BiometricAuthRecordQueryRequest request) {
        CurrentUser currentUser = currentUser();
        PageVO<com.hmiot.hmiot_backend.module.biometric.vo.BiometricAuthRecordVO> records = biometricTemplateService.listUserAuthRecords(currentUser.userId(), request);
        PageVO<VoiceprintAuthRecordStatusVO> page = new PageVO<>(records.current(), records.size(), records.total(), records.records().stream().map(VoiceprintAuthRecordStatusVO::from).toList());
        return ApiResponse.success(page);
    }

    private CurrentUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser currentUser)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前用户未登录");
        }
        return currentUser;
    }

    private Map<String, Object> templateAuditDetail(VoiceprintTemplateStatusVO template) {
        return Map.of(
                "templateId", template.id(),
                "biometricType", template.biometricType(),
                "auditStatus", template.auditStatus(),
                "templateStatus", template.templateStatus(),
                "versionNo", template.versionNo()
        );
    }

    private Map<String, Object> requestAuditDetail(String audioFormat, Integer sampleRate) {
        return Map.of(
                "audioFormat", audioFormat == null ? "" : audioFormat,
                "sampleRate", sampleRate == null ? "" : sampleRate
        );
    }
}
