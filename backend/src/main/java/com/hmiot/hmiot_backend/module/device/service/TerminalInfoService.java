package com.hmiot.hmiot_backend.module.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.module.device.dto.TerminalInfoCreateRequest;
import com.hmiot.hmiot_backend.module.device.dto.TerminalInfoQueryRequest;
import com.hmiot.hmiot_backend.module.device.dto.TerminalInfoStatusRequest;
import com.hmiot.hmiot_backend.module.device.dto.TerminalInfoUpdateRequest;
import com.hmiot.hmiot_backend.module.device.dto.TerminalRegisterRequest;
import com.hmiot.hmiot_backend.module.device.entity.DeviceEntity;
import com.hmiot.hmiot_backend.module.device.entity.TerminalInfo;
import com.hmiot.hmiot_backend.module.device.mapper.TerminalInfoMapper;
import com.hmiot.hmiot_backend.module.device.vo.TerminalInfoVO;
import com.hmiot.hmiot_backend.module.user.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TerminalInfoService {
    public static final String ENABLED = "ENABLED";

    private final TerminalInfoMapper terminalInfoMapper;
    private final DeviceEntityService deviceEntityService;
    private final ObjectMapper objectMapper;

    public TerminalInfoService(TerminalInfoMapper terminalInfoMapper, DeviceEntityService deviceEntityService, ObjectMapper objectMapper) {
        this.terminalInfoMapper = terminalInfoMapper;
        this.deviceEntityService = deviceEntityService;
        this.objectMapper = objectMapper;
    }

    public PageVO<TerminalInfoVO> listTerminals(TerminalInfoQueryRequest request) {
        LambdaQueryWrapper<TerminalInfo> wrapper = new LambdaQueryWrapper<TerminalInfo>().orderByDesc(TerminalInfo::getCreatedAt);
        if (StringUtils.hasText(request.keyword())) {
            wrapper.like(TerminalInfo::getTerminalCode, request.keyword());
        }
        if (StringUtils.hasText(request.clientType())) {
            wrapper.eq(TerminalInfo::getClientType, request.clientType());
        }
        if (StringUtils.hasText(request.status())) {
            wrapper.eq(TerminalInfo::getStatus, request.status());
        }
        if (request.deviceEntityId() != null) {
            wrapper.eq(TerminalInfo::getDeviceEntityId, request.deviceEntityId());
        }
        Page<TerminalInfo> page = terminalInfoMapper.selectPage(Page.of(request.pageCurrent(), request.pageSize()), wrapper);
        List<TerminalInfoVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageVO<>(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    public TerminalInfoVO getTerminal(Long id) {
        return toVO(findTerminal(id));
    }

    @Transactional
    public TerminalInfoVO createTerminal(TerminalInfoCreateRequest request) {
        ensureTerminalCodeUnique(request.terminalCode(), null);
        Long deviceEntityId = validatedDeviceId(request.deviceEntityId());
        TerminalInfo terminal = new TerminalInfo();
        terminal.setTerminalCode(request.terminalCode());
        terminal.setDeviceEntityId(deviceEntityId);
        terminal.setUserAgent(request.userAgent());
        terminal.setOsName(request.osName());
        terminal.setOsVersion(request.osVersion());
        terminal.setClientType(request.clientType());
        terminal.setClientVersion(request.clientVersion());
        terminal.setIpAddress(request.ipAddress());
        terminal.setStatus(StringUtils.hasText(request.status()) ? request.status() : ENABLED);
        terminal.setMetadata(request.metadata());
        terminal.setLastSeenAt(LocalDateTime.now());
        terminalInfoMapper.insert(terminal);
        return toVO(terminal);
    }

    @Transactional
    public TerminalInfoVO updateTerminal(Long id, TerminalInfoUpdateRequest request) {
        TerminalInfo terminal = findTerminal(id);
        terminal.setDeviceEntityId(validatedDeviceId(request.deviceEntityId()));
        terminal.setUserAgent(request.userAgent());
        terminal.setOsName(request.osName());
        terminal.setOsVersion(request.osVersion());
        terminal.setClientType(request.clientType());
        terminal.setClientVersion(request.clientVersion());
        terminal.setIpAddress(request.ipAddress());
        if (StringUtils.hasText(request.status())) {
            terminal.setStatus(request.status());
        }
        terminal.setMetadata(request.metadata());
        terminalInfoMapper.updateById(terminal);
        return toVO(terminal);
    }

    @Transactional
    public TerminalInfoVO updateStatus(Long id, TerminalInfoStatusRequest request) {
        TerminalInfo terminal = findTerminal(id);
        terminal.setStatus(request.status());
        terminalInfoMapper.updateById(terminal);
        return toVO(terminal);
    }

    @Transactional
    public TerminalInfo resolveOrRegister(TerminalRegisterRequest request, String requestIp) {
        String terminalCode = StringUtils.hasText(request.terminalCode()) ? request.terminalCode() : generateTerminalCode(request);
        TerminalInfo terminal = terminalInfoMapper.selectOne(new LambdaQueryWrapper<TerminalInfo>().eq(TerminalInfo::getTerminalCode, terminalCode));
        Long deviceEntityId = null;
        DeviceEntity device = deviceEntityService.findEnabledByFingerprint(request.deviceFingerprint());
        if (device != null) {
            deviceEntityId = device.getId();
        }
        if (terminal == null) {
            terminal = new TerminalInfo();
            terminal.setTerminalCode(terminalCode);
            terminal.setStatus(ENABLED);
            terminal.setClientType(StringUtils.hasText(request.clientType()) ? request.clientType() : "UNKNOWN");
        } else if (!ENABLED.equals(terminal.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "终端不可用");
        } else if (terminal.getDeviceEntityId() != null) {
            deviceEntityService.findEnabledDevice(terminal.getDeviceEntityId());
        }
        validateDeviceFingerprint(terminal, request.deviceFingerprint());
        if (deviceEntityId != null) {
            terminal.setDeviceEntityId(deviceEntityId);
        }
        if (StringUtils.hasText(request.userAgent())) {
            terminal.setUserAgent(request.userAgent());
        }
        if (StringUtils.hasText(request.osName())) {
            terminal.setOsName(request.osName());
        }
        if (StringUtils.hasText(request.osVersion())) {
            terminal.setOsVersion(request.osVersion());
        }
        if (StringUtils.hasText(request.clientType())) {
            terminal.setClientType(request.clientType());
        }
        if (StringUtils.hasText(request.clientVersion())) {
            terminal.setClientVersion(request.clientVersion());
        }
        terminal.setIpAddress(requestIp);
        terminal.setLastSeenAt(LocalDateTime.now());
        terminal.setMetadata(mergedMetadata(terminal.getMetadata(), request.metadata(), request.deviceFingerprint()));
        if (terminal.getId() == null) {
            terminalInfoMapper.insert(terminal);
        } else {
            terminalInfoMapper.updateById(terminal);
        }
        return terminal;
    }

    public TerminalInfo findTerminal(Long id) {
        TerminalInfo terminal = terminalInfoMapper.selectById(id);
        if (terminal == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "终端不存在");
        }
        return terminal;
    }

    public TerminalInfo findEnabledTerminal(Long id) {
        TerminalInfo terminal = findTerminal(id);
        ensureEnabled(terminal);
        return terminal;
    }

    public void ensureEnabled(TerminalInfo terminal) {
        if (!ENABLED.equals(terminal.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "终端不可用");
        }
        if (terminal.getDeviceEntityId() != null) {
            deviceEntityService.findEnabledDevice(terminal.getDeviceEntityId());
        }
    }

    public TerminalInfoVO toVO(TerminalInfo terminal) {
        if (terminal == null) {
            return null;
        }
        DeviceEntity device = terminal.getDeviceEntityId() == null ? null : deviceEntityService.findDevice(terminal.getDeviceEntityId());
        return new TerminalInfoVO(
                terminal.getId(),
                terminal.getTerminalCode(),
                terminal.getDeviceEntityId(),
                terminal.getUserAgent(),
                terminal.getOsName(),
                terminal.getOsVersion(),
                terminal.getClientType(),
                terminal.getClientVersion(),
                terminal.getIpAddress(),
                terminal.getStatus(),
                terminal.getLastSeenAt(),
                terminal.getMetadata(),
                terminal.getCreatedAt(),
                terminal.getUpdatedAt(),
                deviceEntityService.toVO(device)
        );
    }

    private Long validatedDeviceId(Long deviceEntityId) {
        if (deviceEntityId == null) {
            return null;
        }
        return deviceEntityService.findDevice(deviceEntityId).getId();
    }

    private void ensureTerminalCodeUnique(String terminalCode, Long excludeId) {
        TerminalInfo existing = terminalInfoMapper.selectOne(new LambdaQueryWrapper<TerminalInfo>().eq(TerminalInfo::getTerminalCode, terminalCode));
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "终端编码已存在");
        }
    }

    private String generateTerminalCode(TerminalRegisterRequest request) {
        if (StringUtils.hasText(request.deviceFingerprint())) {
            return "TERM-" + Math.abs(request.deviceFingerprint().hashCode());
        }
        return "TERM-" + UUID.randomUUID();
    }

    private void validateDeviceFingerprint(TerminalInfo terminal, String incomingFingerprint) {
        if (terminal == null || terminal.getId() == null || !StringUtils.hasText(incomingFingerprint)) {
            return;
        }
        String existing = stringValue(parseMetadata(terminal.getMetadata()).get("deviceFingerprint"));
        if (StringUtils.hasText(existing) && !existing.equals(incomingFingerprint.trim())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "终端设备指纹不匹配，禁止复用该终端编号");
        }
    }

    private String mergedMetadata(String existingJson, String requestJson, String deviceFingerprint) {
        Map<String, Object> merged = new LinkedHashMap<>(parseMetadata(existingJson));
        merged.putAll(parseMetadata(requestJson));
        if (StringUtils.hasText(deviceFingerprint)) {
            merged.put("deviceFingerprint", deviceFingerprint.trim());
        }
        if (merged.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(merged);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "终端元数据序列化失败");
        }
    }

    private Map<String, Object> parseMetadata(String metadata) {
        if (!StringUtils.hasText(metadata)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(metadata, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "终端元数据格式无效");
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
