package com.hmiot.hmiot_backend.module.app.controller;

import com.hmiot.hmiot_backend.common.response.ApiResponse;
import com.hmiot.hmiot_backend.module.app.dto.AppResourceCreateRequest;
import com.hmiot.hmiot_backend.module.app.dto.AppResourceQueryRequest;
import com.hmiot.hmiot_backend.module.app.dto.AppResourceStatusRequest;
import com.hmiot.hmiot_backend.module.app.dto.AppResourceUpdateRequest;
import com.hmiot.hmiot_backend.module.app.service.AppResourceService;
import com.hmiot.hmiot_backend.module.app.vo.AppResourceVO;
import com.hmiot.hmiot_backend.module.audit.service.AuditLogService;
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

import java.util.Map;

@RestController
@RequestMapping("/api/v1/apps")
public class AppResourceController {
    private final AppResourceService appResourceService;
    private final AuditLogService auditLogService;

    public AppResourceController(AppResourceService appResourceService, AuditLogService auditLogService) {
        this.appResourceService = appResourceService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<PageVO<AppResourceVO>> listApps(AppResourceQueryRequest request) {
        return ApiResponse.success(appResourceService.listApps(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<AppResourceVO> getApp(@PathVariable Long id) {
        return ApiResponse.success(appResourceService.getApp(id));
    }

    @PostMapping
    public ApiResponse<AppResourceVO> createApp(@Valid @RequestBody AppResourceCreateRequest request, HttpServletRequest servletRequest) {
        try {
            AppResourceVO app = appResourceService.createApp(request);
            auditLogService.recordAdminSuccess("APP_RESOURCE_CREATE", "APP_RESOURCE", String.valueOf(app.id()), servletRequest.getRemoteAddr(), auditDetail(app));
            return ApiResponse.success(app);
        } catch (RuntimeException exception) {
            auditLogService.recordAdminFailure("APP_RESOURCE_CREATE", "APP_RESOURCE", null, servletRequest.getRemoteAddr(), exception.getMessage(), Map.of(
                    "appCode", request.appCode(),
                    "appName", request.appName()
            ));
            throw exception;
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<AppResourceVO> updateApp(@PathVariable Long id, @Valid @RequestBody AppResourceUpdateRequest request, HttpServletRequest servletRequest) {
        try {
            AppResourceVO app = appResourceService.updateApp(id, request);
            auditLogService.recordAdminSuccess("APP_RESOURCE_UPDATE", "APP_RESOURCE", String.valueOf(id), servletRequest.getRemoteAddr(), auditDetail(app));
            return ApiResponse.success(app);
        } catch (RuntimeException exception) {
            auditLogService.recordAdminFailure("APP_RESOURCE_UPDATE", "APP_RESOURCE", String.valueOf(id), servletRequest.getRemoteAddr(), exception.getMessage(), Map.of(
                    "appName", request.appName()
            ));
            throw exception;
        }
    }

    @PostMapping("/{id}/status")
    public ApiResponse<AppResourceVO> updateStatus(@PathVariable Long id, @Valid @RequestBody AppResourceStatusRequest request, HttpServletRequest servletRequest) {
        try {
            AppResourceVO app = appResourceService.updateStatus(id, request);
            auditLogService.recordAdminSuccess("APP_RESOURCE_STATUS_CHANGE", "APP_RESOURCE", String.valueOf(id), servletRequest.getRemoteAddr(), auditDetail(app));
            return ApiResponse.success(app);
        } catch (RuntimeException exception) {
            auditLogService.recordAdminFailure("APP_RESOURCE_STATUS_CHANGE", "APP_RESOURCE", String.valueOf(id), servletRequest.getRemoteAddr(), exception.getMessage(), Map.of(
                    "status", request.status()
            ));
            throw exception;
        }
    }

    private Map<String, Object> auditDetail(AppResourceVO app) {
        return Map.of(
                "appCode", app.appCode(),
                "appName", app.appName(),
                "status", app.status(),
                "requiredTrustLevel", app.requiredTrustLevel()
        );
    }
}
