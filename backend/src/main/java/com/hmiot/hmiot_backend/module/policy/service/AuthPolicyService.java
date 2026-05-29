package com.hmiot.hmiot_backend.module.policy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.common.security.CurrentAdmin;
import com.hmiot.hmiot_backend.module.app.entity.AppResource;
import com.hmiot.hmiot_backend.module.app.service.AppResourceService;
import com.hmiot.hmiot_backend.module.policy.dto.AuthPolicyCreateRequest;
import com.hmiot.hmiot_backend.module.policy.dto.AuthPolicyQueryRequest;
import com.hmiot.hmiot_backend.module.policy.dto.AuthPolicyUpdateRequest;
import com.hmiot.hmiot_backend.module.policy.dto.PolicyResolveRequest;
import com.hmiot.hmiot_backend.module.policy.dto.PolicyStatusRequest;
import com.hmiot.hmiot_backend.module.policy.entity.AuthPolicy;
import com.hmiot.hmiot_backend.module.policy.mapper.AuthPolicyMapper;
import com.hmiot.hmiot_backend.module.policy.vo.AuthPolicyVO;
import com.hmiot.hmiot_backend.module.user.vo.PageVO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AuthPolicyService {
    public static final String ENABLED = "ENABLED";
    public static final String DISABLED = "DISABLED";
    public static final String LOGIN = "LOGIN";
    public static final String APP_ACCESS = "APP_ACCESS";
    public static final String STEP_UP_AUTH = "STEP_UP_AUTH";
    public static final String DENY = "DENY";
    public static final String STEP_UP = "STEP_UP";
    public static final String WARN = "WARN";
    private static final Set<String> SCENE_TYPES = Set.of(LOGIN, APP_ACCESS, STEP_UP_AUTH, "USB_LOGIN", "CERTIFICATE_LOGIN");
    private static final Set<String> FAIL_ACTIONS = Set.of(DENY, STEP_UP, WARN);

    private final AuthPolicyMapper authPolicyMapper;
    private final AuthMethodService authMethodService;
    private final AppResourceService appResourceService;
    private final ObjectMapper objectMapper;

    public AuthPolicyService(AuthPolicyMapper authPolicyMapper, AuthMethodService authMethodService, AppResourceService appResourceService, ObjectMapper objectMapper) {
        this.authPolicyMapper = authPolicyMapper;
        this.authMethodService = authMethodService;
        this.appResourceService = appResourceService;
        this.objectMapper = objectMapper;
    }

    public PageVO<AuthPolicyVO> listPolicies(AuthPolicyQueryRequest request) {
        LambdaQueryWrapper<AuthPolicy> wrapper = new LambdaQueryWrapper<AuthPolicy>().orderByDesc(AuthPolicy::getUpdatedAt);
        if (StringUtils.hasText(request.keyword())) {
            wrapper.and(condition -> condition.like(AuthPolicy::getPolicyCode, request.keyword()).or().like(AuthPolicy::getPolicyName, request.keyword()));
        }
        if (StringUtils.hasText(request.sceneType())) {
            wrapper.eq(AuthPolicy::getSceneType, request.sceneType());
        }
        if (request.appResourceId() != null) {
            wrapper.eq(AuthPolicy::getAppResourceId, request.appResourceId());
        }
        if (StringUtils.hasText(request.status())) {
            wrapper.eq(AuthPolicy::getStatus, request.status());
        }
        Page<AuthPolicy> page = authPolicyMapper.selectPage(Page.of(request.pageCurrent(), request.pageSize()), wrapper);
        List<AuthPolicyVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageVO<>(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    public AuthPolicyVO getPolicy(Long id) {
        return toVO(findPolicy(id));
    }

    @Transactional
    public AuthPolicyVO createPolicy(AuthPolicyCreateRequest request) {
        ensurePolicyCodeUnique(request.policyCode(), null);
        validatePolicy(request.sceneType(), request.appResourceId(), request.userScope(), request.deviceScope(), request.authChain(), request.minTrustLevel(), request.sessionTtlSeconds(), request.actionOnFail());
        AuthPolicy policy = new AuthPolicy();
        policy.setPolicyCode(request.policyCode());
        policy.setPolicyName(request.policyName());
        applyMutableFields(policy, request.sceneType(), request.appResourceId(), request.userScope(), request.deviceScope(), request.authChain(), request.minTrustLevel(), request.sessionTtlSeconds(), request.actionOnFail(), request.status());
        policy.setVersionNo(1);
        policy.setCreatedBy(currentAdminId());
        authPolicyMapper.insert(policy);
        return toVO(policy);
    }

    @Transactional
    public AuthPolicyVO updatePolicy(Long id, AuthPolicyUpdateRequest request) {
        AuthPolicy policy = findPolicy(id);
        validatePolicy(request.sceneType(), request.appResourceId(), request.userScope(), request.deviceScope(), request.authChain(), request.minTrustLevel(), request.sessionTtlSeconds(), request.actionOnFail());
        policy.setPolicyName(request.policyName());
        applyMutableFields(policy, request.sceneType(), request.appResourceId(), request.userScope(), request.deviceScope(), request.authChain(), request.minTrustLevel(), request.sessionTtlSeconds(), request.actionOnFail(), request.status());
        policy.setVersionNo(policy.getVersionNo() == null ? 1 : policy.getVersionNo() + 1);
        authPolicyMapper.updateById(policy);
        return toVO(policy);
    }

    @Transactional
    public AuthPolicyVO updateStatus(Long id, PolicyStatusRequest request) {
        AuthPolicy policy = findPolicy(id);
        ensureStatus(request.status());
        policy.setStatus(request.status());
        authPolicyMapper.updateById(policy);
        return toVO(policy);
    }

    public AuthPolicy findPolicy(Long id) {
        AuthPolicy policy = authPolicyMapper.selectById(id);
        if (policy == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "认证策略不存在");
        }
        return policy;
    }

    public List<AuthPolicy> findEnabledByScene(String sceneType) {
        return authPolicyMapper.selectList(new LambdaQueryWrapper<AuthPolicy>()
                .eq(AuthPolicy::getSceneType, sceneType)
                .eq(AuthPolicy::getStatus, ENABLED)
                .orderByDesc(AuthPolicy::getAppResourceId)
                .orderByDesc(AuthPolicy::getUpdatedAt));
    }

    public AuthPolicyVO toVO(AuthPolicy policy) {
        AppResource app = policy.getAppResourceId() == null ? null : appResourceService.findApp(policy.getAppResourceId());
        return new AuthPolicyVO(policy.getId(), policy.getPolicyCode(), policy.getPolicyName(), policy.getSceneType(), policy.getAppResourceId(), policy.getUserScope(), policy.getDeviceScope(), policy.getAuthChain(), policy.getMinTrustLevel(), policy.getSessionTtlSeconds(), policy.getActionOnFail(), policy.getStatus(), policy.getVersionNo(), policy.getCreatedBy(), policy.getCreatedAt(), policy.getUpdatedAt(), app == null ? null : appResourceService.toVO(app));
    }

    public List<String> authChain(AuthPolicy policy) {
        return authChain(policy.getAuthChain());
    }

    public List<String> authChain(String authChain) {
        try {
            List<String> chain = objectMapper.readValue(authChain, new TypeReference<>() {
            });
            if (chain.isEmpty()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "认证链路不能为空");
            }
            return chain;
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "认证链路必须是JSON数组");
        }
    }

    public Map<String, Object> scope(String scope) {
        if (!StringUtils.hasText(scope)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(scope, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "策略范围必须是JSON对象");
        }
    }

    private void applyMutableFields(AuthPolicy policy, String sceneType, Long appResourceId, String userScope, String deviceScope, String authChain, Integer minTrustLevel, Integer sessionTtlSeconds, String actionOnFail, String status) {
        policy.setSceneType(sceneType);
        policy.setAppResourceId(appResourceId);
        policy.setUserScope(userScope);
        policy.setDeviceScope(deviceScope);
        policy.setAuthChain(authChain);
        policy.setMinTrustLevel(minTrustLevel == null ? 0 : minTrustLevel);
        policy.setSessionTtlSeconds(sessionTtlSeconds == null ? 7200 : sessionTtlSeconds);
        policy.setActionOnFail(StringUtils.hasText(actionOnFail) ? actionOnFail : DENY);
        policy.setStatus(StringUtils.hasText(status) ? status : ENABLED);
    }

    private void validatePolicy(String sceneType, Long appResourceId, String userScope, String deviceScope, String authChain, Integer minTrustLevel, Integer sessionTtlSeconds, String actionOnFail) {
        if (!SCENE_TYPES.contains(sceneType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "场景类型不支持");
        }
        if (appResourceId != null) {
            appResourceService.findApp(appResourceId);
        }
        List<String> chain = authChain(authChain);
        chain.forEach(authMethodService::findEnabledByCode);
        scope(userScope);
        scope(deviceScope);
        if (minTrustLevel != null && minTrustLevel < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "最低信任等级不能小于0");
        }
        if (sessionTtlSeconds != null && (sessionTtlSeconds < 60 || sessionTtlSeconds > 86400)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "会话有效期必须在60到86400秒之间");
        }
        if (StringUtils.hasText(actionOnFail) && !FAIL_ACTIONS.contains(actionOnFail)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "失败处置动作不支持");
        }
    }

    private void ensurePolicyCodeUnique(String policyCode, Long excludeId) {
        AuthPolicy existing = authPolicyMapper.selectOne(new LambdaQueryWrapper<AuthPolicy>().eq(AuthPolicy::getPolicyCode, policyCode));
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "策略编码已存在");
        }
    }

    private void ensureStatus(String status) {
        if (!ENABLED.equals(status) && !DISABLED.equals(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "策略状态不支持");
        }
    }

    private Long currentAdminId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentAdmin currentAdmin)) {
            return null;
        }
        return currentAdmin.adminId();
    }
}
