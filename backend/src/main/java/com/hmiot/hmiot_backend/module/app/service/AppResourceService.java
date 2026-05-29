package com.hmiot.hmiot_backend.module.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.module.app.dto.AppResourceCreateRequest;
import com.hmiot.hmiot_backend.module.app.dto.AppResourceQueryRequest;
import com.hmiot.hmiot_backend.module.app.dto.AppResourceStatusRequest;
import com.hmiot.hmiot_backend.module.app.dto.AppResourceUpdateRequest;
import com.hmiot.hmiot_backend.module.app.entity.AppResource;
import com.hmiot.hmiot_backend.module.app.mapper.AppResourceMapper;
import com.hmiot.hmiot_backend.module.app.vo.AppResourceVO;
import com.hmiot.hmiot_backend.module.user.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AppResourceService {
    public static final String ENABLED = "ENABLED";

    private final AppResourceMapper appResourceMapper;

    public AppResourceService(AppResourceMapper appResourceMapper) {
        this.appResourceMapper = appResourceMapper;
    }

    public PageVO<AppResourceVO> listApps(AppResourceQueryRequest request) {
        LambdaQueryWrapper<AppResource> wrapper = new LambdaQueryWrapper<AppResource>()
                .eq(AppResource::getDeleted, false)
                .orderByDesc(AppResource::getCreatedAt);
        if (StringUtils.hasText(request.keyword())) {
            wrapper.and(condition -> condition.like(AppResource::getAppCode, request.keyword()).or().like(AppResource::getAppName, request.keyword()));
        }
        if (StringUtils.hasText(request.resourceType())) {
            wrapper.eq(AppResource::getResourceType, request.resourceType());
        }
        if (StringUtils.hasText(request.status())) {
            wrapper.eq(AppResource::getStatus, request.status());
        }
        if (request.securityLevel() != null) {
            wrapper.eq(AppResource::getSecurityLevel, request.securityLevel());
        }
        Page<AppResource> page = appResourceMapper.selectPage(Page.of(request.pageCurrent(), request.pageSize()), wrapper);
        List<AppResourceVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageVO<>(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    public AppResourceVO getApp(Long id) {
        return toVO(findApp(id));
    }

    @Transactional
    public AppResourceVO createApp(AppResourceCreateRequest request) {
        ensureAppCodeUnique(request.appCode(), null);
        AppResource app = new AppResource();
        app.setAppCode(request.appCode());
        app.setAppName(request.appName());
        app.setResourceType(StringUtils.hasText(request.resourceType()) ? request.resourceType() : "APP");
        app.setAccessUrl(request.accessUrl());
        app.setSecurityLevel(request.securityLevel());
        app.setRequiredTrustLevel(request.requiredTrustLevel());
        app.setStatus(StringUtils.hasText(request.status()) ? request.status() : ENABLED);
        app.setDescription(request.description());
        app.setMetadata(request.metadata());
        app.setDeleted(false);
        appResourceMapper.insert(app);
        return toVO(app);
    }

    @Transactional
    public AppResourceVO updateApp(Long id, AppResourceUpdateRequest request) {
        AppResource app = findApp(id);
        app.setAppName(request.appName());
        app.setResourceType(StringUtils.hasText(request.resourceType()) ? request.resourceType() : "APP");
        app.setAccessUrl(request.accessUrl());
        app.setSecurityLevel(request.securityLevel());
        app.setRequiredTrustLevel(request.requiredTrustLevel());
        if (StringUtils.hasText(request.status())) {
            app.setStatus(request.status());
        }
        app.setDescription(request.description());
        app.setMetadata(request.metadata());
        appResourceMapper.updateById(app);
        return toVO(app);
    }

    @Transactional
    public AppResourceVO updateStatus(Long id, AppResourceStatusRequest request) {
        AppResource app = findApp(id);
        app.setStatus(request.status());
        appResourceMapper.updateById(app);
        return toVO(app);
    }

    public AppResource findEnabledByCode(String appCode) {
        AppResource app = appResourceMapper.selectOne(new LambdaQueryWrapper<AppResource>()
                .eq(AppResource::getAppCode, appCode)
                .eq(AppResource::getDeleted, false));
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "应用资源不存在");
        }
        if (!ENABLED.equals(app.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "应用资源不可用");
        }
        return app;
    }

    public AppResource findApp(Long id) {
        AppResource app = appResourceMapper.selectOne(new LambdaQueryWrapper<AppResource>()
                .eq(AppResource::getId, id)
                .eq(AppResource::getDeleted, false));
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "应用资源不存在");
        }
        return app;
    }

    public AppResourceVO toVO(AppResource app) {
        return new AppResourceVO(
                app.getId(),
                app.getAppCode(),
                app.getAppName(),
                app.getResourceType(),
                app.getAccessUrl(),
                app.getSecurityLevel(),
                app.getRequiredTrustLevel(),
                app.getStatus(),
                app.getDescription(),
                app.getMetadata(),
                app.getCreatedAt(),
                app.getUpdatedAt()
        );
    }

    private void ensureAppCodeUnique(String appCode, Long excludeId) {
        AppResource existing = appResourceMapper.selectOne(new LambdaQueryWrapper<AppResource>()
                .eq(AppResource::getAppCode, appCode)
                .eq(AppResource::getDeleted, false));
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "应用编码已存在");
        }
    }
}
