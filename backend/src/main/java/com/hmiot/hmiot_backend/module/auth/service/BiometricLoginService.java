package com.hmiot.hmiot_backend.module.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.common.security.JwtTokenProvider;
import com.hmiot.hmiot_backend.module.audit.service.AuditLogService;
import com.hmiot.hmiot_backend.module.auth.dto.BiometricLoginRequest;
import com.hmiot.hmiot_backend.module.auth.dto.VoiceprintLoginRequest;
import com.hmiot.hmiot_backend.module.auth.vo.UserLoginResponse;
import com.hmiot.hmiot_backend.module.biometric.entity.BiometricAuthRecord;
import com.hmiot.hmiot_backend.module.biometric.entity.BiometricTemplate;
import com.hmiot.hmiot_backend.module.biometric.mapper.BiometricAuthRecordMapper;
import com.hmiot.hmiot_backend.module.biometric.service.BiometricTemplateService;
import com.hmiot.hmiot_backend.module.binding.service.TerminalBindingValidationService;
import com.hmiot.hmiot_backend.module.device.dto.TerminalRegisterRequest;
import com.hmiot.hmiot_backend.module.device.entity.IotDeviceStatus;
import com.hmiot.hmiot_backend.module.device.entity.TerminalInfo;
import com.hmiot.hmiot_backend.module.device.service.IotDeviceStatusService;
import com.hmiot.hmiot_backend.module.device.service.TerminalInfoService;
import com.hmiot.hmiot_backend.module.integration.service.ExternalIntegrationCallService;
import com.hmiot.hmiot_backend.module.integration.service.VoiceprintIntegrationClient;
import com.hmiot.hmiot_backend.module.policy.service.PolicyExecutionService;
import com.hmiot.hmiot_backend.module.policy.vo.PolicyResolveResponse;
import com.hmiot.hmiot_backend.module.user.entity.UserAccount;
import com.hmiot.hmiot_backend.module.user.mapper.UserAccountMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class BiometricLoginService {
    public static final String FINGERPRINT_LOGIN = "FINGERPRINT_LOGIN";
    public static final String FACE_LOGIN = "FACE_LOGIN";
    public static final String VOICEPRINT_LOGIN = "VOICEPRINT_LOGIN";

    private static final String FINGERPRINT = "FINGERPRINT";
    private static final String FACE = "FACE";
    private static final long DYNAMIC_BIOMETRIC_AUTH_VALID_SECONDS = 300;

    private final UserAccountMapper userAccountMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final TerminalInfoService terminalInfoService;
    private final TerminalBindingValidationService terminalBindingValidationService;
    private final PolicyExecutionService policyExecutionService;
    private final BiometricTemplateService biometricTemplateService;
    private final BiometricAuthRecordMapper biometricAuthRecordMapper;
    private final IotDeviceStatusService iotDeviceStatusService;
    private final VoiceprintIntegrationClient voiceprintIntegrationClient;
    private final ExternalIntegrationCallService externalIntegrationCallService;
    private final AuditLogService auditLogService;

    public BiometricLoginService(UserAccountMapper userAccountMapper, JwtTokenProvider jwtTokenProvider, TerminalInfoService terminalInfoService, TerminalBindingValidationService terminalBindingValidationService, PolicyExecutionService policyExecutionService, BiometricTemplateService biometricTemplateService, BiometricAuthRecordMapper biometricAuthRecordMapper, IotDeviceStatusService iotDeviceStatusService, VoiceprintIntegrationClient voiceprintIntegrationClient, ExternalIntegrationCallService externalIntegrationCallService, AuditLogService auditLogService) {
        this.userAccountMapper = userAccountMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.terminalInfoService = terminalInfoService;
        this.terminalBindingValidationService = terminalBindingValidationService;
        this.policyExecutionService = policyExecutionService;
        this.biometricTemplateService = biometricTemplateService;
        this.biometricAuthRecordMapper = biometricAuthRecordMapper;
        this.iotDeviceStatusService = iotDeviceStatusService;
        this.voiceprintIntegrationClient = voiceprintIntegrationClient;
        this.externalIntegrationCallService = externalIntegrationCallService;
        this.auditLogService = auditLogService;
    }

    public UserLoginResponse loginByFingerprint(BiometricLoginRequest request, String requestIp) {
        return localBiometricLogin(request, requestIp, FINGERPRINT_LOGIN, FINGERPRINT, "ANDROID_BIOMETRIC_PROMPT");
    }

    public UserLoginResponse loginByFace(BiometricLoginRequest request, String requestIp) {
        return localBiometricLogin(request, requestIp, FACE_LOGIN, FACE, "ANDROID_FACE_CAPTURE");
    }

    public UserLoginResponse loginByVoiceprint(VoiceprintLoginRequest request, String requestIp) {
        UserAccount user = findEnabledUser(request.username());
        TerminalInfo terminal = resolveTerminal(
                request.terminalCode(),
                request.deviceFingerprint(),
                request.clientType(),
                request.clientVersion(),
                request.osName(),
                request.osVersion(),
                request.userAgent(),
                request.metadata(),
                requestIp
        );
        terminalBindingValidationService.validateUserTerminalForLogin(user.getId(), user.getUsername(), terminal.getId(), requestIp);
        PolicyResolveResponse policy = policyExecutionService.ensurePolicyAllowsLogin(user.getId(), terminal.getId(), terminal.getClientType(), VOICEPRINT_LOGIN);
        BiometricAuthRecord dynamicBiometricRecord = ensureDynamicBiometricSatisfied(policy, request.dynamicBiometricAuthRecordId(), user);
        IotDeviceStatus iotDeviceStatus = ensureIotDeviceSatisfied(policy, terminal.getId());

        BiometricTemplate template = biometricTemplateService.latestApprovedVoiceprint(user.getId());
        LocalDateTime startedAt = LocalDateTime.now();
        Map<String, Object> requestSummary = Map.of(
                "userId", user.getId(),
                "username", user.getUsername(),
                "terminalId", terminal.getId(),
                "terminalCode", terminal.getTerminalCode(),
                "templateId", template.getId()
        );

        VoiceprintIntegrationClient.VerifyResult verifyResult;
        try {
            verifyResult = voiceprintIntegrationClient.verify(
                    template.getTemplateRef(),
                    template.getTemplateDigest(),
                    request.audioBase64(),
                    template.getThreshold(),
                    Map.of(
                            "scene", VOICEPRINT_LOGIN,
                            "audioFormat", request.audioFormat() == null ? "wav" : request.audioFormat(),
                            "sampleRate", request.sampleRate() == null ? 16000 : request.sampleRate()
                    )
            );
        } catch (RuntimeException exception) {
            BiometricAuthRecord failedRecord = insertVoiceprintRecord(user.getId(), template, null, null, template.getThreshold(), "FAIL", exception.getMessage(), voiceprintIntegrationClient.targetSystem(), Map.of("error", exception.getMessage()));
            externalIntegrationCallService.recordFailure("VOICEPRINT_LOGIN_VERIFY", voiceprintIntegrationClient.targetSystem(), String.valueOf(user.getId()), requestSummary, exception.getMessage(), startedAt);
            auditLogService.recordUserLoginFailure(user.getId(), user.getUsername(), VOICEPRINT_LOGIN, requestIp, exception.getMessage(), Map.of(
                    "terminalId", terminal.getId(),
                    "terminalCode", terminal.getTerminalCode(),
                    "voiceprintAuthRecordId", failedRecord.getId()
            ));
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "声纹认证失败");
        }

        String authResult = verifyResult.verified() ? BiometricTemplateService.SUCCESS : "FAIL";
        String failReason = verifyResult.verified() ? null : (verifyResult.failReason() == null ? "声纹相似度低于阈值" : verifyResult.failReason());
        BiometricAuthRecord voiceprintRecord = insertVoiceprintRecord(user.getId(), template, verifyResult.requestId(), verifyResult.score(), verifyResult.threshold(), authResult, failReason, verifyResult.modelSource(), verifyResult.rawResponse());
        externalIntegrationCallService.recordSuccess("VOICEPRINT_LOGIN_VERIFY", voiceprintIntegrationClient.targetSystem(), String.valueOf(user.getId()), requestSummary, Map.of(
                "recordId", voiceprintRecord.getId(),
                "verified", verifyResult.verified(),
                "score", verifyResult.score(),
                "threshold", verifyResult.threshold(),
                "failReason", failReason == null ? "" : failReason
        ), startedAt);
        if (!verifyResult.verified()) {
            auditLogService.recordUserLoginFailure(user.getId(), user.getUsername(), VOICEPRINT_LOGIN, requestIp, failReason, Map.of(
                    "terminalId", terminal.getId(),
                    "terminalCode", terminal.getTerminalCode(),
                    "voiceprintAuthRecordId", voiceprintRecord.getId()
            ));
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "声纹认证未通过");
        }

        String token = issueUserToken(user, terminal, VOICEPRINT_LOGIN);
        auditLogService.recordUserLoginSuccess(user.getId(), user.getUsername(), VOICEPRINT_LOGIN, requestIp, loginAuditDetail(terminal, dynamicBiometricRecord, iotDeviceStatus, voiceprintRecord));
        return new UserLoginResponse(token, "Bearer", user.getId(), user.getUsername(), user.getRealName(), VOICEPRINT_LOGIN, terminal.getId(), terminal.getTerminalCode(), terminal.getClientType());
    }

    private UserLoginResponse localBiometricLogin(BiometricLoginRequest request, String requestIp, String sceneType, String biometricType, String source) {
        UserAccount user = findEnabledUser(request.username());
        TerminalInfo terminal = resolveTerminal(
                request.terminalCode(),
                request.deviceFingerprint(),
                request.clientType(),
                request.clientVersion(),
                request.osName(),
                request.osVersion(),
                request.userAgent(),
                request.metadata(),
                requestIp
        );
        terminalBindingValidationService.validateUserTerminalForLogin(user.getId(), user.getUsername(), terminal.getId(), requestIp);
        PolicyResolveResponse policy = policyExecutionService.ensurePolicyAllowsLogin(user.getId(), terminal.getId(), terminal.getClientType(), sceneType);
        BiometricAuthRecord dynamicBiometricRecord = ensureDynamicBiometricSatisfied(policy, request.dynamicBiometricAuthRecordId(), user);
        IotDeviceStatus iotDeviceStatus = ensureIotDeviceSatisfied(policy, terminal.getId());
        BiometricAuthRecord localRecord = insertLocalBiometricRecord(user.getId(), biometricType, source);

        String token = issueUserToken(user, terminal, sceneType);
        auditLogService.recordUserLoginSuccess(user.getId(), user.getUsername(), sceneType, requestIp, loginAuditDetail(terminal, dynamicBiometricRecord, iotDeviceStatus, localRecord));
        return new UserLoginResponse(token, "Bearer", user.getId(), user.getUsername(), user.getRealName(), sceneType, terminal.getId(), terminal.getTerminalCode(), terminal.getClientType());
    }

    private TerminalInfo resolveTerminal(String terminalCode, String deviceFingerprint, String clientType, String clientVersion, String osName, String osVersion, String userAgent, String metadata, String requestIp) {
        return terminalInfoService.resolveOrRegister(new TerminalRegisterRequest(
                terminalCode,
                deviceFingerprint,
                clientType,
                clientVersion,
                osName,
                osVersion,
                userAgent,
                metadata
        ), requestIp);
    }

    private String issueUserToken(UserAccount user, TerminalInfo terminal, String authMethod) {
        return jwtTokenProvider.generateToken(String.valueOf(user.getId()), Map.of(
                "principalType", "USER",
                "username", user.getUsername(),
                "realName", user.getRealName(),
                "authMethod", authMethod,
                "terminalId", terminal.getId(),
                "terminalCode", terminal.getTerminalCode(),
                "clientType", terminal.getClientType()
        ));
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

    private BiometricAuthRecord insertLocalBiometricRecord(Long userId, String biometricType, String source) {
        BiometricAuthRecord record = new BiometricAuthRecord();
        record.setUserId(userId);
        record.setBiometricType(biometricType);
        record.setRequestId(UUID.randomUUID().toString());
        record.setAuthResult(BiometricTemplateService.SUCCESS);
        record.setModelSource(source);
        record.setRawResponse(biometricTemplateService.toJson(Map.of(
                "source", source,
                "verified", true
        )));
        biometricAuthRecordMapper.insert(record);
        return record;
    }

    private BiometricAuthRecord insertVoiceprintRecord(Long userId, BiometricTemplate template, String requestId, java.math.BigDecimal score, java.math.BigDecimal threshold, String authResult, String failReason, String modelSource, Map<String, Object> rawResponse) {
        BiometricAuthRecord record = new BiometricAuthRecord();
        record.setUserId(userId);
        record.setBiometricType(BiometricTemplateService.VOICEPRINT);
        record.setTemplateId(template.getId());
        record.setRequestId(requestId);
        record.setScore(score);
        record.setThreshold(threshold);
        record.setAuthResult(authResult);
        record.setFailReason(failReason);
        record.setModelSource(modelSource);
        record.setRawResponse(biometricTemplateService.toJson(rawResponse));
        biometricAuthRecordMapper.insert(record);
        return record;
    }

    private Map<String, Object> loginAuditDetail(TerminalInfo terminal, BiometricAuthRecord dynamicBiometricRecord, IotDeviceStatus iotDeviceStatus, BiometricAuthRecord biometricRecord) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("terminalId", terminal.getId());
        detail.put("terminalCode", terminal.getTerminalCode());
        if (biometricRecord != null) {
            detail.put("biometricAuthRecordId", biometricRecord.getId());
            detail.put("biometricType", biometricRecord.getBiometricType());
        }
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
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户不存在或账号不可用");
        }
        if (!"ENABLED".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "用户账号未审核通过或不可用");
        }
        return user;
    }
}
