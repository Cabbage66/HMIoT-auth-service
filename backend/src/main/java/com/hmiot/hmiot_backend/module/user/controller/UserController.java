package com.hmiot.hmiot_backend.module.user.controller;

import com.hmiot.hmiot_backend.common.response.ApiResponse;
import com.hmiot.hmiot_backend.module.audit.service.AuditLogService;
import com.hmiot.hmiot_backend.module.user.dto.UserQueryRequest;
import com.hmiot.hmiot_backend.module.user.dto.UserUpdateRequest;
import com.hmiot.hmiot_backend.module.user.service.UserService;
import com.hmiot.hmiot_backend.module.user.vo.PageVO;
import com.hmiot.hmiot_backend.module.user.vo.UserAttributeSyncResponse;
import com.hmiot.hmiot_backend.module.user.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;
    private final AuditLogService auditLogService;

    public UserController(UserService userService, AuditLogService auditLogService) {
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<PageVO<UserVO>> listUsers(UserQueryRequest request) {
        return ApiResponse.success(userService.listUsers(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserVO> getUser(@PathVariable Long id) {
        return ApiResponse.success(userService.getUser(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserVO> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request, HttpServletRequest servletRequest) {
        UserVO user = userService.updateUser(id, request);
        auditLogService.recordAdminSuccess("USER_UPDATE", "USER_ACCOUNT", String.valueOf(id), servletRequest.getRemoteAddr(), Map.of(
                "username", user.username(),
                "status", user.status()
        ));
        return ApiResponse.success(user);
    }

    @PostMapping("/{id}/attributes/sync")
    public ApiResponse<UserAttributeSyncResponse> syncAttributes(@PathVariable Long id, HttpServletRequest servletRequest) {
        try {
            UserAttributeSyncResponse response = userService.syncAttributes(id);
            auditLogService.recordAdminSuccess("USER_ATTRIBUTE_SYNC", "USER_ACCOUNT", String.valueOf(id), servletRequest.getRemoteAddr(), Map.of(
                    "username", response.user().username(),
                    "externalSource", response.afterProfile() == null ? "" : response.afterProfile().externalSource(),
                    "oldAttributeVersion", response.beforeProfile() == null ? "" : response.beforeProfile().attributeVersion(),
                    "newAttributeVersion", response.afterProfile() == null ? "" : response.afterProfile().attributeVersion(),
                    "externalCallId", response.externalCall() == null ? "" : response.externalCall().callId()
            ));
            return ApiResponse.success(response);
        } catch (RuntimeException exception) {
            auditLogService.recordAdminFailure("USER_ATTRIBUTE_SYNC", "USER_ACCOUNT", String.valueOf(id), servletRequest.getRemoteAddr(), exception.getMessage(), Map.of(
                    "userId", id
            ));
            throw exception;
        }
    }
}
