package com.hmiot.hmiot_backend.module.registration.controller;

import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.common.response.ApiResponse;
import com.hmiot.hmiot_backend.common.security.CurrentAdmin;
import com.hmiot.hmiot_backend.module.audit.service.AuditLogService;
import com.hmiot.hmiot_backend.module.registration.dto.AdminRegistrationQueryRequest;
import com.hmiot.hmiot_backend.module.registration.dto.OfflineRegistrationBatchRequest;
import com.hmiot.hmiot_backend.module.registration.dto.OfflineRegistrationCreateRequest;
import com.hmiot.hmiot_backend.module.registration.dto.RegistrationRejectRequest;
import com.hmiot.hmiot_backend.module.registration.service.RegistrationService;
import com.hmiot.hmiot_backend.module.registration.vo.AdminRegistrationVO;
import com.hmiot.hmiot_backend.module.registration.vo.OfflineRegistrationBatchResponse;
import com.hmiot.hmiot_backend.module.registration.vo.RegistrationSubmitResponse;
import com.hmiot.hmiot_backend.module.user.vo.PageVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/registrations")
public class AdminRegistrationController {
    private final RegistrationService registrationService;
    private final AuditLogService auditLogService;

    public AdminRegistrationController(RegistrationService registrationService, AuditLogService auditLogService) {
        this.registrationService = registrationService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<PageVO<AdminRegistrationVO>> list(AdminRegistrationQueryRequest request) {
        return ApiResponse.success(registrationService.listAdminRegistrations(request));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<AdminRegistrationVO> approve(@PathVariable Long id, HttpServletRequest servletRequest) {
        try {
            AdminRegistrationVO response = registrationService.approveRegistration(id, currentAdmin().adminId());
            auditLogService.recordAdminSuccess("REGISTRATION_APPROVE", "REGISTRATION_REQUEST", String.valueOf(id), servletRequest.getRemoteAddr(), Map.of(
                    "userId", response.userId(),
                    "username", response.username(),
                    "status", response.status()
            ));
            return ApiResponse.success(response);
        } catch (RuntimeException exception) {
            auditLogService.recordAdminFailure("REGISTRATION_APPROVE", "REGISTRATION_REQUEST", String.valueOf(id), servletRequest.getRemoteAddr(), exception.getMessage(), Map.of());
            throw exception;
        }
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<AdminRegistrationVO> reject(@PathVariable Long id, @Valid @RequestBody RegistrationRejectRequest request, HttpServletRequest servletRequest) {
        try {
            AdminRegistrationVO response = registrationService.rejectRegistration(id, currentAdmin().adminId(), request.rejectReason());
            auditLogService.recordAdminSuccess("REGISTRATION_REJECT", "REGISTRATION_REQUEST", String.valueOf(id), servletRequest.getRemoteAddr(), Map.of(
                    "userId", response.userId(),
                    "username", response.username(),
                    "rejectReason", request.rejectReason()
            ));
            return ApiResponse.success(response);
        } catch (RuntimeException exception) {
            auditLogService.recordAdminFailure("REGISTRATION_REJECT", "REGISTRATION_REQUEST", String.valueOf(id), servletRequest.getRemoteAddr(), exception.getMessage(), Map.of(
                    "rejectReason", request.rejectReason()
            ));
            throw exception;
        }
    }

    @PostMapping("/offline")
    public ApiResponse<RegistrationSubmitResponse> offline(@Valid @RequestBody OfflineRegistrationCreateRequest request, HttpServletRequest servletRequest) {
        try {
            RegistrationSubmitResponse response = registrationService.createOfflineRegistration(request, currentAdmin().adminId());
            auditLogService.recordAdminSuccess("REGISTRATION_OFFLINE_CREATE", "REGISTRATION_REQUEST", String.valueOf(response.registrationId()), servletRequest.getRemoteAddr(), Map.of(
                    "userId", response.userId(),
                    "username", response.username(),
                    "status", response.status(),
                    "autoApprove", request.autoApprove() == null || request.autoApprove()
            ));
            return ApiResponse.success(response);
        } catch (RuntimeException exception) {
            auditLogService.recordAdminFailure("REGISTRATION_OFFLINE_CREATE", "REGISTRATION_REQUEST", null, servletRequest.getRemoteAddr(), exception.getMessage(), Map.of(
                    "username", request.username()
            ));
            throw exception;
        }
    }

    @PostMapping("/offline/batch")
    public ApiResponse<OfflineRegistrationBatchResponse> offlineBatch(@Valid @RequestBody OfflineRegistrationBatchRequest request, HttpServletRequest servletRequest) {
        OfflineRegistrationBatchResponse response = registrationService.createOfflineBatch(request, currentAdmin().adminId());
        auditLogService.recordAdminSuccess("REGISTRATION_OFFLINE_BATCH", "REGISTRATION_REQUEST", null, servletRequest.getRemoteAddr(), Map.of(
                "total", response.total(),
                "successCount", response.successCount(),
                "failCount", response.failCount()
        ));
        return ApiResponse.success(response);
    }

    private CurrentAdmin currentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentAdmin currentAdmin)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "管理员未登录");
        }
        return currentAdmin;
    }
}
