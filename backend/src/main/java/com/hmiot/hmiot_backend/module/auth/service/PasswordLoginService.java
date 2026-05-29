package com.hmiot.hmiot_backend.module.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.common.security.JwtTokenProvider;
import com.hmiot.hmiot_backend.module.audit.service.AuditLogService;
import com.hmiot.hmiot_backend.module.auth.dto.PasswordLoginRequest;
import com.hmiot.hmiot_backend.module.auth.vo.UserLoginResponse;
import com.hmiot.hmiot_backend.module.biometric.entity.BiometricAuthRecord;
import com.hmiot.hmiot_backend.module.biometric.service.BiometricTemplateService;
import com.hmiot.hmiot_backend.module.binding.service.TerminalBindingValidationService;
import com.hmiot.hmiot_backend.module.device.dto.TerminalRegisterRequest;
import com.hmiot.hmiot_backend.module.device.entity.IotDeviceStatus;
import com.hmiot.hmiot_backend.module.device.entity.TerminalInfo;
import com.hmiot.hmiot_backend.module.device.service.IotDeviceStatusService;
import com.hmiot.hmiot_backend.module.device.service.TerminalInfoService;
import com.hmiot.hmiot_backend.module.policy.service.PolicyExecutionService;
import com.hmiot.hmiot_backend.module.policy.vo.PolicyResolveResponse;
import com.hmiot.hmiot_backend.module.user.entity.UserAccount;
import com.hmiot.hmiot_backend.module.user.mapper.UserAccountMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PasswordLoginService {
    private static final String PASSWORD_LOGIN = "PASSWORD_LOGIN";
    private static final long DYNAMIC_BIOMETRIC_AUTH_VALID_SECONDS = 300;

    private final UserAccountMapper userAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TerminalInfoService terminalInfoService;
    private final TerminalBindingValidationService terminalBindingValidationService;
    private final PolicyExecutionService policyExecutionService;
    private final BiometricTemplateService biometricTemplateService;
    private final IotDeviceStatusService iotDeviceStatusService;
    private final AuditLogService auditLogService;

    public PasswordLoginService(UserAccountMapper userAccountMapper, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider, TerminalInfoService terminalInfoService, TerminalBindingValidationService terminalBindingValidationService, PolicyExecutionService policyExecutionService, BiometricTemplateService biometricTemplateService, IotDeviceStatusService iotDeviceStatusService, AuditLogService auditLogService) {
        this.userAccountMapper = userAccountMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.terminalInfoService = terminalInfoService;
        this.terminalBindingValidationService = terminalBindingValidationService;
        this.policyExecutionService = policyExecutionService;
        this.biometricTemplateService = biometricTemplateService;
        this.iotDeviceStatusService = iotDeviceStatusService;
        this.auditLogService = auditLogService;
    }

    public UserLoginResponse login(PasswordLoginRequest request, String requestIp) {
        UserAccount user = findEnabledUser(request.username());
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            auditLogService.recordUserLoginFailure(user.getId(), user.getUsername(), PASSWORD_LOGIN, requestIp, "账号或密码错误", Map.of("username", user.getUsername()));
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }
        TerminalInfo terminal = terminalInfoService.resolveOrRegister(new TerminalRegisterRequest(
                request.terminalCode(),
                request.deviceFingerprint(),
                request.clientType(),
                request.clientVersion(),
                request.osName(),
                request.osVersion(),
                request.userAgent(),
                request.metadata()
        ), requestIp);
        terminalBindingValidationService.validateUserTerminalForLogin(user.getId(), user.getUsername(), terminal.getId(), requestIp);
        PolicyResolveResponse policy = policyExecutionService.ensurePolicyAllowsLogin(user.getId(), terminal.getId(), terminal.getClientType(), PASSWORD_LOGIN);
        BiometricAuthRecord dynamicBiometricRecord = ensureDynamicBiometricSatisfied(policy, request.dynamicBiometricAuthRecordId(), user);
        IotDeviceStatus iotDeviceStatus = ensureIotDeviceSatisfied(policy, terminal.getId());

        String token = jwtTokenProvider.generateToken(String.valueOf(user.getId()), Map.of(
                "principalType", "USER",
                "username", user.getUsername(),
                "realName", user.getRealName(),
                "authMethod", PASSWORD_LOGIN,
                "terminalId", terminal.getId(),
                "terminalCode", terminal.getTerminalCode(),
                "clientType", terminal.getClientType()
        ));
        auditLogService.recordUserLoginSuccess(user.getId(), user.getUsername(), PASSWORD_LOGIN, requestIp, loginAuditDetail(terminal, dynamicBiometricRecord, iotDeviceStatus));
        return new UserLoginResponse(token, "Bearer", user.getId(), user.getUsername(), user.getRealName(), PASSWORD_LOGIN, terminal.getId(), terminal.getTerminalCode(), terminal.getClientType());
    }

    private BiometricAuthRecord ensureDynamicBiometricSatisfied(PolicyResolveResponse policy, Long dynamicBiometricAuthRecordId, UserAccount user) {
        if (!policy.authChain().contains(BiometricTemplateService.DYNAMIC_BIOMETRIC)) {
            return null;
        }
        if (dynamicBiometricAuthRecordId == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前登录策略要求完成动态生物认证");
        }
        BiometricAuthRecord record = biometricTemplateService.findSuccessfulDynamicBiometricRecord(dynamicBiometricAuthRecordId, user.getId());
        if (record.getCreatedAt() == null || record.getCreatedAt().isBefore(LocalDateTime.now().minusSeconds(DYNAMIC_BIOMETRIC_AUTH_VALID_SECONDS))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "动态生物认证已过期，请重新验证");
        }
        return record;
    }

    private IotDeviceStatus ensureIotDeviceSatisfied(PolicyResolveResponse policy, Long terminalId) {
        if (!policy.authChain().contains(IotDeviceStatusService.IOT_DEVICE)) {
            return null;
        }
        return iotDeviceStatusService.ensureTerminalDeviceTrusted(terminalId);
    }

    private Map<String, Object> loginAuditDetail(TerminalInfo terminal, BiometricAuthRecord dynamicBiometricRecord, IotDeviceStatus iotDeviceStatus) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("terminalId", terminal.getId());
        detail.put("terminalCode", terminal.getTerminalCode());
        if (dynamicBiometricRecord != null) {
            detail.put("dynamicBiometricAuthRecordId", dynamicBiometricRecord.getId());
            detail.put("dynamicBiometricAuthAt", dynamicBiometricRecord.getCreatedAt());
        }
        detail.putAll(iotDeviceStatusService.safeSummary(iotDeviceStatus));
        return detail;
    }

    private UserAccount findEnabledUser(String username) {
        UserAccount user = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getUsername, username)
                .eq(UserAccount::getDeleted, false));
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }
        if (!"ENABLED".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "用户账号未审核通过或不可用");
        }
        return user;
    }
}
