package com.hmiot.hmiot_backend.module.ticket.controller;

import com.hmiot.hmiot_backend.common.response.ApiResponse;
import com.hmiot.hmiot_backend.common.security.CurrentUser;
import com.hmiot.hmiot_backend.module.audit.service.AuditLogService;
import com.hmiot.hmiot_backend.module.ticket.dto.SsoAccessRequest;
import com.hmiot.hmiot_backend.module.ticket.dto.SsoValidateRequest;
import com.hmiot.hmiot_backend.module.ticket.service.IdentityTicketService;
import com.hmiot.hmiot_backend.module.ticket.service.SsoService;
import com.hmiot.hmiot_backend.module.ticket.vo.SsoAccessResponse;
import com.hmiot.hmiot_backend.module.ticket.vo.SsoValidateResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/sso")
public class SsoController {
    private final SsoService ssoService;
    private final IdentityTicketService identityTicketService;
    private final AuditLogService auditLogService;

    public SsoController(SsoService ssoService, IdentityTicketService identityTicketService, AuditLogService auditLogService) {
        this.ssoService = ssoService;
        this.identityTicketService = identityTicketService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/access")
    public ApiResponse<SsoAccessResponse> access(@Valid @RequestBody SsoAccessRequest request, HttpServletRequest servletRequest) {
        CurrentUser currentUser = identityTicketService.currentUser();
        SsoAccessResponse response = ssoService.access(request, currentUser);
        auditLogService.recordUserOperationSuccess(currentUser.userId(), currentUser.username(), "SSO_ACCESS", "APP_RESOURCE", response.appCode(), servletRequest.getRemoteAddr(), Map.of(
                "targetSystem", response.targetSystem(),
                "ticketId", response.ticket().ticketId(),
                "terminalId", response.ticket().terminalId(),
                "terminalCode", response.ticket().terminalCode() == null ? "" : response.ticket().terminalCode()
        ));
        return ApiResponse.success(response);
    }

    @PostMapping("/validate")
    public ApiResponse<SsoValidateResponse> validate(@Valid @RequestBody SsoValidateRequest request, HttpServletRequest servletRequest) {
        SsoValidateResponse response = ssoService.validate(request, servletRequest.getRemoteAddr());
        auditLogService.recordSystemOperation(response.success() ? "SSO_VALIDATE" : "SSO_VALIDATE_FAIL", "IDENTITY_TICKET", request.ticketId(), servletRequest.getRemoteAddr(), response.success() ? null : response.message(), Map.of(
                "sourceSystem", request.sourceSystem(),
                "targetSystem", request.targetSystem(),
                "appCode", request.appCode(),
                "success", response.success()
        ));
        return ApiResponse.success(response);
    }
}
