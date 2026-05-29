package com.hmiot.hmiot_backend.module.biometric.controller;

import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.common.response.ApiResponse;
import com.hmiot.hmiot_backend.common.security.CurrentUser;
import com.hmiot.hmiot_backend.module.audit.service.AuditLogService;
import com.hmiot.hmiot_backend.module.biometric.dto.BiometricAuthRecordQueryRequest;
import com.hmiot.hmiot_backend.module.biometric.dto.DynamicBiometricQueryRequest;
import com.hmiot.hmiot_backend.module.biometric.dto.DynamicBiometricReportRequest;
import com.hmiot.hmiot_backend.module.biometric.service.BiometricTemplateService;
import com.hmiot.hmiot_backend.module.biometric.service.DynamicBiometricAuthService;
import com.hmiot.hmiot_backend.module.biometric.vo.DynamicBiometricAuthRecordStatusVO;
import com.hmiot.hmiot_backend.module.biometric.vo.DynamicBiometricHealthResponse;
import com.hmiot.hmiot_backend.module.biometric.vo.DynamicBiometricVerifyResponse;
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

import java.util.Map;

@RestController
@RequestMapping("/api/v1/dynamic-biometrics")
public class DynamicBiometricController {
    private final DynamicBiometricAuthService dynamicBiometricAuthService;
    private final BiometricTemplateService biometricTemplateService;
    private final AuditLogService auditLogService;

    public DynamicBiometricController(DynamicBiometricAuthService dynamicBiometricAuthService, BiometricTemplateService biometricTemplateService, AuditLogService auditLogService) {
        this.dynamicBiometricAuthService = dynamicBiometricAuthService;
        this.biometricTemplateService = biometricTemplateService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/health")
    public ApiResponse<DynamicBiometricHealthResponse> health() {
        return ApiResponse.success(dynamicBiometricAuthService.health());
    }

    @PostMapping("/query")
    public ApiResponse<DynamicBiometricVerifyResponse> query(@RequestBody DynamicBiometricQueryRequest request, HttpServletRequest servletRequest) {
        CurrentUser currentUser = currentUser();
        try {
            DynamicBiometricVerifyResponse response = dynamicBiometricAuthService.query(currentUser, request);
            auditLogService.recordUserOperationSuccess(currentUser.userId(), currentUser.username(), "DYNAMIC_BIOMETRIC_QUERY", "BIOMETRIC_AUTH_RECORD", String.valueOf(response.recordId()), servletRequest.getRemoteAddr(), auditDetail(response));
            return ApiResponse.success(response);
        } catch (RuntimeException exception) {
            auditLogService.recordUserOperationFailure(currentUser.userId(), currentUser.username(), "DYNAMIC_BIOMETRIC_QUERY", "BIOMETRIC_AUTH_RECORD", null, servletRequest.getRemoteAddr(), exception.getMessage(), Map.of(
                    "requestId", request.requestId() == null ? "" : request.requestId(),
                    "biometricType", request.biometricType() == null ? BiometricTemplateService.DYNAMIC_BIOMETRIC : request.biometricType()
            ));
            throw exception;
        }
    }

    @PostMapping("/reports")
    public ApiResponse<DynamicBiometricVerifyResponse> report(@Valid @RequestBody DynamicBiometricReportRequest request, HttpServletRequest servletRequest) {
        try {
            DynamicBiometricVerifyResponse response = dynamicBiometricAuthService.report(request);
            auditLogService.recordUserOperationSuccess(request.userId(), String.valueOf(request.userId()), "DYNAMIC_BIOMETRIC_REPORT", "BIOMETRIC_AUTH_RECORD", String.valueOf(response.recordId()), servletRequest.getRemoteAddr(), auditDetail(response));
            return ApiResponse.success(response);
        } catch (RuntimeException exception) {
            auditLogService.recordUserOperationFailure(request.userId(), String.valueOf(request.userId()), "DYNAMIC_BIOMETRIC_REPORT", "BIOMETRIC_AUTH_RECORD", null, servletRequest.getRemoteAddr(), exception.getMessage(), Map.of(
                    "requestId", request.requestId(),
                    "userId", request.userId(),
                    "externalSubjectId", request.externalSubjectId()
            ));
            throw exception;
        }
    }

    @GetMapping("/auth-records/me")
    public ApiResponse<PageVO<DynamicBiometricAuthRecordStatusVO>> myAuthRecords(BiometricAuthRecordQueryRequest request) {
        CurrentUser currentUser = currentUser();
        PageVO<com.hmiot.hmiot_backend.module.biometric.vo.BiometricAuthRecordVO> records = biometricTemplateService.listUserDynamicBiometricAuthRecords(currentUser.userId(), request);
        PageVO<DynamicBiometricAuthRecordStatusVO> page = new PageVO<>(records.current(), records.size(), records.total(), records.records().stream().map(DynamicBiometricAuthRecordStatusVO::from).toList());
        return ApiResponse.success(page);
    }

    private CurrentUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser currentUser)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前用户未登录");
        }
        return currentUser;
    }

    private Map<String, Object> auditDetail(DynamicBiometricVerifyResponse response) {
        return Map.of(
                "recordId", response.recordId(),
                "requestId", response.requestId() == null ? "" : response.requestId(),
                "matched", response.matched(),
                "score", response.score(),
                "threshold", response.threshold(),
                "failReason", response.failReason() == null ? "" : response.failReason()
        );
    }
}
