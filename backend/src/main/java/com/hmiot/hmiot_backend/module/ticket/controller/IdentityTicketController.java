package com.hmiot.hmiot_backend.module.ticket.controller;

import com.hmiot.hmiot_backend.common.response.ApiResponse;
import com.hmiot.hmiot_backend.common.security.CurrentUser;
import com.hmiot.hmiot_backend.module.audit.service.AuditLogService;
import com.hmiot.hmiot_backend.module.ticket.dto.TicketIssueRequest;
import com.hmiot.hmiot_backend.module.ticket.dto.TicketLifecycleRequest;
import com.hmiot.hmiot_backend.module.ticket.dto.TicketVerifyRequest;
import com.hmiot.hmiot_backend.module.ticket.service.IdentityTicketService;
import com.hmiot.hmiot_backend.module.ticket.vo.IdentityTicketVO;
import com.hmiot.hmiot_backend.module.ticket.vo.TicketVerifyResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/tickets")
public class IdentityTicketController {
    private final IdentityTicketService identityTicketService;
    private final AuditLogService auditLogService;

    public IdentityTicketController(IdentityTicketService identityTicketService, AuditLogService auditLogService) {
        this.identityTicketService = identityTicketService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/issue")
    public ApiResponse<IdentityTicketVO> issueTicket(@Valid @RequestBody TicketIssueRequest request, HttpServletRequest servletRequest) {
        CurrentUser currentUser = identityTicketService.currentUser();
        try {
            IdentityTicketVO ticket = identityTicketService.issueTicket(request, currentUser);
            auditLogService.recordUserOperationSuccess(currentUser.userId(), currentUser.username(), "IDENTITY_TICKET_ISSUE", "IDENTITY_TICKET", ticket.ticketId(), servletRequest.getRemoteAddr(), auditDetail(ticket));
            return ApiResponse.success(ticket);
        } catch (RuntimeException exception) {
            auditLogService.recordUserOperationFailure(currentUser.userId(), currentUser.username(), "IDENTITY_TICKET_ISSUE_FAIL", "IDENTITY_TICKET", null, servletRequest.getRemoteAddr(), exception.getMessage(), Map.of(
                    "appCode", request.appCode(),
                    "audienceSystem", request.audienceSystem() == null ? "" : request.audienceSystem()
            ));
            throw exception;
        }
    }

    @PostMapping("/verify")
    public ApiResponse<TicketVerifyResponse> verifyTicket(@Valid @RequestBody TicketVerifyRequest request, HttpServletRequest servletRequest) {
        TicketVerifyResponse response = identityTicketService.verifyTicket(request, servletRequest.getRemoteAddr());
        auditLogService.recordSystemOperation(response.verified() ? "IDENTITY_TICKET_VERIFY" : "IDENTITY_TICKET_VERIFY_FAIL", "IDENTITY_TICKET", request.ticketId(), servletRequest.getRemoteAddr(), response.verified() ? null : response.message(), Map.of(
                "appCode", request.appCode(),
                "verifierSystem", request.verifierSystem(),
                "verified", response.verified()
        ));
        return ApiResponse.success(response);
    }

    @PostMapping("/{ticketId}/refresh")
    public ApiResponse<IdentityTicketVO> refreshTicket(@PathVariable String ticketId, HttpServletRequest servletRequest) {
        CurrentUser currentUser = identityTicketService.currentUser();
        IdentityTicketVO ticket = identityTicketService.refreshTicket(ticketId, currentUser);
        auditLogService.recordUserOperationSuccess(currentUser.userId(), currentUser.username(), "IDENTITY_TICKET_REFRESH", "IDENTITY_TICKET", ticketId, servletRequest.getRemoteAddr(), auditDetail(ticket));
        return ApiResponse.success(ticket);
    }

    @PostMapping("/{ticketId}/keepalive")
    public ApiResponse<IdentityTicketVO> keepAlive(@PathVariable String ticketId, HttpServletRequest servletRequest) {
        CurrentUser currentUser = identityTicketService.currentUser();
        IdentityTicketVO ticket = identityTicketService.keepAlive(ticketId, currentUser);
        auditLogService.recordUserOperationSuccess(currentUser.userId(), currentUser.username(), "IDENTITY_TICKET_KEEPALIVE", "IDENTITY_TICKET", ticketId, servletRequest.getRemoteAddr(), auditDetail(ticket));
        return ApiResponse.success(ticket);
    }

    @PostMapping("/{ticketId}/revoke")
    public ApiResponse<IdentityTicketVO> revokeTicket(@PathVariable String ticketId, @RequestBody(required = false) TicketLifecycleRequest request, HttpServletRequest servletRequest) {
        Object principal = identityTicketService.currentPrincipal();
        IdentityTicketVO existing = identityTicketService.getTicket(ticketId);
        identityTicketService.ensureCanRevoke(existing, principal);
        IdentityTicketVO ticket = identityTicketService.revokeTicket(ticketId);
        auditLogService.recordSystemOperation("IDENTITY_TICKET_REVOKE", "IDENTITY_TICKET", ticketId, servletRequest.getRemoteAddr(), null, Map.of(
                "userId", ticket.userId(),
                "reason", request == null || request.reason() == null ? "" : request.reason()
        ));
        return ApiResponse.success(ticket);
    }

    private Map<String, Object> auditDetail(IdentityTicketVO ticket) {
        return Map.of(
                "ticketId", ticket.ticketId(),
                "appCode", ticket.appCode(),
                "audienceSystem", ticket.audienceSystem(),
                "terminalId", ticket.terminalId(),
                "terminalCode", ticket.terminalCode() == null ? "" : ticket.terminalCode(),
                "trustLevel", ticket.trustLevel()
        );
    }
}
