package com.hmiot.hmiot_backend.module.policy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.module.policy.dto.AuthMethodCreateRequest;
import com.hmiot.hmiot_backend.module.policy.dto.AuthMethodQueryRequest;
import com.hmiot.hmiot_backend.module.policy.dto.AuthMethodStatusRequest;
import com.hmiot.hmiot_backend.module.policy.dto.AuthMethodUpdateRequest;
import com.hmiot.hmiot_backend.module.policy.entity.AuthMethod;
import com.hmiot.hmiot_backend.module.policy.mapper.AuthMethodMapper;
import com.hmiot.hmiot_backend.module.policy.vo.AuthMethodVO;
import com.hmiot.hmiot_backend.module.user.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AuthMethodService {
    private final AuthMethodMapper authMethodMapper;

    public AuthMethodService(AuthMethodMapper authMethodMapper) {
        this.authMethodMapper = authMethodMapper;
    }

    public PageVO<AuthMethodVO> listMethods(AuthMethodQueryRequest request) {
        LambdaQueryWrapper<AuthMethod> wrapper = new LambdaQueryWrapper<AuthMethod>()
                .orderByAsc(AuthMethod::getSortOrder)
                .orderByDesc(AuthMethod::getCreatedAt);
        if (StringUtils.hasText(request.keyword())) {
            wrapper.and(condition -> condition.like(AuthMethod::getMethodCode, request.keyword()).or().like(AuthMethod::getMethodName, request.keyword()));
        }
        if (StringUtils.hasText(request.methodType())) {
            wrapper.eq(AuthMethod::getMethodType, request.methodType());
        }
        if (request.enabled() != null) {
            wrapper.eq(AuthMethod::getEnabled, request.enabled());
        }
        Page<AuthMethod> page = authMethodMapper.selectPage(Page.of(request.pageCurrent(), request.pageSize()), wrapper);
        List<AuthMethodVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageVO<>(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    public AuthMethodVO getMethod(Long id) {
        return toVO(findMethod(id));
    }

    @Transactional
    public AuthMethodVO createMethod(AuthMethodCreateRequest request) {
        ensureMethodCodeUnique(request.methodCode(), null);
        AuthMethod method = new AuthMethod();
        method.setMethodCode(request.methodCode());
        method.setMethodName(request.methodName());
        method.setMethodType(request.methodType());
        method.setCompareMode(request.compareMode());
        method.setEnabled(request.enabled() == null || request.enabled());
        method.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        method.setDescription(request.description());
        authMethodMapper.insert(method);
        return toVO(method);
    }

    @Transactional
    public AuthMethodVO updateMethod(Long id, AuthMethodUpdateRequest request) {
        AuthMethod method = findMethod(id);
        method.setMethodName(request.methodName());
        method.setMethodType(request.methodType());
        method.setCompareMode(request.compareMode());
        method.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        method.setDescription(request.description());
        authMethodMapper.updateById(method);
        return toVO(method);
    }

    @Transactional
    public AuthMethodVO updateStatus(Long id, AuthMethodStatusRequest request) {
        AuthMethod method = findMethod(id);
        method.setEnabled(request.enabled());
        authMethodMapper.updateById(method);
        return toVO(method);
    }

    public AuthMethod findEnabledByCode(String methodCode) {
        AuthMethod method = authMethodMapper.selectOne(new LambdaQueryWrapper<AuthMethod>().eq(AuthMethod::getMethodCode, methodCode));
        if (method == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "认证方式不存在：" + methodCode);
        }
        if (!Boolean.TRUE.equals(method.getEnabled())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "认证方式已停用：" + methodCode);
        }
        return method;
    }

    public AuthMethod findMethod(Long id) {
        AuthMethod method = authMethodMapper.selectById(id);
        if (method == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "认证方式不存在");
        }
        return method;
    }

    public AuthMethodVO toVO(AuthMethod method) {
        return new AuthMethodVO(method.getId(), method.getMethodCode(), method.getMethodName(), method.getMethodType(), method.getCompareMode(), method.getEnabled(), method.getSortOrder(), method.getDescription(), method.getCreatedAt(), method.getUpdatedAt());
    }

    private void ensureMethodCodeUnique(String methodCode, Long excludeId) {
        AuthMethod existing = authMethodMapper.selectOne(new LambdaQueryWrapper<AuthMethod>().eq(AuthMethod::getMethodCode, methodCode));
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "认证方式编码已存在");
        }
    }
}
