package com.hmiot.hmiot_backend.module.biometric.controller;

import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.common.response.ApiResponse;
import com.hmiot.hmiot_backend.common.security.CurrentUser;
import com.hmiot.hmiot_backend.module.audit.service.AuditLogService;
import com.hmiot.hmiot_backend.module.biometric.dto.BiometricAuthRecordQueryRequest;
import com.hmiot.hmiot_backend.module.biometric.dto.FingerprintChallengeRequest;
import com.hmiot.hmiot_backend.module.biometric.dto.FingerprintSubmitRequest;
import com.hmiot.hmiot_backend.module.biometric.dto.FingerprintVerifyRequest;
import com.hmiot.hmiot_backend.module.biometric.service.BiometricTemplateService;
import com.hmiot.hmiot_backend.module.biometric.service.FingerprintAuthService;
import com.hmiot.hmiot_backend.module.biometric.vo.FingerprintAuthRecordStatusVO;
import com.hmiot.hmiot_backend.module.biometric.vo.FingerprintChallengeResponse;
import com.hmiot.hmiot_backend.module.biometric.vo.FingerprintHealthResponse;
import com.hmiot.hmiot_backend.module.biometric.vo.FingerprintTemplateStatusVO;
import com.hmiot.hmiot_backend.module.biometric.vo.FingerprintVerifyResponse;
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
@RequestMapping("/api/v1/fingerprints")
public class FingerprintController {
    private final FingerprintAuthService fingerprintAuthService;
    private final BiometricTemplateService biometricTemplateService;
    private final AuditLogService auditLogService;

