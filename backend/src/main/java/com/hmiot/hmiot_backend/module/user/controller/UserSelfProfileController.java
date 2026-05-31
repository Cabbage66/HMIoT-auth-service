package com.hmiot.hmiot_backend.module.user.controller;

import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.common.response.ApiResponse;
import com.hmiot.hmiot_backend.common.security.CurrentUser;
import com.hmiot.hmiot_backend.module.audit.service.AuditLogService;
import com.hmiot.hmiot_backend.module.user.dto.UserSelfUpdateRequest;
import com.hmiot.hmiot_backend.module.user.service.UserService;
import com.hmiot.hmiot_backend.module.user.vo.UserSelfProfileVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/profile")
public class UserSelfProfileController {
    private final UserService userService;
    private final AuditLogService auditLogService;

    public UserSelfProfileController(UserService userService, AuditLogService auditLogService) {
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/me")
    public ApiResponse<UserSelfProfileVO> me() {
        CurrentUser currentUser = currentUser();
        return ApiResponse.success(userService.getSelfProfile(currentUser.userId()));
    }

    @PutMapping("/me")
    public ApiResponse<UserSelfProfileVO> updateMe(@Valid @RequestBody UserSelfUpdateRequest request, HttpServletRequest servletRequest) {
        CurrentUser currentUser = currentUser();
        try {
            UserSelfProfileVO profile = userService.updateSelfProfile(currentUser.userId(), request);
            auditLogService.recordUserOperationSuccess(currentUser.userId(), currentUser.username(), "USER_SELF_PROFILE_UPDATE", "USER_ACCOUNT", String.valueOf(currentUser.userId()), servletRequest.getRemoteAddr(), Map.of(
                    "realName", profile.realName(),
                    "phone", profile.phone() == null ? "" : profile.phone(),
                    "email", profile.email() == null ? "" : profile.email()
            ));
            return ApiResponse.success(profile);
        } catch (RuntimeException exception) {
            auditLogService.recordUserOperationFailure(currentUser.userId(), currentUser.username(), "USER_SELF_PROFILE_UPDATE", "USER_ACCOUNT", String.valueOf(currentUser.userId()), servletRequest.getRemoteAddr(), exception.getMessage(), Map.of(
                    "userId", currentUser.userId()
            ));
            throw exception;
        }
    }

    private CurrentUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser currentUser)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前用户未登录");
        }
        return currentUser;
    }
}
