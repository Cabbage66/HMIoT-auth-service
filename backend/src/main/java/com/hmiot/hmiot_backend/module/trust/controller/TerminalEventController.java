package com.hmiot.hmiot_backend.module.trust.controller;

import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.common.response.ApiResponse;
import com.hmiot.hmiot_backend.common.security.CurrentUser;
import com.hmiot.hmiot_backend.module.audit.service.AuditLogService;
import com.hmiot.hmiot_backend.module.trust.dto.TerminalEventQueryRequest;
import com.hmiot.hmiot_backend.module.trust.dto.TerminalEventReportRequest;
import com.hmiot.hmiot_backend.module.trust.service.TerminalEventService;
import com.hmiot.hmiot_backend.module.trust.vo.TerminalEventVO;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/terminal-events")
public class TerminalEventController {
    private final TerminalEventService terminalEventService;
    private final AuditLogService auditLogService;

    public TerminalEventController(TerminalEventService terminalEventService, AuditLogService auditLogService) {
        this.terminalEventService = terminalEventService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/reports")
    public ApiResponse<TerminalEventVO> report(@Valid @RequestBody TerminalEventReportRequest request, HttpServletRequest servletRequest) {
        CurrentUser currentUser = currentUser();
        try {
            TerminalEventVO event = terminalEventService.report(request, currentUser);
            auditLogService.recordUserOperationSuccess(currentUser.userId(), currentUser.username(), "TERMINAL_EVENT_REPORT", "TERMINAL_EVENT", String.valueOf(event.id()), servletRequest.getRemoteAddr(), eventAuditDetail(event));
            return ApiResponse.success(event);
        } catch (RuntimeException exception) {
            auditLogService.recordUserOperationFailure(currentUser.userId(), currentUser.username(), "TERMINAL_EVENT_REPORT_FAIL", "TERMINAL_EVENT", null, servletRequest.getRemoteAddr(), exception.getMessage(), Map.of(
                    "eventType", request.eventType() == null ? "" : request.eventType(),
                    "eventLevel", request.eventLevel() == null ? "" : request.eventLevel()
            ));
            throw exception;
        }
    }

    @GetMapping("/my/recent")
    public ApiResponse<List<TerminalEventVO>> myRecent() {
        return ApiResponse.success(terminalEventService.recentForCurrentUser(currentUser()));
    }

    @GetMapping
    public ApiResponse<PageVO<TerminalEventVO>> list(TerminalEventQueryRequest request) {
        return ApiResponse.success(terminalEventService.listEvents(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<TerminalEventVO> detail(@PathVariable Long id) {
        return ApiResponse.success(terminalEventService.detail(id));
    }

    private CurrentUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser currentUser)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前用户未登录");
        }
        return currentUser;
    }

    private Map<String, Object> eventAuditDetail(TerminalEventVO event) {
        return Map.of(
                "eventId", event.id(),
                "terminalId", event.terminalId(),
                "userId", event.userId(),
                "eventType", event.eventType(),
                "eventLevel", event.eventLevel(),
                "eventTime", event.eventTime() == null ? "" : event.eventTime()
        );
    }
}
