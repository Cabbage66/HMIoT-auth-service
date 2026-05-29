package com.hmiot.hmiot_backend.module.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.module.device.dto.DeviceEntityCreateRequest;
import com.hmiot.hmiot_backend.module.device.dto.DeviceEntityQueryRequest;
import com.hmiot.hmiot_backend.module.device.dto.DeviceEntityStatusRequest;
import com.hmiot.hmiot_backend.module.device.dto.DeviceEntityUpdateRequest;
import com.hmiot.hmiot_backend.module.device.entity.DeviceEntity;
import com.hmiot.hmiot_backend.module.device.mapper.DeviceEntityMapper;
import com.hmiot.hmiot_backend.module.device.vo.DeviceEntityVO;
import com.hmiot.hmiot_backend.module.user.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class DeviceEntityService {
    public static final String ENABLED = "ENABLED";

    private final DeviceEntityMapper deviceEntityMapper;

    public DeviceEntityService(DeviceEntityMapper deviceEntityMapper) {
        this.deviceEntityMapper = deviceEntityMapper;
    }

    public PageVO<DeviceEntityVO> listDevices(DeviceEntityQueryRequest request) {
        LambdaQueryWrapper<DeviceEntity> wrapper = new LambdaQueryWrapper<DeviceEntity>()
                .eq(DeviceEntity::getDeleted, false)
                .orderByDesc(DeviceEntity::getCreatedAt);
        if (StringUtils.hasText(request.keyword())) {
            wrapper.and(condition -> condition.like(DeviceEntity::getDeviceCode, request.keyword()).or().like(DeviceEntity::getDeviceName, request.keyword()));
        }
        if (StringUtils.hasText(request.deviceType())) {
            wrapper.eq(DeviceEntity::getDeviceType, request.deviceType());
        }
        if (StringUtils.hasText(request.status())) {
            wrapper.eq(DeviceEntity::getStatus, request.status());
        }
        if (StringUtils.hasText(request.sourceSystem())) {
            wrapper.eq(DeviceEntity::getSourceSystem, request.sourceSystem());
        }
        Page<DeviceEntity> page = deviceEntityMapper.selectPage(Page.of(request.pageCurrent(), request.pageSize()), wrapper);
        List<DeviceEntityVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageVO<>(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    public DeviceEntityVO getDevice(Long id) {
        return toVO(findDevice(id));
    }

    @Transactional
    public DeviceEntityVO createDevice(DeviceEntityCreateRequest request) {
        ensureDeviceCodeUnique(request.deviceCode(), null);
        DeviceEntity device = new DeviceEntity();
        device.setEntityId(request.entityId());
        device.setDeviceCode(request.deviceCode());
        device.setDeviceName(request.deviceName());
        device.setDeviceType(request.deviceType());
        device.setDeviceFingerprint(request.deviceFingerprint());
        device.setCertSubject(request.certSubject());
        device.setStatus(StringUtils.hasText(request.status()) ? request.status() : ENABLED);
        device.setSourceSystem(request.sourceSystem());
        device.setDescription(request.description());
        device.setMetadata(request.metadata());
        device.setDeleted(false);
        deviceEntityMapper.insert(device);
        return toVO(device);
    }

    @Transactional
    public DeviceEntityVO updateDevice(Long id, DeviceEntityUpdateRequest request) {
        DeviceEntity device = findDevice(id);
        device.setEntityId(request.entityId());
        device.setDeviceName(request.deviceName());
        device.setDeviceType(request.deviceType());
        device.setDeviceFingerprint(request.deviceFingerprint());
        device.setCertSubject(request.certSubject());
        if (StringUtils.hasText(request.status())) {
            device.setStatus(request.status());
        }
        device.setSourceSystem(request.sourceSystem());
        device.setDescription(request.description());
        device.setMetadata(request.metadata());
        deviceEntityMapper.updateById(device);
        return toVO(device);
    }

    @Transactional
    public DeviceEntityVO updateStatus(Long id, DeviceEntityStatusRequest request) {
        DeviceEntity device = findDevice(id);
        device.setStatus(request.status());
        deviceEntityMapper.updateById(device);
        return toVO(device);
    }

    public DeviceEntity findDevice(Long id) {
        DeviceEntity device = deviceEntityMapper.selectOne(new LambdaQueryWrapper<DeviceEntity>()
                .eq(DeviceEntity::getId, id)
                .eq(DeviceEntity::getDeleted, false));
        if (device == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "设备实体不存在");
        }
        return device;
    }

    public DeviceEntity findEnabledDevice(Long id) {
        DeviceEntity device = findDevice(id);
        if (!ENABLED.equals(device.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "设备实体不可用");
        }
        return device;
    }

    public DeviceEntity findEnabledByFingerprint(String fingerprint) {
        if (!StringUtils.hasText(fingerprint)) {
            return null;
        }
        DeviceEntity device = deviceEntityMapper.selectOne(new LambdaQueryWrapper<DeviceEntity>()
                .eq(DeviceEntity::getDeviceFingerprint, fingerprint)
                .eq(DeviceEntity::getDeleted, false)
                .last("LIMIT 1"));
        if (device != null && !ENABLED.equals(device.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "设备实体不可用");
        }
        return device;
    }

    public DeviceEntityVO toVO(DeviceEntity device) {
        if (device == null) {
            return null;
        }
        return new DeviceEntityVO(
                device.getId(),
                device.getEntityId(),
                device.getDeviceCode(),
                device.getDeviceName(),
                device.getDeviceType(),
                device.getDeviceFingerprint(),
                device.getCertSubject(),
                device.getStatus(),
                device.getSourceSystem(),
                device.getDescription(),
                device.getMetadata(),
                device.getCreatedAt(),
                device.getUpdatedAt()
        );
    }

    private void ensureDeviceCodeUnique(String deviceCode, Long excludeId) {
        DeviceEntity existing = deviceEntityMapper.selectOne(new LambdaQueryWrapper<DeviceEntity>()
                .eq(DeviceEntity::getDeviceCode, deviceCode)
                .eq(DeviceEntity::getDeleted, false));
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "设备编码已存在");
        }
    }
}