    public FingerprintController(FingerprintAuthService fingerprintAuthService, BiometricTemplateService biometricTemplateService, AuditLogService auditLogService) {
        this.fingerprintAuthService = fingerprintAuthService;
        this.biometricTemplateService = biometricTemplateService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/health")
    public ApiResponse<FingerprintHealthResponse> health() {
        return ApiResponse.success(fingerprintAuthService.health());
    }

    @PostMapping("/challenge")
    public ApiResponse<FingerprintChallengeResponse> challenge(@RequestBody(required = false) FingerprintChallengeRequest request, HttpServletRequest servletRequest) {
        CurrentUser currentUser = currentUser();
        FingerprintChallengeResponse response = fingerprintAuthService.requestChallenge(currentUser, request == null ? new FingerprintChallengeRequest(null, null) : request);
        auditLogService.recordUserOperationSuccess(currentUser.userId(), currentUser.username(), "FINGERPRINT_CHALLENGE", "AUTH_SESSION", response.sessionId(), servletRequest.getRemoteAddr(), Map.of(
                "challengeId", response.challengeId(),
                "expiresInSeconds", response.expiresInSeconds(),
                "keyId", response.keyId() == null ? "" : response.keyId()
        ));
        return ApiResponse.success(response);
    }

    @PostMapping("/enroll")
    public ApiResponse<FingerprintTemplateStatusVO> enroll(@Valid @RequestBody FingerprintSubmitRequest request, HttpServletRequest servletRequest) {
        CurrentUser currentUser = currentUser();
        try {
            FingerprintTemplateStatusVO template = FingerprintTemplateStatusVO.from(biometricTemplateService.toTemplateVO(fingerprintAuthService.enroll(currentUser, request)));
            auditLogService.recordUserOperationSuccess(currentUser.userId(), currentUser.username(), "FINGERPRINT_ENROLL", "BIOMETRIC_TEMPLATE", String.valueOf(template.id()), servletRequest.getRemoteAddr(), Map.of(
                    "templateId", template.id(),
                    "auditStatus", template.auditStatus(),
                    "templateStatus", template.templateStatus(),
                    "versionNo", template.versionNo()
            ));
            return ApiResponse.success(template);
        } catch (RuntimeException exception) {
            auditLogService.recordUserOperationFailure(currentUser.userId(), currentUser.username(), "FINGERPRINT_ENROLL", "BIOMETRIC_TEMPLATE", null, servletRequest.getRemoteAddr(), exception.getMessage(), Map.of(
                    "keyId", request.keyId()
            ));
            throw exception;
        }
    }

    @PostMapping("/recollect")
    public ApiResponse<FingerprintTemplateStatusVO> recollect(@Valid @RequestBody FingerprintSubmitRequest request, HttpServletRequest servletRequest) {
        CurrentUser currentUser = currentUser();
        try {
            FingerprintTemplateStatusVO template = FingerprintTemplateStatusVO.from(biometricTemplateService.toTemplateVO(fingerprintAuthService.recollect(currentUser, request)));
            auditLogService.recordUserOperationSuccess(currentUser.userId(), currentUser.username(), "FINGERPRINT_RECOLLECT", "BIOMETRIC_TEMPLATE", String.valueOf(template.id()), servletRequest.getRemoteAddr(), Map.of(
                    "templateId", template.id(),
                    "auditStatus", template.auditStatus(),
                    "templateStatus", template.templateStatus(),
                    "versionNo", template.versionNo()
            ));
            return ApiResponse.success(template);
        } catch (RuntimeException exception) {
            auditLogService.recordUserOperationFailure(currentUser.userId(), currentUser.username(), "FINGERPRINT_RECOLLECT", "BIOMETRIC_TEMPLATE", null, servletRequest.getRemoteAddr(), exception.getMessage(), Map.of(
                    "keyId", request.keyId()
            ));
            throw exception;
        }
    }

    @PostMapping("/verify")
    public ApiResponse<FingerprintVerifyResponse> verify(@Valid @RequestBody FingerprintVerifyRequest request, HttpServletRequest servletRequest) {
        CurrentUser currentUser = currentUser();
        try {
            FingerprintVerifyResponse response = fingerprintAuthService.verify(currentUser, request);
            auditLogService.recordUserOperationSuccess(currentUser.userId(), currentUser.username(), "FINGERPRINT_VERIFY", "BIOMETRIC_AUTH_RECORD", String.valueOf(response.recordId()), servletRequest.getRemoteAddr(), Map.of(
                    "recordId", response.recordId(),
                    "verified", response.verified(),
                    "failReason", response.failReason() == null ? "" : response.failReason()
            ));
            return ApiResponse.success(response);
        } catch (RuntimeException exception) {
            auditLogService.recordUserOperationFailure(currentUser.userId(), currentUser.username(), "FINGERPRINT_VERIFY", "BIOMETRIC_AUTH_RECORD", null, servletRequest.getRemoteAddr(), exception.getMessage(), Map.of(
                    "keyId", request.keyId()
            ));
            throw exception;
        }
    }

    @GetMapping("/me")
    public ApiResponse<List<FingerprintTemplateStatusVO>> myTemplates() {
        CurrentUser currentUser = currentUser();
        return ApiResponse.success(
                biometricTemplateService.listUserFingerprints(currentUser.userId())
                        .stream()
                        .map(FingerprintTemplateStatusVO::from)
                        .toList()
        );
    }

    @GetMapping("/auth-records/me")
    public ApiResponse<PageVO<FingerprintAuthRecordStatusVO>> myAuthRecords(BiometricAuthRecordQueryRequest request) {
        CurrentUser currentUser = currentUser();
        BiometricAuthRecordQueryRequest scoped = new BiometricAuthRecordQueryRequest(
                currentUser.userId(),
                BiometricTemplateService.FINGERPRINT,
                request.authResult(),
                request.startTime(),
                request.endTime(),
                request.current(),
                request.size()
        );
        PageVO<com.hmiot.hmiot_backend.module.biometric.vo.BiometricAuthRecordVO> records = biometricTemplateService.listAuthRecords(scoped);
        PageVO<FingerprintAuthRecordStatusVO> page = new PageVO<>(
                records.current(),
                records.size(),
                records.total(),
                records.records().stream().map(FingerprintAuthRecordStatusVO::from).toList()
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

