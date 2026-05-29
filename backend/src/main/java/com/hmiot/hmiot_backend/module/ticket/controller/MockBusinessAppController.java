package com.hmiot.hmiot_backend.module.ticket.controller;

import com.hmiot.hmiot_backend.common.response.ApiResponse;
import com.hmiot.hmiot_backend.module.audit.service.AuditLogService;
import com.hmiot.hmiot_backend.module.ticket.dto.SsoValidateRequest;
import com.hmiot.hmiot_backend.module.ticket.service.SsoService;
import com.hmiot.hmiot_backend.module.ticket.vo.SsoValidateResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/mock-business-app")
public class MockBusinessAppController {
    private final SsoService ssoService;
    private final AuditLogService auditLogService;

    public MockBusinessAppController(SsoService ssoService, AuditLogService auditLogService) {
        this.ssoService = ssoService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/access")
    public ApiResponse<SsoValidateResponse> access(@Valid @RequestBody SsoValidateRequest request, HttpServletRequest servletRequest) {
        SsoValidateResponse response = ssoService.validate(request, servletRequest.getRemoteAddr());
        auditLogService.recordSystemOperation(response.success() ? "MOCK_BUSINESS_APP_ACCESS" : "MOCK_BUSINESS_APP_ACCESS_FAIL", "APP_RESOURCE", request.appCode(), servletRequest.getRemoteAddr(), response.success() ? null : response.message(), Map.of(
                "sourceSystem", request.sourceSystem(),
                "targetSystem", request.targetSystem(),
                "ticketId", request.ticketId()
        ));
        return ApiResponse.success(response);
    }
}
