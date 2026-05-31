package com.hmiot.hmiot_backend.module.biometric.controller;

import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.common.response.ApiResponse;
import com.hmiot.hmiot_backend.common.security.CurrentUser;
import com.hmiot.hmiot_backend.module.audit.service.AuditLogService;
import com.hmiot.hmiot_backend.module.biometric.dto.BiometricAuthRecordQueryRequest;
import com.hmiot.hmiot_backend.module.biometric.dto.FaceSubmitRequest;
import com.hmiot.hmiot_backend.module.biometric.dto.FaceVerifyRequest;
import com.hmiot.hmiot_backend.module.biometric.service.BiometricTemplateService;
import com.hmiot.hmiot_backend.module.biometric.service.FaceAuthService;
import com.hmiot.hmiot_backend.module.biometric.vo.FaceAuthRecordStatusVO;
import com.hmiot.hmiot_backend.module.biometric.vo.FaceHealthResponse;
import com.hmiot.hmiot_backend.module.biometric.vo.FaceTemplateStatusVO;
import com.hmiot.hmiot_backend.module.biometric.vo.FaceVerifyResponse;
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
@RequestMapping("/api/v1/faces")
public class FaceController {
    private final FaceAuthService faceAuthService;
    private final BiometricTemplateService biometricTemplateService;
    private final AuditLogService auditLogService;

    public FaceController(FaceAuthService faceAuthService, BiometricTemplateService biometricTemplateService, AuditLogService auditLogService) {
        this.faceAuthService = faceAuthService;
        this.biometricTemplateService = biometricTemplateService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/health")
    public ApiResponse<FaceHealthResponse> health() {
        return ApiResponse.success(faceAuthService.health());
    }

    @PostMapping("/enroll")
    public ApiResponse<FaceTemplateStatusVO> enroll(@Valid @RequestBody FaceSubmitRequest request, HttpServletRequest servletRequest) {
        CurrentUser currentUser = currentUser();
        try {
            FaceTemplateStatusVO template = FaceTemplateStatusVO.from(faceAuthService.enroll(currentUser, request));
            auditLogService.recordUserOperationSuccess(currentUser.userId(), currentUser.username(), "FACE_ENROLL", "BIOMETRIC_TEMPLATE", String.valueOf(template.id()), servletRequest.getRemoteAddr(), Map.of(
                    "templateId", template.id(),
                    "auditStatus", template.auditStatus(),
                    "templateStatus", template.templateStatus(),
                    "versionNo", template.versionNo()
            ));
            return ApiResponse.success(template);
        } catch (RuntimeException exception) {
            auditLogService.recordUserOperationFailure(currentUser.userId(), currentUser.username(), "FACE_ENROLL", "BIOMETRIC_TEMPLATE", null, servletRequest.getRemoteAddr(), exception.getMessage(), Map.of());
            throw exception;
        }
    }

    @PostMapping("/recollect")
    public ApiResponse<FaceTemplateStatusVO> recollect(@Valid @RequestBody FaceSubmitRequest request, HttpServletRequest servletRequest) {
        CurrentUser currentUser = currentUser();
        try {
            FaceTemplateStatusVO template = FaceTemplateStatusVO.from(faceAuthService.recollect(currentUser, request));
            auditLogService.recordUserOperationSuccess(currentUser.userId(), currentUser.username(), "FACE_RECOLLECT", "BIOMETRIC_TEMPLATE", String.valueOf(template.id()), servletRequest.getRemoteAddr(), Map.of(
                    "templateId", template.id(),
                    "auditStatus", template.auditStatus(),
                    "templateStatus", template.templateStatus(),
                    "versionNo", template.versionNo()
            ));
            return ApiResponse.success(template);
        } catch (RuntimeException exception) {
            auditLogService.recordUserOperationFailure(currentUser.userId(), currentUser.username(), "FACE_RECOLLECT", "BIOMETRIC_TEMPLATE", null, servletRequest.getRemoteAddr(), exception.getMessage(), Map.of());
            throw exception;
        }
    }

    @PostMapping("/verify")
    public ApiResponse<FaceVerifyResponse> verify(@Valid @RequestBody FaceVerifyRequest request, HttpServletRequest servletRequest) {
        CurrentUser currentUser = currentUser();
        try {
            FaceVerifyResponse response = faceAuthService.verify(currentUser, request);
            auditLogService.recordUserOperationSuccess(currentUser.userId(), currentUser.username(), "FACE_VERIFY", "BIOMETRIC_AUTH_RECORD", String.valueOf(response.recordId()), servletRequest.getRemoteAddr(), Map.of(
                    "recordId", response.recordId(),
                    "verified", response.verified(),
                    "score", response.score() == null ? "" : response.score(),
                    "threshold", response.threshold() == null ? "" : response.threshold(),
                    "failReason", response.failReason() == null ? "" : response.failReason()
            ));
            return ApiResponse.success(response);
        } catch (RuntimeException exception) {
            auditLogService.recordUserOperationFailure(currentUser.userId(), currentUser.username(), "FACE_VERIFY", "BIOMETRIC_AUTH_RECORD", null, servletRequest.getRemoteAddr(), exception.getMessage(), Map.of(
                    "requestId", request.requestId() == null ? "" : request.requestId()
            ));
            throw exception;
        }
    }

    @GetMapping("/me")
    public ApiResponse<List<FaceTemplateStatusVO>> myTemplates() {
        CurrentUser currentUser = currentUser();
        return ApiResponse.success(
                biometricTemplateService.listUserFaces(currentUser.userId())
                        .stream()
                        .map(FaceTemplateStatusVO::from)
                        .toList()
        );
    }

    @GetMapping("/auth-records/me")
    public ApiResponse<PageVO<FaceAuthRecordStatusVO>> myAuthRecords(BiometricAuthRecordQueryRequest request) {
        CurrentUser currentUser = currentUser();
        BiometricAuthRecordQueryRequest scoped = new BiometricAuthRecordQueryRequest(
                currentUser.userId(),
                BiometricTemplateService.FACE,
                request.authResult(),
                request.startTime(),
                request.endTime(),
                request.current(),
                request.size()
        );
        PageVO<com.hmiot.hmiot_backend.module.biometric.vo.BiometricAuthRecordVO> records = biometricTemplateService.listAuthRecords(scoped);
        PageVO<FaceAuthRecordStatusVO> page = new PageVO<>(
                records.current(),
                records.size(),
                records.total(),
                records.records().stream().map(FaceAuthRecordStatusVO::from).toList()
        );
        return ApiResponse.success(page);
    }

    private CurrentUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser currentUser)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前用户未登录");
        }
        return currentUser;
    }
}

