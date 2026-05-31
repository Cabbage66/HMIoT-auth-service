package com.hmiot.hmiot_backend.module.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.common.security.JwtTokenProvider;
import com.hmiot.hmiot_backend.module.audit.service.AuditLogService;
import com.hmiot.hmiot_backend.module.auth.dto.FaceLoginRequest;
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
import com.hmiot.hmiot_backend.module.integration.service.FaceIntegrationClient;
import com.hmiot.hmiot_backend.module.integration.service.FaceIntegrationClient.VerifyResult;
import com.hmiot.hmiot_backend.module.policy.service.PolicyExecutionService;
import com.hmiot.hmiot_backend.module.policy.vo.PolicyResolveResponse;
import com.hmiot.hmiot_backend.module.user.entity.UserAccount;
import com.hmiot.hmiot_backend.module.user.mapper.UserAccountMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class FaceLoginService {
    private static final String FACE_LOGIN = "FACE_LOGIN";
    private static final long DYNAMIC_BIOMETRIC_AUTH_VALID_SECONDS = 300;
    private static final String FAIL = "FAIL";

    private final UserAccountMapper userAccountMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final TerminalInfoService terminalInfoService;
    private final TerminalBindingValidationService terminalBindingValidationService;
    private final PolicyExecutionService policyExecutionService;
    private final BiometricTemplateService biometricTemplateService;
    private final BiometricAuthRecordMapper biometricAuthRecordMapper;
    private final IotDeviceStatusService iotDeviceStatusService;
    private final FaceIntegrationClient faceIntegrationClient;
    private final ExternalIntegrationCallService externalIntegrationCallService;
    private final AuditLogService auditLogService;

    public FaceLoginService(
            UserAccountMapper userAccountMapper,
            JwtTokenProvider jwtTokenProvider,
            TerminalInfoService terminalInfoService,
            TerminalBindingValidationService terminalBindingValidationService,
            PolicyExecutionService policyExecutionService,
            BiometricTemplateService biometricTemplateService,
            BiometricAuthRecordMapper biometricAuthRecordMapper,
            IotDeviceStatusService iotDeviceStatusService,
            FaceIntegrationClient faceIntegrationClient,
            ExternalIntegrationCallService externalIntegrationCallService,
            AuditLogService auditLogService
    ) {
        this.userAccountMapper = userAccountMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.terminalInfoService = terminalInfoService;
        this.terminalBindingValidationService = terminalBindingValidationService;
        this.policyExecutionService = policyExecutionService;
        this.biometricTemplateService = biometricTemplateService;
        this.biometricAuthRecordMapper = biometricAuthRecordMapper;
        this.iotDeviceStatusService = iotDeviceStatusService;
        this.faceIntegrationClient = faceIntegrationClient;
        this.externalIntegrationCallService = externalIntegrationCallService;
        this.auditLogService = auditLogService;
    }

    public UserLoginResponse login(FaceLoginRequest request, String requestIp) {
        UserAccount user = findEnabledUser(request.username());
        TerminalInfo terminal = resolveTerminal(request, requestIp);
        terminalBindingValidationService.validateUserTerminalForLogin(user.getId(), user.getUsername(), terminal.getId(), requestIp);
        PolicyResolveResponse policy = policyExecutionService.ensurePolicyAllowsLogin(user.getId(), terminal.getId(), terminal.getClientType(), FACE_LOGIN);
        BiometricAuthRecord dynamicBiometricRecord = ensureDynamicBiometricSatisfied(policy, request.dynamicBiometricAuthRecordId(), user);
        IotDeviceStatus iotDeviceStatus = ensureIotDeviceSatisfied(policy, terminal.getId());

        BiometricTemplate template = biometricTemplateService.latestApprovedFace(user.getId());
        LocalDateTime startedAt = LocalDateTime.now();
        Map<String, Object> requestSummary = Map.of(
                "userId", user.getId(),
                "username", user.getUsername(),
                "terminalId", terminal.getId(),
                "terminalCode", terminal.getTerminalCode(),
                "templateId", template.getId(),
                "imageFormat", normalizeImageFormat(request.imageFormat())
        );

        VerifyResult verifyResult;
        try {
            verifyResult = faceIntegrationClient.verify(
                    template.getTemplateRef(),
                    template.getTemplateDigest(),
                    request.imageBase64(),
                    normalizeImageFormat(request.imageFormat()),
                    template.getThreshold(),
                    Map.of(
                            "scene", FACE_LOGIN,
                            "imageFormat", normalizeImageFormat(request.imageFormat())
                    )
            );
        } catch (RuntimeException exception) {
            BiometricAuthRecord failedRecord = insertFaceRecord(user.getId(), template, null, null, template.getThreshold(), FAIL, exception.getMessage(), faceIntegrationClient.targetSystem(), Map.of("error", exception.getMessage()));
            externalIntegrationCallService.recordFailure("FACE_LOGIN_VERIFY", faceIntegrationClient.targetSystem(), String.valueOf(user.getId()), requestSummary, exception.getMessage(), startedAt);
            auditLogService.recordUserLoginFailure(user.getId(), user.getUsername(), FACE_LOGIN, requestIp, exception.getMessage(), Map.of(
                    "terminalId", terminal.getId(),
                    "terminalCode", terminal.getTerminalCode(),
                    "faceAuthRecordId", failedRecord.getId()
            ));
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "人脸认证失败");
        }

        String authResult = verifyResult.verified() ? BiometricTemplateService.SUCCESS : FAIL;
        String failReason = verifyResult.verified() ? null : (verifyResult.failReason() == null ? "人脸相似度低于阈值" : verifyResult.failReason());
        BiometricAuthRecord faceRecord = insertFaceRecord(user.getId(), template, verifyResult.requestId(), verifyResult.score(), verifyResult.threshold(), authResult, failReason, verifyResult.modelSource(), verifyResult.rawResponse());
        externalIntegrationCallService.recordSuccess("FACE_LOGIN_VERIFY", faceIntegrationClient.targetSystem(), String.valueOf(user.getId()), requestSummary, Map.of(
                "recordId", faceRecord.getId(),
                "verified", verifyResult.verified(),
                "score", verifyResult.score(),
                "threshold", verifyResult.threshold(),
                "failReason", failReason == null ? "" : failReason
        ), startedAt);
        if (!verifyResult.verified()) {
            auditLogService.recordUserLoginFailure(user.getId(), user.getUsername(), FACE_LOGIN, requestIp, failReason, Map.of(
                    "terminalId", terminal.getId(),
                    "terminalCode", terminal.getTerminalCode(),
                    "faceAuthRecordId", faceRecord.getId()
            ));
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "人脸认证未通过");
        }

        String token = issueUserToken(user, terminal, FACE_LOGIN);
        auditLogService.recordUserLoginSuccess(user.getId(), user.getUsername(), FACE_LOGIN, requestIp, loginAuditDetail(terminal, dynamicBiometricRecord, iotDeviceStatus, faceRecord));
        return new UserLoginResponse(token, "Bearer", user.getId(), user.getUsername(), user.getRealName(), FACE_LOGIN, terminal.getId(), terminal.getTerminalCode(), terminal.getClientType());
    }

    private TerminalInfo resolveTerminal(FaceLoginRequest request, String requestIp) {
        return terminalInfoService.resolveOrRegister(new TerminalRegisterRequest(
                request.terminalCode(),
                request.deviceFingerprint(),
                request.clientType(),
                request.clientVersion(),
                request.osName(),
                request.osVersion(),
                request.userAgent(),
                request.metadata()
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

    private BiometricAuthRecord insertFaceRecord(Long userId, BiometricTemplate template, String requestId, java.math.BigDecimal score, java.math.BigDecimal threshold, String authResult, String failReason, String modelSource, Map<String, Object> rawResponse) {
        BiometricAuthRecord record = new BiometricAuthRecord();
        record.setUserId(userId);
        record.setBiometricType(BiometricTemplateService.FACE);
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

    private String normalizeImageFormat(String imageFormat) {
        if (!StringUtils.hasText(imageFormat)) {
            return "jpeg";
        }
        return imageFormat.trim().toLowerCase();
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

