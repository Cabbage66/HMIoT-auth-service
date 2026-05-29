package com.hmiot.hmiot_backend.module.device.controller;

import com.hmiot.hmiot_backend.common.response.ApiResponse;
import com.hmiot.hmiot_backend.module.audit.service.AuditLogService;
import com.hmiot.hmiot_backend.module.device.dto.TerminalInfoCreateRequest;
import com.hmiot.hmiot_backend.module.device.dto.TerminalInfoQueryRequest;
import com.hmiot.hmiot_backend.module.device.dto.TerminalInfoStatusRequest;
import com.hmiot.hmiot_backend.module.device.dto.TerminalInfoUpdateRequest;
import com.hmiot.hmiot_backend.module.device.service.TerminalInfoService;
import com.hmiot.hmiot_backend.module.device.vo.TerminalInfoVO;
import com.hmiot.hmiot_backend.module.user.vo.PageVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/terminals")
public class TerminalInfoController {
    private final TerminalInfoService terminalInfoService;
    private final AuditLogService auditLogService;

    public TerminalInfoController(TerminalInfoService terminalInfoService, AuditLogService auditLogService) {
        this.terminalInfoService = terminalInfoService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<PageVO<TerminalInfoVO>> listTerminals(TerminalInfoQueryRequest request) {
        return ApiResponse.success(terminalInfoService.listTerminals(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<TerminalInfoVO> getTerminal(@PathVariable Long id) {
        return ApiResponse.success(terminalInfoService.getTerminal(id));
    }

    @PostMapping
    public ApiResponse<TerminalInfoVO> createTerminal(@Valid @RequestBody TerminalInfoCreateRequest request, HttpServletRequest servletRequest) {
        try {
            TerminalInfoVO terminal = terminalInfoService.createTerminal(request);
            auditLogService.recordAdminSuccess("TERMINAL_CREATE", "TERMINAL_INFO", String.valueOf(terminal.id()), servletRequest.getRemoteAddr(), auditDetail(terminal));
            return ApiResponse.success(terminal);
        } catch (RuntimeException exception) {
            auditLogService.recordAdminFailure("TERMINAL_CREATE", "TERMINAL_INFO", null, servletRequest.getRemoteAddr(), exception.getMessage(), Map.of(
                    "terminalCode", request.terminalCode(),
                    "clientType", request.clientType()
            ));
            throw exception;
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<TerminalInfoVO> updateTerminal(@PathVariable Long id, @Valid @RequestBody TerminalInfoUpdateRequest request, HttpServletRequest servletRequest) {
        try {
            TerminalInfoVO terminal = terminalInfoService.updateTerminal(id, request);
            auditLogService.recordAdminSuccess("TERMINAL_UPDATE", "TERMINAL_INFO", String.valueOf(id), servletRequest.getRemoteAddr(), auditDetail(terminal));
            return ApiResponse.success(terminal);
        } catch (RuntimeException exception) {
            auditLogService.recordAdminFailure("TERMINAL_UPDATE", "TERMINAL_INFO", String.valueOf(id), servletRequest.getRemoteAddr(), exception.getMessage(), Map.of(
                    "deviceEntityId", request.deviceEntityId() == null ? "" : request.deviceEntityId(),
                    "clientType", request.clientType() == null ? "" : request.clientType()
            ));
            throw exception;
        }
    }

    @PostMapping("/{id}/status")
    public ApiResponse<TerminalInfoVO> updateStatus(@PathVariable Long id, @Valid @RequestBody TerminalInfoStatusRequest request, HttpServletRequest servletRequest) {
        try {
            TerminalInfoVO terminal = terminalInfoService.updateStatus(id, request);
            auditLogService.recordAdminSuccess("TERMINAL_STATUS_CHANGE", "TERMINAL_INFO", String.valueOf(id), servletRequest.getRemoteAddr(), auditDetail(terminal));
            return ApiResponse.success(terminal);
        } catch (RuntimeException exception) {
            auditLogService.recordAdminFailure("TERMINAL_STATUS_CHANGE", "TERMINAL_INFO", String.valueOf(id), servletRequest.getRemoteAddr(), exception.getMessage(), Map.of(
                    "status", request.status()
            ));
            throw exception;
        }
    }

    private Map<String, Object> auditDetail(TerminalInfoVO terminal) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("terminalCode", terminal.terminalCode());
        detail.put("deviceEntityId", terminal.deviceEntityId());
        detail.put("clientType", terminal.clientType());
        detail.put("status", terminal.status());
        return detail;
    }
}
