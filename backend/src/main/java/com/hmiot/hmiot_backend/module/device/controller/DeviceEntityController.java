package com.hmiot.hmiot_backend.module.device.controller;

import com.hmiot.hmiot_backend.common.response.ApiResponse;
import com.hmiot.hmiot_backend.module.audit.service.AuditLogService;
import com.hmiot.hmiot_backend.module.device.dto.DeviceEntityCreateRequest;
import com.hmiot.hmiot_backend.module.device.dto.DeviceEntityQueryRequest;
import com.hmiot.hmiot_backend.module.device.dto.DeviceEntityStatusRequest;
import com.hmiot.hmiot_backend.module.device.dto.DeviceEntityUpdateRequest;
import com.hmiot.hmiot_backend.module.device.service.DeviceEntityService;
import com.hmiot.hmiot_backend.module.device.vo.DeviceEntityVO;
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
@RequestMapping("/api/v1/devices")
public class DeviceEntityController {
    private final DeviceEntityService deviceEntityService;
    private final AuditLogService auditLogService;

    public DeviceEntityController(DeviceEntityService deviceEntityService, AuditLogService auditLogService) {
        this.deviceEntityService = deviceEntityService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<PageVO<DeviceEntityVO>> listDevices(DeviceEntityQueryRequest request) {
        return ApiResponse.success(deviceEntityService.listDevices(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<DeviceEntityVO> getDevice(@PathVariable Long id) {
        return ApiResponse.success(deviceEntityService.getDevice(id));
    }

    @PostMapping
    public ApiResponse<DeviceEntityVO> createDevice(@Valid @RequestBody DeviceEntityCreateRequest request, HttpServletRequest servletRequest) {
        try {
            DeviceEntityVO device = deviceEntityService.createDevice(request);
            auditLogService.recordAdminSuccess("DEVICE_CREATE", "DEVICE_ENTITY", String.valueOf(device.id()), servletRequest.getRemoteAddr(), auditDetail(device));
            return ApiResponse.success(device);
        } catch (RuntimeException exception) {
            auditLogService.recordAdminFailure("DEVICE_CREATE", "DEVICE_ENTITY", null, servletRequest.getRemoteAddr(), exception.getMessage(), Map.of(
                    "deviceCode", request.deviceCode(),
                    "deviceName", request.deviceName(),
                    "deviceType", request.deviceType()
            ));
            throw exception;
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<DeviceEntityVO> updateDevice(@PathVariable Long id, @Valid @RequestBody DeviceEntityUpdateRequest request, HttpServletRequest servletRequest) {
        try {
            DeviceEntityVO device = deviceEntityService.updateDevice(id, request);
            auditLogService.recordAdminSuccess("DEVICE_UPDATE", "DEVICE_ENTITY", String.valueOf(id), servletRequest.getRemoteAddr(), auditDetail(device));
            return ApiResponse.success(device);
        } catch (RuntimeException exception) {
            auditLogService.recordAdminFailure("DEVICE_UPDATE", "DEVICE_ENTITY", String.valueOf(id), servletRequest.getRemoteAddr(), exception.getMessage(), Map.of(
                    "deviceName", request.deviceName(),
                    "deviceType", request.deviceType()
            ));
            throw exception;
        }
    }

    @PostMapping("/{id}/status")
    public ApiResponse<DeviceEntityVO> updateStatus(@PathVariable Long id, @Valid @RequestBody DeviceEntityStatusRequest request, HttpServletRequest servletRequest) {
        try {
            DeviceEntityVO device = deviceEntityService.updateStatus(id, request);
            auditLogService.recordAdminSuccess("DEVICE_STATUS_CHANGE", "DEVICE_ENTITY", String.valueOf(id), servletRequest.getRemoteAddr(), auditDetail(device));
            return ApiResponse.success(device);
        } catch (RuntimeException exception) {
            auditLogService.recordAdminFailure("DEVICE_STATUS_CHANGE", "DEVICE_ENTITY", String.valueOf(id), servletRequest.getRemoteAddr(), exception.getMessage(), Map.of(
                    "status", request.status()
            ));
            throw exception;
        }
    }

    private Map<String, Object> auditDetail(DeviceEntityVO device) {
        return Map.of(
                "deviceCode", device.deviceCode(),
                "deviceName", device.deviceName(),
                "deviceType", device.deviceType(),
                "status", device.status()
        );
    }
}
