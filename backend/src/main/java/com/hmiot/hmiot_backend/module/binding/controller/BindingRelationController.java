package com.hmiot.hmiot_backend.module.binding.controller;

import com.hmiot.hmiot_backend.common.response.ApiResponse;
import com.hmiot.hmiot_backend.common.security.CurrentAdmin;
import com.hmiot.hmiot_backend.module.audit.service.AuditLogService;
import com.hmiot.hmiot_backend.module.binding.dto.BindingRelationQueryRequest;
import com.hmiot.hmiot_backend.module.binding.dto.BindingUnbindRequest;
import com.hmiot.hmiot_backend.module.binding.dto.UserDynamicBiometricBindRequest;
import com.hmiot.hmiot_backend.module.binding.dto.UserTerminalBindRequest;
import com.hmiot.hmiot_backend.module.binding.service.BindingRelationService;
import com.hmiot.hmiot_backend.module.binding.vo.BindingRelationVO;
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
@RequestMapping("/api/v1")
public class BindingRelationController {
    private final BindingRelationService bindingRelationService;
    private final AuditLogService auditLogService;

    public BindingRelationController(BindingRelationService bindingRelationService, AuditLogService auditLogService) {
        this.bindingRelationService = bindingRelationService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/bindings")
    public ApiResponse<PageVO<BindingRelationVO>> listBindings(BindingRelationQueryRequest request) {
        return ApiResponse.success(bindingRelationService.listBindings(request));
    }

    @PostMapping("/bindings/user-terminal")
    public ApiResponse<BindingRelationVO> bindUserTerminal(@Valid @RequestBody UserTerminalBindRequest request, HttpServletRequest servletRequest) {
        try {
            BindingRelationVO binding = bindingRelationService.bindUserTerminal(request, currentAdminId());
            auditLogService.recordAdminSuccess("USER_TERMINAL_BIND", "BINDING_RELATION", String.valueOf(binding.id()), servletRequest.getRemoteAddr(), auditDetail(binding));
            return ApiResponse.success(binding);
        } catch (RuntimeException exception) {
            auditLogService.recordAdminFailure("USER_TERMINAL_BIND", "BINDING_RELATION", null, servletRequest.getRemoteAddr(), exception.getMessage(), Map.of(
                    "userId", request.userId(),
                    "terminalId", request.terminalId()
            ));
            throw exception;
        }
    }

    @PostMapping("/bindings/user-dynamic-biometric")
    public ApiResponse<BindingRelationVO> bindUserDynamicBiometric(@Valid @RequestBody UserDynamicBiometricBindRequest request, HttpServletRequest servletRequest) {
        try {
            BindingRelationVO binding = bindingRelationService.bindUserDynamicBiometric(request, currentAdminId());
            auditLogService.recordAdminSuccess("USER_DYNAMIC_BIOMETRIC_BIND", "BINDING_RELATION", String.valueOf(binding.id()), servletRequest.getRemoteAddr(), auditDetail(binding));
            return ApiResponse.success(binding);
        } catch (RuntimeException exception) {
            auditLogService.recordAdminFailure("USER_DYNAMIC_BIOMETRIC_BIND", "BINDING_RELATION", null, servletRequest.getRemoteAddr(), exception.getMessage(), Map.of(
                    "userId", request.userId(),
                    "externalSubjectId", request.externalSubjectId()
            ));
            throw exception;
        }
    }

    @PostMapping("/bindings/{id}/unbind")
    public ApiResponse<BindingRelationVO> unbind(@PathVariable Long id, @RequestBody(required = false) BindingUnbindRequest request, HttpServletRequest servletRequest) {
        BindingUnbindRequest unbindRequest = request == null ? new BindingUnbindRequest(null) : request;
        try {
            BindingRelationVO binding = bindingRelationService.unbind(id, unbindRequest);
            String operation = unbindOperation(binding);
            auditLogService.recordAdminSuccess(operation, "BINDING_RELATION", String.valueOf(id), servletRequest.getRemoteAddr(), auditDetail(binding));
            return ApiResponse.success(binding);
        } catch (RuntimeException exception) {
            auditLogService.recordAdminFailure("BINDING_UNBIND", "BINDING_RELATION", String.valueOf(id), servletRequest.getRemoteAddr(), exception.getMessage(), Map.of(
                    "reason", unbindRequest.reason() == null ? "" : unbindRequest.reason()
            ));
            throw exception;
        }
    }

    @GetMapping("/users/{userId}/terminals")
    public ApiResponse<List<BindingRelationVO>> listUserTerminals(@PathVariable Long userId) {
        return ApiResponse.success(bindingRelationService.listUserTerminals(userId));
    }

    @GetMapping("/terminals/{terminalId}/users")
    public ApiResponse<List<BindingRelationVO>> listTerminalUsers(@PathVariable Long terminalId) {
        return ApiResponse.success(bindingRelationService.listTerminalUsers(terminalId));
    }

    @GetMapping("/users/{userId}/dynamic-biometrics")
    public ApiResponse<List<BindingRelationVO>> listUserDynamicBiometrics(@PathVariable Long userId) {
        return ApiResponse.success(bindingRelationService.listUserDynamicBiometrics(userId));
    }

    @GetMapping("/dynamic-biometrics/{externalSubjectId}/users")
    public ApiResponse<List<BindingRelationVO>> listDynamicBiometricUsers(@PathVariable String externalSubjectId) {
        return ApiResponse.success(bindingRelationService.listDynamicBiometricUsers(externalSubjectId));
    }

    private Long currentAdminId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentAdmin currentAdmin)) {
            return null;
        }
        return currentAdmin.adminId();
    }

    private String unbindOperation(BindingRelationVO binding) {
        if (BindingRelationService.TARGET_DYNAMIC_BIOMETRIC.equals(binding.targetType())) {
            return "USER_DYNAMIC_BIOMETRIC_UNBIND";
        }
        return "USER_TERMINAL_UNBIND";
    }

    private Map<String, Object> auditDetail(BindingRelationVO binding) {
        return Map.of(
                "subjectType", binding.subjectType(),
                "subjectId", binding.subjectId(),
                "targetType", binding.targetType(),
                "targetId", binding.targetId(),
                "relationType", binding.relationType(),
                "status", binding.status()
        );
    }
}
