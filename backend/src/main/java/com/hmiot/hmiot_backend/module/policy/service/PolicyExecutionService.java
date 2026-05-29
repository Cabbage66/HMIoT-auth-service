package com.hmiot.hmiot_backend.module.policy.service;

import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.common.security.CurrentUser;
import com.hmiot.hmiot_backend.module.app.entity.AppResource;
import com.hmiot.hmiot_backend.module.app.service.AppResourceService;
import com.hmiot.hmiot_backend.module.policy.dto.ClientPolicyQueryRequest;
import com.hmiot.hmiot_backend.module.policy.dto.PolicyResolveRequest;
import com.hmiot.hmiot_backend.module.policy.entity.AuthPolicy;
import com.hmiot.hmiot_backend.module.policy.vo.ClientPolicyBundleVO;
import com.hmiot.hmiot_backend.module.policy.vo.PolicyResolveResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PolicyExecutionService {
    private static final Set<String> IMPLEMENTED_METHODS = Set.of(
            "USB_PIN", "USB_LOGIN",
            "CERTIFICATE", "CERTIFICATE_LOGIN",
            "PASSWORD", "PASSWORD_LOGIN",
            "FINGERPRINT", "FINGERPRINT_LOGIN",
            "FACE", "FACE_LOGIN",
            "VOICEPRINT", "VOICEPRINT_LOGIN",
            "DYNAMIC_BIOMETRIC", "IOT_DEVICE"
    );

    private final AuthPolicyService authPolicyService;
    private final AppResourceService appResourceService;
    private final SecurityBaselineService securityBaselineService;
    private final PolicyPublishService policyPublishService;

    public PolicyExecutionService(AuthPolicyService authPolicyService, AppResourceService appResourceService, SecurityBaselineService securityBaselineService, PolicyPublishService policyPublishService) {
        this.authPolicyService = authPolicyService;
        this.appResourceService = appResourceService;
        this.securityBaselineService = securityBaselineService;
        this.policyPublishService = policyPublishService;
    }

    public PolicyResolveResponse resolvePolicy(PolicyResolveRequest request) {
        AppResource app = resolveApp(request.appCode(), request.appResourceId());
        String sceneType = normalizeScene(request.sceneType());
        AuthPolicy policy = bestPolicy(sceneType, app, request.userId(), request.terminalId(), request.clientType());
        if (policy != null) {
            List<String> chain = authPolicyService.authChain(policy);
            return new PolicyResolveResponse(sceneType, policy.getId(), policy.getPolicyCode(), policy.getPolicyName(), policy.getVersionNo(), policy.getAppResourceId(), app == null ? null : app.getAppCode(), chain, policy.getMinTrustLevel(), policy.getSessionTtlSeconds(), policy.getActionOnFail(), true, "POLICY_MATCHED", app == null ? null : appResourceService.toVO(app));
        }
        return defaultPolicy(sceneType, app);
    }

    public PolicyResolveResponse ensurePolicyAllowsLogin(Long userId, Long terminalId, String clientType, String authMethod) {
        PolicyResolveResponse resolved = resolvePolicy(new PolicyResolveRequest(AuthPolicyService.LOGIN, null, null, userId, terminalId, clientType, trustLevel(authMethod), authMethod));
        ensureImplemented(resolved);
        if (!methodSatisfied(resolved.authChain(), authMethod)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前登录认证方式不满足登录策略要求");
        }
        return resolved;
    }

    public PolicyResolveResponse ensurePolicyAllowsAccess(AppResource app, CurrentUser currentUser, Integer currentTrustLevel, String authMethod) {
        PolicyResolveResponse resolved = resolvePolicy(new PolicyResolveRequest(AuthPolicyService.APP_ACCESS, app.getAppCode(), app.getId(), currentUser.userId(), currentUser.terminalId(), currentUser.clientType(), currentTrustLevel, authMethod));
        ensureImplemented(resolved);
        if (!methodSatisfied(resolved.authChain(), authMethod)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前认证方式不满足访问策略要求，请重新认证或增强认证");
        }
        if (currentTrustLevel == null || currentTrustLevel < resolved.minTrustLevel()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前信任等级不足，请重新认证或增强认证");
        }
        return resolved;
    }

    public ClientPolicyBundleVO clientPolicies(ClientPolicyQueryRequest request, CurrentUser currentUser) {
        ensureTerminalContext(currentUser);
        List<String> scenes = StringUtils.hasText(request.sceneType()) ? List.of(normalizeScene(request.sceneType())) : List.of(AuthPolicyService.LOGIN, AuthPolicyService.APP_ACCESS, AuthPolicyService.STEP_UP_AUTH);
        List<PolicyResolveResponse> policies = scenes.stream()
                .map(scene -> resolvePolicy(new PolicyResolveRequest(scene, request.appCode(), null, currentUser.userId(), currentUser.terminalId(), currentUser.clientType(), trustLevel(currentUser.authMethod()), currentUser.authMethod())))
                .toList();
        return new ClientPolicyBundleVO(currentUser.userId(), currentUser.terminalId(), currentUser.terminalCode(), currentUser.clientType(), policies, securityBaselineService.listEnabledForClient(currentUser.clientType(), currentUser.terminalId()), policyPublishService.pendingForClient(currentUser));
    }

    public int trustLevel(String authMethod) {
        return "USB_LOGIN".equals(authMethod)
                || "CERTIFICATE_LOGIN".equals(authMethod)
                || "FINGERPRINT_LOGIN".equals(authMethod)
                || "FACE_LOGIN".equals(authMethod)
                || "VOICEPRINT_LOGIN".equals(authMethod) ? 2 : 1;
    }

    public void ensureTerminalContext(CurrentUser currentUser) {
        if (currentUser.terminalId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前用户缺少可信终端上下文，请重新登录");
        }
    }

    private AuthPolicy bestPolicy(String sceneType, AppResource app, Long userId, Long terminalId, String clientType) {
        List<AuthPolicy> policies = authPolicyService.findEnabledByScene(sceneType);
        AuthPolicy scopedAppPolicy = policies.stream()
                .filter(policy -> app != null && app.getId().equals(policy.getAppResourceId()))
                .filter(policy -> scopeMatches(policy, userId, terminalId, clientType))
                .findFirst()
                .orElse(null);
        if (scopedAppPolicy != null) {
            return scopedAppPolicy;
        }
        AuthPolicy appDefaultPolicy = policies.stream()
                .filter(policy -> app != null && app.getId().equals(policy.getAppResourceId()))
                .filter(policy -> scopeEmpty(policy.getUserScope()) && scopeEmpty(policy.getDeviceScope()))
                .findFirst()
                .orElse(null);
        if (appDefaultPolicy != null) {
            return appDefaultPolicy;
        }
        return policies.stream()
                .filter(policy -> policy.getAppResourceId() == null)
                .filter(policy -> scopeMatches(policy, userId, terminalId, clientType))
                .findFirst()
                .orElse(null);
    }

    private boolean scopeMatches(AuthPolicy policy, Long userId, Long terminalId, String clientType) {
        return userScopeMatches(policy.getUserScope(), userId) && deviceScopeMatches(policy.getDeviceScope(), terminalId, clientType);
    }

    private boolean userScopeMatches(String scopeJson, Long userId) {
        Map<String, Object> scope = authPolicyService.scope(scopeJson);
        if (scope.isEmpty()) {
            return true;
        }
        Object userIds = scope.get("userIds");
        return !(userIds instanceof List<?> list) || list.isEmpty() || list.stream().map(String::valueOf).anyMatch(value -> value.equals(String.valueOf(userId)));
    }

    private boolean deviceScopeMatches(String scopeJson, Long terminalId, String clientType) {
        Map<String, Object> scope = authPolicyService.scope(scopeJson);
        if (scope.isEmpty()) {
            return true;
        }
        Object terminalIds = scope.get("terminalIds");
        if (terminalIds instanceof List<?> list && !list.isEmpty() && list.stream().map(String::valueOf).noneMatch(value -> value.equals(String.valueOf(terminalId)))) {
            return false;
        }
        Object clientTypes = scope.get("clientTypes");
        return !(clientTypes instanceof List<?> list) || list.isEmpty() || list.contains(clientType);
    }

    private boolean scopeEmpty(String scopeJson) {
        return authPolicyService.scope(scopeJson).isEmpty();
    }

    private AppResource resolveApp(String appCode, Long appResourceId) {
        if (StringUtils.hasText(appCode)) {
            return appResourceService.findEnabledByCode(appCode);
        }
        if (appResourceId != null) {
            AppResource app = appResourceService.findApp(appResourceId);
            if (!AppResourceService.ENABLED.equals(app.getStatus())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "应用资源不可用");
            }
            return app;
        }
        return null;
    }

    private PolicyResolveResponse defaultPolicy(String sceneType, AppResource app) {
        if (AuthPolicyService.LOGIN.equals(sceneType)) {
            return new PolicyResolveResponse(sceneType, null, null, "安全默认登录策略", 1, null, null, List.of("PASSWORD"), 1, 7200, AuthPolicyService.DENY, false, "SECURITY_DEFAULT", null);
        }
        if (AuthPolicyService.APP_ACCESS.equals(sceneType) && app != null) {
            int minTrustLevel = app.getRequiredTrustLevel() == null ? 0 : app.getRequiredTrustLevel();
            return new PolicyResolveResponse(sceneType, null, null, "应用默认访问策略", 1, app.getId(), app.getAppCode(), List.of("PASSWORD"), minTrustLevel, 7200, AuthPolicyService.DENY, false, "APP_DEFAULT", appResourceService.toVO(app));
        }
        return new PolicyResolveResponse(sceneType, null, null, "安全默认拒绝策略", 1, app == null ? null : app.getId(), app == null ? null : app.getAppCode(), List.of(), 999, 0, AuthPolicyService.DENY, false, "DEFAULT_DENY", app == null ? null : appResourceService.toVO(app));
    }

    private String normalizeScene(String sceneType) {
        if ("USB_LOGIN".equals(sceneType) || "CERTIFICATE_LOGIN".equals(sceneType)) {
            return AuthPolicyService.LOGIN;
        }
        return sceneType;
    }

    private boolean methodSatisfied(List<String> chain, String authMethod) {
        if (chain.isEmpty()) {
            return false;
        }
        return chain.stream()
                .filter(method -> !"VOICEPRINT".equals(method) && !"DYNAMIC_BIOMETRIC".equals(method) && !"IOT_DEVICE".equals(method))
                .allMatch(method -> sameMethod(method, authMethod));
    }

    private boolean sameMethod(String requiredMethod, String authMethod) {
        if (requiredMethod.equals(authMethod)) {
            return true;
        }
        return "USB_PIN".equals(requiredMethod) && ("USB_LOGIN".equals(authMethod) || "CERTIFICATE_LOGIN".equals(authMethod))
                || "CERTIFICATE".equals(requiredMethod) && "CERTIFICATE_LOGIN".equals(authMethod)
                || "PASSWORD".equals(requiredMethod) && ("PASSWORD_LOGIN".equals(authMethod) || "FINGERPRINT_LOGIN".equals(authMethod) || "FACE_LOGIN".equals(authMethod) || "VOICEPRINT_LOGIN".equals(authMethod))
                || "FINGERPRINT".equals(requiredMethod) && "FINGERPRINT_LOGIN".equals(authMethod)
                || "FACE".equals(requiredMethod) && "FACE_LOGIN".equals(authMethod)
                || "VOICEPRINT".equals(requiredMethod) && "VOICEPRINT_LOGIN".equals(authMethod);
    }

    private void ensureImplemented(PolicyResolveResponse resolved) {
        for (String method : resolved.authChain()) {
            if (!IMPLEMENTED_METHODS.contains(method)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "认证方式暂未接入当前阶段：" + method);
            }
        }
    }
}
