package com.hmiot.hmiot_backend.module.device.controller;

import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.common.response.ApiResponse;
import com.hmiot.hmiot_backend.common.security.CurrentUser;
import com.hmiot.hmiot_backend.module.audit.service.AuditLogService;
import com.hmiot.hmiot_backend.module.device.dto.IotDeviceStatusCheckRequest;
import com.hmiot.hmiot_backend.module.device.dto.IotDeviceStatusQueryRequest;
import com.hmiot.hmiot_backend.module.device.dto.IotDeviceStatusReportRequest;
import com.hmiot.hmiot_backend.module.device.service.IotDeviceStatusService;
import com.hmiot.hmiot_backend.module.device.vo.IotDeviceHealthResponse;
import com.hmiot.hmiot_backend.module.device.vo.IotDeviceStatusCheckResponse;
import com.hmiot.hmiot_backend.module.device.vo.IotDeviceStatusVO;
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

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class IotDeviceStatusController {
    private final IotDeviceStatusService iotDeviceStatusService;
    private final AuditLogService auditLogService;

    public IotDeviceStatusController(IotDeviceStatusService iotDeviceStatusService, AuditLogService auditLogService) {
        this.iotDeviceStatusService = iotDeviceStatusService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/iot-device-statuses/health")
    public ApiResponse<IotDeviceHealthResponse> health() {
        return ApiResponse.success(iotDeviceStatusService.health());
    }

    @GetMapping("/iot-device-statuses")
    public ApiResponse<PageVO<IotDeviceStatusVO>> listStatuses(IotDeviceStatusQueryRequest request) {
        return ApiResponse.success(iotDeviceStatusService.listStatuses(request));
    }

    @GetMapping("/devices/{deviceEntityId}/iot-status")
    public ApiResponse<IotDeviceStatusVO> getDeviceStatus(@PathVariable Long deviceEntityId) {
        return ApiResponse.success(iotDeviceStatusService.getDeviceStatus(deviceEntityId));
    }

    @PostMapping("/devices/{deviceEntityId}/iot-status/query")
    public ApiResponse<IotDeviceStatusVO> queryDeviceStatus(@PathVariable Long deviceEntityId, @RequestBody(required = false) IotDeviceStatusCheckRequest request, HttpServletRequest servletRequest) {
        String requestId = request == null ? null : request.requestId();
        try {
            IotDeviceStatusVO status = iotDeviceStatusService.queryDeviceStatus(deviceEntityId, requestId);
            auditLogService.recordAdminSuccess("IOT_DEVICE_STATUS_QUERY", "IOT_DEVICE_STATUS", String.valueOf(deviceEntityId), servletRequest.getRemoteAddr(), statusAuditDetail(status));
            return ApiResponse.success(status);
        } catch (RuntimeException exception) {
            auditLogService.recordAdminFailure("IOT_DEVICE_STATUS_QUERY", "IOT_DEVICE_STATUS", String.valueOf(deviceEntityId), servletRequest.getRemoteAddr(), exception.getMessage(), Map.of(
                    "deviceEntityId", deviceEntityId,
                    "requestId", requestId == null ? "" : requestId
            ));
            throw exception;
        }
    }

    @PostMapping("/iot-device-statuses/reports")
    public ApiResponse<IotDeviceStatusVO> reportStatus(@Valid @RequestBody IotDeviceStatusReportRequest request, HttpServletRequest servletRequest) {
        try {
            IotDeviceStatusVO status = iotDeviceStatusService.reportStatus(request);
            auditLogService.recordSystemOperation("IOT_DEVICE_STATUS_REPORT", "IOT_DEVICE_STATUS", String.valueOf(status.deviceEntityId()), servletRequest.getRemoteAddr(), null, statusAuditDetail(status));
            return ApiResponse.success(status);
        } catch (RuntimeException exception) {
            auditLogService.recordSystemOperation("IOT_DEVICE_STATUS_REPORT", "IOT_DEVICE_STATUS", request.deviceEntityId() == null ? null : String.valueOf(request.deviceEntityId()), servletRequest.getRemoteAddr(), exception.getMessage(), Map.of(
                    "deviceEntityId", request.deviceEntityId() == null ? "" : request.deviceEntityId(),
                    "deviceCode", request.deviceCode() == null ? "" : request.deviceCode(),
                    "requestId", request.requestId() == null ? "" : request.requestId()
            ));
            throw exception;
        }
    }

    @PostMapping("/iot-device-statuses/check")
    public ApiResponse<IotDeviceStatusCheckResponse> check(@RequestBody(required = false) IotDeviceStatusCheckRequest request, HttpServletRequest servletRequest) {
        CurrentUser currentUser = currentUser();
        IotDeviceStatusCheckResponse response = iotDeviceStatusService.checkCurrentTerminalDevice(currentUser);
        if (response.trusted()) {
            auditLogService.recordUserOperationSuccess(currentUser.userId(), currentUser.username(), "IOT_DEVICE_STATUS_CHECK", "IOT_DEVICE_STATUS", response.deviceEntityId() == null ? null : String.valueOf(response.deviceEntityId()), servletRequest.getRemoteAddr(), checkAuditDetail(response, request));
        } else {
            auditLogService.recordUserOperationFailure(currentUser.userId(), currentUser.username(), "IOT_DEVICE_STATUS_CHECK", "IOT_DEVICE_STATUS", response.deviceEntityId() == null ? null : String.valueOf(response.deviceEntityId()), servletRequest.getRemoteAddr(), response.failReason(), checkAuditDetail(response, request));
        }
        return ApiResponse.success(response);
    }

    private CurrentUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser currentUser)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前用户未登录");
        }
        return currentUser;
    }

    private Map<String, Object> statusAuditDetail(IotDeviceStatusVO status) {
        return Map.of(
                "deviceEntityId", status.deviceEntityId(),
                "connectStatus", status.connectStatus(),
                "trustStatus", status.trustStatus(),
                "lastReportAt", status.lastReportAt() == null ? "" : status.lastReportAt()
        );
    }

    private Map<String, Object> checkAuditDetail(IotDeviceStatusCheckResponse response, IotDeviceStatusCheckRequest request) {
        return Map.of(
                "deviceEntityId", response.deviceEntityId() == null ? "" : response.deviceEntityId(),
                "connectStatus", response.connectStatus() == null ? "" : response.connectStatus(),
                "trustStatus", response.trustStatus() == null ? "" : response.trustStatus(),
                "trusted", response.trusted(),
                "lastReportAt", response.lastReportAt() == null ? "" : response.lastReportAt(),
                "requestId", request == null || request.requestId() == null ? "" : request.requestId(),
                "failReason", response.failReason() == null ? "" : response.failReason()
        );
    }
}
