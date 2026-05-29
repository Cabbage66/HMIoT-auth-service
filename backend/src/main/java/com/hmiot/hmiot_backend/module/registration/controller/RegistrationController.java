package com.hmiot.hmiot_backend.module.registration.controller;

import com.hmiot.hmiot_backend.common.response.ApiResponse;
import com.hmiot.hmiot_backend.module.audit.service.AuditLogService;
import com.hmiot.hmiot_backend.module.registration.dto.RegistrationProgressQueryRequest;
import com.hmiot.hmiot_backend.module.registration.dto.RegistrationSubmitRequest;
import com.hmiot.hmiot_backend.module.registration.service.RegistrationService;
import com.hmiot.hmiot_backend.module.registration.vo.RegistrationProgressVO;
import com.hmiot.hmiot_backend.module.registration.vo.RegistrationSubmitResponse;
import com.hmiot.hmiot_backend.module.user.vo.PageVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/registrations")
public class RegistrationController {
    private final RegistrationService registrationService;
    private final AuditLogService auditLogService;

    public RegistrationController(RegistrationService registrationService, AuditLogService auditLogService) {
        this.registrationService = registrationService;
        this.auditLogService = auditLogService;
    }

    @PostMapping
    public ApiResponse<RegistrationSubmitResponse> submit(@Valid @RequestBody RegistrationSubmitRequest request, HttpServletRequest servletRequest) {
        RegistrationSubmitResponse response = registrationService.submit(request);
        auditLogService.recordSystemOperation("USER_REGISTRATION_SUBMIT", "REGISTRATION_REQUEST", String.valueOf(response.registrationId()), servletRequest.getRemoteAddr(), null, Map.of(
                "username", response.username(),
                "status", response.status()
        ));
        return ApiResponse.success(response);
    }

    @GetMapping("/progress")
    public ApiResponse<PageVO<RegistrationProgressVO>> progress(@Valid RegistrationProgressQueryRequest request, HttpServletRequest servletRequest) {
        PageVO<RegistrationProgressVO> response = registrationService.progress(request);
        auditLogService.recordSystemOperation("USER_REGISTRATION_PROGRESS_QUERY", "REGISTRATION_REQUEST", request.username(), servletRequest.getRemoteAddr(), null, Map.of(
                "username", request.username(),
                "resultSize", response.records().size()
        ));
        return ApiResponse.success(response);
    }
}
