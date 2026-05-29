package com.hmiot.hmiot_backend.module.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.common.security.CurrentUser;
import com.hmiot.hmiot_backend.module.device.dto.IotDeviceStatusQueryRequest;
import com.hmiot.hmiot_backend.module.device.dto.IotDeviceStatusReportRequest;
import com.hmiot.hmiot_backend.module.device.entity.DeviceEntity;
import com.hmiot.hmiot_backend.module.device.entity.IotDeviceStatus;
import com.hmiot.hmiot_backend.module.device.entity.TerminalInfo;
import com.hmiot.hmiot_backend.module.device.mapper.DeviceEntityMapper;
import com.hmiot.hmiot_backend.module.device.mapper.IotDeviceStatusMapper;
import com.hmiot.hmiot_backend.module.device.vo.IotDeviceHealthResponse;
import com.hmiot.hmiot_backend.module.device.vo.IotDeviceStatusCheckResponse;
import com.hmiot.hmiot_backend.module.device.vo.IotDeviceStatusVO;
import com.hmiot.hmiot_backend.module.integration.service.ExternalIntegrationCallService;
import com.hmiot.hmiot_backend.module.integration.service.IotDeviceIntegrationClient;
import com.hmiot.hmiot_backend.module.integration.service.IotDeviceIntegrationClient.HealthResult;
import com.hmiot.hmiot_backend.module.integration.service.IotDeviceIntegrationClient.StatusResult;
import com.hmiot.hmiot_backend.module.user.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class IotDeviceStatusService {
    public static final String IOT_DEVICE = "IOT_DEVICE";
    public static final String ONLINE = "ONLINE";
    public static final String OFFLINE = "OFFLINE";
    public static final String UNKNOWN = "UNKNOWN";
    public static final String TRUSTED = "TRUSTED";
    public static final String UNTRUSTED = "UNTRUSTED";
    public static final String SUSPECT = "SUSPECT";
    private static final long STATUS_VALID_SECONDS = 300;
    private static final Set<String> CONNECT_STATUSES = Set.of(ONLINE, OFFLINE, UNKNOWN);
    private static final Set<String> TRUST_STATUSES = Set.of(TRUSTED, UNTRUSTED, SUSPECT, UNKNOWN);

    private final IotDeviceStatusMapper iotDeviceStatusMapper;
    private final DeviceEntityMapper deviceEntityMapper;
    private final DeviceEntityService deviceEntityService;
    private final TerminalInfoService terminalInfoService;
    private final IotDeviceIntegrationClient iotDeviceIntegrationClient;
    private final ExternalIntegrationCallService externalIntegrationCallService;
    private final ObjectMapper objectMapper;

    public IotDeviceStatusService(IotDeviceStatusMapper iotDeviceStatusMapper, DeviceEntityMapper deviceEntityMapper, DeviceEntityService deviceEntityService, TerminalInfoService terminalInfoService, IotDeviceIntegrationClient iotDeviceIntegrationClient, ExternalIntegrationCallService externalIntegrationCallService, ObjectMapper objectMapper) {
        this.iotDeviceStatusMapper = iotDeviceStatusMapper;
        this.deviceEntityMapper = deviceEntityMapper;
        this.deviceEntityService = deviceEntityService;
        this.terminalInfoService = terminalInfoService;
        this.iotDeviceIntegrationClient = iotDeviceIntegrationClient;
        this.externalIntegrationCallService = externalIntegrationCallService;
        this.objectMapper = objectMapper;
    }

    public IotDeviceHealthResponse health() {
        LocalDateTime startedAt = LocalDateTime.now();
        try {
            HealthResult result = iotDeviceIntegrationClient.health();
            externalIntegrationCallService.recordSuccess("IOT_DEVICE_HEALTH", iotDeviceIntegrationClient.targetSystem(), IOT_DEVICE, Map.of(), Map.of("healthy", result.healthy(), "modelVersion", result.modelVersion()), startedAt);
            return new IotDeviceHealthResponse(result.healthy(), iotDeviceIntegrationClient.targetSystem(), result.modelVersion());
        } catch (RuntimeException exception) {
            externalIntegrationCallService.recordFailure("IOT_DEVICE_HEALTH", iotDeviceIntegrationClient.targetSystem(), IOT_DEVICE, Map.of(), exception.getMessage(), startedAt);
            throw exception;
        }
    }

    public PageVO<IotDeviceStatusVO> listStatuses(IotDeviceStatusQueryRequest request) {
        LambdaQueryWrapper<IotDeviceStatus> wrapper = new LambdaQueryWrapper<IotDeviceStatus>()
                .orderByDesc(IotDeviceStatus::getUpdatedAt);
        if (request.deviceEntityId() != null) {
            wrapper.eq(IotDeviceStatus::getDeviceEntityId, request.deviceEntityId());
        }
        if (StringUtils.hasText(request.connectStatus())) {
            wrapper.eq(IotDeviceStatus::getConnectStatus, request.connectStatus());
        }
        if (StringUtils.hasText(request.trustStatus())) {
            wrapper.eq(IotDeviceStatus::getTrustStatus, request.trustStatus());
        }
        Page<IotDeviceStatus> page = iotDeviceStatusMapper.selectPage(Page.of(request.pageCurrent(), request.pageSize()), wrapper);
        List<IotDeviceStatusVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageVO<>(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    public IotDeviceStatusVO getDeviceStatus(Long deviceEntityId) {
        deviceEntityService.findEnabledDevice(deviceEntityId);
        return toVO(findStatus(deviceEntityId));
    }

    @Transactional
    public IotDeviceStatusVO queryDeviceStatus(Long deviceEntityId, String requestId) {
        DeviceEntity device = deviceEntityService.findEnabledDevice(deviceEntityId);
        LocalDateTime startedAt = LocalDateTime.now();
        Map<String, Object> requestSummary = deviceSummary(device, requestId);
        try {
            StatusResult result = iotDeviceIntegrationClient.queryStatus(device, requestId);
            IotDeviceStatus status = upsertStatus(device.getId(), result.connectStatus(), result.trustStatus(), result.reportAt(), result.rawResponse());
            externalIntegrationCallService.recordSuccess("IOT_DEVICE_STATUS_QUERY", iotDeviceIntegrationClient.targetSystem(), String.valueOf(device.getId()), requestSummary, responseSummary(status), startedAt);
            return toVO(status);
        } catch (RuntimeException exception) {
            externalIntegrationCallService.recordFailure("IOT_DEVICE_STATUS_QUERY", iotDeviceIntegrationClient.targetSystem(), String.valueOf(device.getId()), requestSummary, exception.getMessage(), startedAt);
            throw exception;
        }
    }

    @Transactional
    public IotDeviceStatusVO reportStatus(IotDeviceStatusReportRequest request) {
        DeviceEntity device = resolveReportDevice(request);
        LocalDateTime startedAt = LocalDateTime.now();
        Map<String, Object> requestSummary = deviceSummary(device, request.requestId());
        try {
            StatusResult result = iotDeviceIntegrationClient.normalizeReport(request);
            IotDeviceStatus status = upsertStatus(device.getId(), result.connectStatus(), result.trustStatus(), result.reportAt(), result.rawResponse());
            externalIntegrationCallService.recordSuccess("IOT_DEVICE_STATUS_REPORT", iotDeviceIntegrationClient.targetSystem(), String.valueOf(device.getId()), requestSummary, responseSummary(status), startedAt);
            return toVO(status);
        } catch (RuntimeException exception) {
            externalIntegrationCallService.recordFailure("IOT_DEVICE_STATUS_REPORT", iotDeviceIntegrationClient.targetSystem(), String.valueOf(device.getId()), requestSummary, exception.getMessage(), startedAt);
            throw exception;
        }
    }

    public IotDeviceStatusCheckResponse checkCurrentTerminalDevice(CurrentUser currentUser) {
        return checkTerminalDevice(currentUser.terminalId());
    }

    public IotDeviceStatus ensureCurrentTerminalDeviceTrusted(CurrentUser currentUser) {
        return ensureTerminalDeviceTrusted(currentUser.terminalId());
    }

    public IotDeviceStatus ensureTerminalDeviceTrusted(Long terminalId) {
        if (terminalId == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前认证终端不存在");
        }
        TerminalInfo terminal = terminalInfoService.findEnabledTerminal(terminalId);
        Long deviceEntityId = terminal.getDeviceEntityId();
        if (deviceEntityId == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前终端未关联物联网设备");
        }
        deviceEntityService.findEnabledDevice(deviceEntityId);
        IotDeviceStatus status = findStatus(deviceEntityId);
        String failReason = trustedFailReason(status);
        if (failReason != null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, failReason);
        }
        return status;
    }

    public Map<String, Object> safeSummary(IotDeviceStatus status) {
        if (status == null) {
            return Map.of();
        }
        return Map.of(
                "iotDeviceEntityId", status.getDeviceEntityId(),
                "iotDeviceStatusAt", status.getLastReportAt() == null ? "" : status.getLastReportAt(),
                "iotDeviceConnectStatus", status.getConnectStatus(),
                "iotDeviceTrustStatus", status.getTrustStatus()
        );
    }

    public IotDeviceStatusVO toVO(IotDeviceStatus status) {
        return new IotDeviceStatusVO(status.getId(), status.getDeviceEntityId(), status.getConnectStatus(), status.getTrustStatus(), status.getLastReportAt(), status.getLastPayload(), status.getCreatedAt(), status.getUpdatedAt());
    }

    private IotDeviceStatusCheckResponse checkTerminalDevice(Long terminalId) {
        try {
            IotDeviceStatus status = ensureTerminalDeviceTrusted(terminalId);
            return new IotDeviceStatusCheckResponse(status.getDeviceEntityId(), status.getConnectStatus(), status.getTrustStatus(), true, status.getLastReportAt(), null);
        } catch (BusinessException exception) {
            Long deviceEntityId = null;
            String connectStatus = null;
            String trustStatus = null;
            LocalDateTime lastReportAt = null;
            try {
                if (terminalId != null) {
                    TerminalInfo terminal = terminalInfoService.findEnabledTerminal(terminalId);
                    deviceEntityId = terminal.getDeviceEntityId();
                    if (deviceEntityId != null) {
                        IotDeviceStatus status = statusOrNull(deviceEntityId);
                        if (status != null) {
                            connectStatus = status.getConnectStatus();
                            trustStatus = status.getTrustStatus();
                            lastReportAt = status.getLastReportAt();
                        }
                    }
                }
            } catch (RuntimeException ignored) {
                deviceEntityId = null;
            }
            return new IotDeviceStatusCheckResponse(deviceEntityId, connectStatus, trustStatus, false, lastReportAt, exception.getMessage());
        }
    }

    private IotDeviceStatus upsertStatus(Long deviceEntityId, String connectStatus, String trustStatus, LocalDateTime reportAt, Map<String, Object> payload) {
        validateStatuses(connectStatus, trustStatus);
        IotDeviceStatus status = statusOrNull(deviceEntityId);
        if (status == null) {
            status = new IotDeviceStatus();
            status.setDeviceEntityId(deviceEntityId);
        }
        status.setConnectStatus(connectStatus);
        status.setTrustStatus(trustStatus);
        status.setLastReportAt(reportAt == null ? LocalDateTime.now() : reportAt);
        status.setLastPayload(toJson(payload));
        if (status.getId() == null) {
            iotDeviceStatusMapper.insert(status);
        } else {
            iotDeviceStatusMapper.updateById(status);
        }
        return status;
    }

    private IotDeviceStatus findStatus(Long deviceEntityId) {
        IotDeviceStatus status = statusOrNull(deviceEntityId);
        if (status == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "物联网设备状态不存在");
        }
        return status;
    }

    private IotDeviceStatus statusOrNull(Long deviceEntityId) {
        return iotDeviceStatusMapper.selectOne(new LambdaQueryWrapper<IotDeviceStatus>()
                .eq(IotDeviceStatus::getDeviceEntityId, deviceEntityId)
                .last("LIMIT 1"));
    }

    private DeviceEntity resolveReportDevice(IotDeviceStatusReportRequest request) {
        DeviceEntity device = null;
        if (request.deviceEntityId() != null) {
            device = deviceEntityService.findEnabledDevice(request.deviceEntityId());
        } else if (StringUtils.hasText(request.deviceFingerprint())) {
            device = deviceEntityService.findEnabledByFingerprint(request.deviceFingerprint());
        } else if (StringUtils.hasText(request.deviceCode())) {
            device = deviceEntityMapper.selectOne(new LambdaQueryWrapper<DeviceEntity>()
                    .eq(DeviceEntity::getDeviceCode, request.deviceCode())
                    .eq(DeviceEntity::getDeleted, false)
                    .last("LIMIT 1"));
            if (device != null && !DeviceEntityService.ENABLED.equals(device.getStatus())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "设备实体不可用");
            }
        }
        if (device == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "设备实体不存在");
        }
        return device;
    }

    private void validateStatuses(String connectStatus, String trustStatus) {
        if (!CONNECT_STATUSES.contains(connectStatus)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "设备连接状态无效");
        }
        if (!TRUST_STATUSES.contains(trustStatus)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "设备信任状态无效");
        }
    }

    private String trustedFailReason(IotDeviceStatus status) {
        if (!ONLINE.equals(status.getConnectStatus())) {
            return "物联网设备不在线";
        }
        if (!TRUSTED.equals(status.getTrustStatus())) {
            return "物联网设备不可信";
        }
        if (status.getLastReportAt() == null || status.getLastReportAt().isBefore(LocalDateTime.now().minusSeconds(STATUS_VALID_SECONDS))) {
            return "物联网设备状态已过期";
        }
        return null;
    }

    private Map<String, Object> deviceSummary(DeviceEntity device, String requestId) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("deviceEntityId", device.getId());
        summary.put("deviceCode", device.getDeviceCode());
        summary.put("deviceType", device.getDeviceType());
        summary.put("requestId", requestId == null ? "" : requestId);
        return summary;
    }

    private Map<String, Object> responseSummary(IotDeviceStatus status) {
        return Map.of(
                "deviceEntityId", status.getDeviceEntityId(),
                "connectStatus", status.getConnectStatus(),
                "trustStatus", status.getTrustStatus(),
                "lastReportAt", status.getLastReportAt() == null ? "" : status.getLastReportAt()
        );
    }

    private String toJson(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "物联网设备状态报文序列化失败");
        }
    }
}
