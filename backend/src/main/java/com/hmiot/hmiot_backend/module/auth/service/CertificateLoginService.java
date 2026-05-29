package com.hmiot.hmiot_backend.module.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.common.security.JwtTokenProvider;
import com.hmiot.hmiot_backend.module.auth.dto.CertificateChallengeRequest;
import com.hmiot.hmiot_backend.module.auth.dto.CertificateVerifyRequest;
import com.hmiot.hmiot_backend.module.auth.entity.AuthSession;
import com.hmiot.hmiot_backend.module.auth.mapper.AuthSessionMapper;
import com.hmiot.hmiot_backend.module.auth.vo.CertificateChallengeResponse;
import com.hmiot.hmiot_backend.module.auth.vo.UserLoginResponse;
import com.hmiot.hmiot_backend.module.audit.service.AuditLogService;
import com.hmiot.hmiot_backend.module.biometric.entity.BiometricAuthRecord;
import com.hmiot.hmiot_backend.module.biometric.service.BiometricTemplateService;
import com.hmiot.hmiot_backend.module.device.entity.IotDeviceStatus;
import com.hmiot.hmiot_backend.module.device.service.IotDeviceStatusService;
import com.hmiot.hmiot_backend.module.binding.service.TerminalBindingValidationService;
import com.hmiot.hmiot_backend.module.certificate.service.CertificateService;
import com.hmiot.hmiot_backend.module.device.dto.TerminalRegisterRequest;
import com.hmiot.hmiot_backend.module.device.entity.TerminalInfo;
import com.hmiot.hmiot_backend.module.device.service.TerminalInfoService;
import com.hmiot.hmiot_backend.module.integration.service.CertificateIntegrationClient;
import com.hmiot.hmiot_backend.module.integration.service.CertificateIntegrationClient.ChallengeResult;
import com.hmiot.hmiot_backend.module.integration.service.CertificateIntegrationClient.VerifyResult;
import com.hmiot.hmiot_backend.module.integration.service.ExternalIntegrationCallService;
import com.hmiot.hmiot_backend.module.policy.service.PolicyExecutionService;
import com.hmiot.hmiot_backend.module.policy.vo.PolicyResolveResponse;
import com.hmiot.hmiot_backend.module.user.entity.UserAccount;
import com.hmiot.hmiot_backend.module.user.mapper.UserAccountMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class CertificateLoginService {
    private static final String INIT = "INIT";
    private static final String SUCCESS = "SUCCESS";
    private static final String FAIL = "FAIL";
    private static final String CHALLENGE_REQUESTED = "CHALLENGE_REQUESTED";
    private static final String VERIFIED = "VERIFIED";
    private static final long DYNAMIC_BIOMETRIC_AUTH_VALID_SECONDS = 300;

    private final UserAccountMapper userAccountMapper;
    private final AuthSessionMapper authSessionMapper;
    private final CertificateService certificateService;
    private final CertificateIntegrationClient certificateIntegrationClient;
    private final ExternalIntegrationCallService externalIntegrationCallService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditLogService auditLogService;
    private final TerminalInfoService terminalInfoService;
    private final TerminalBindingValidationService terminalBindingValidationService;
    private final PolicyExecutionService policyExecutionService;
    private final BiometricTemplateService biometricTemplateService;
    private final IotDeviceStatusService iotDeviceStatusService;
    private final ObjectMapper objectMapper;

    public CertificateLoginService(UserAccountMapper userAccountMapper, AuthSessionMapper authSessionMapper, CertificateService certificateService, CertificateIntegrationClient certificateIntegrationClient, ExternalIntegrationCallService externalIntegrationCallService, JwtTokenProvider jwtTokenProvider, AuditLogService auditLogService, TerminalInfoService terminalInfoService, TerminalBindingValidationService terminalBindingValidationService, PolicyExecutionService policyExecutionService, BiometricTemplateService biometricTemplateService, IotDeviceStatusService iotDeviceStatusService, ObjectMapper objectMapper) {
        this.userAccountMapper = userAccountMapper;
        this.authSessionMapper = authSessionMapper;
        this.certificateService = certificateService;
        this.certificateIntegrationClient = certificateIntegrationClient;
        this.externalIntegrationCallService = externalIntegrationCallService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.auditLogService = auditLogService;
        this.terminalInfoService = terminalInfoService;
        this.terminalBindingValidationService = terminalBindingValidationService;
        this.policyExecutionService = policyExecutionService;
        this.biometricTemplateService = biometricTemplateService;
        this.iotDeviceStatusService = iotDeviceStatusService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CertificateChallengeResponse requestChallenge(CertificateChallengeRequest request, String sceneType, String requestIp) {
        UserAccount user = findEnabledUser(request.username());
        if (!certificateService.hasValidCertificate(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "用户无有效证书");
        }
        LocalDateTime startedAt = LocalDateTime.now();
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
        Map<String, Object> requestSummary = Map.of(
                "userId", user.getId(),
                "username", user.getUsername(),
                "sceneType", sceneType,
                "terminalId", terminal.getId(),
                "terminalCode", terminal.getTerminalCode()
        );
        ChallengeResult challenge;
        try {
            challenge = certificateIntegrationClient.requestChallenge(user.getUsername());
        } catch (RuntimeException exception) {
            externalIntegrationCallService.recordFailure("CERTIFICATE_CHALLENGE_REQUEST", certificateIntegrationClient.targetSystem(), String.valueOf(user.getId()), requestSummary, exception.getMessage(), startedAt);
            auditLogService.recordUserLoginFailure(user.getId(), user.getUsername(), sceneType, requestIp, exception.getMessage(), requestSummary);
            throw exception;
        }
        externalIntegrationCallService.recordSuccess("CERTIFICATE_CHALLENGE_REQUEST", certificateIntegrationClient.targetSystem(), String.valueOf(user.getId()), requestSummary, Map.of(
                "challengeId", challenge.challengeId(),
                "expiresInSeconds", challenge.expiresInSeconds()
        ), startedAt);

        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(challenge.expiresInSeconds());
        AuthSession session = new AuthSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setUserId(user.getId());
        session.setTerminalId(terminal.getId());
        session.setSceneType(sceneType);
        session.setStatus(INIT);
        session.setCurrentStep(CHALLENGE_REQUESTED);
        session.setAuthContext(toJson(Map.of(
                "challengeId", challenge.challengeId(),
                "terminalId", terminal.getId(),
                "terminalCode", terminal.getTerminalCode(),
                "clientType", terminal.getClientType() == null ? "" : terminal.getClientType(),
                "targetSystem", certificateIntegrationClient.targetSystem()
        )));
        session.setStartedAt(LocalDateTime.now());
        session.setExpiresAt(expiresAt);
        authSessionMapper.insert(session);
        return new CertificateChallengeResponse(session.getSessionId(), challenge.challengeId(), challenge.challenge(), challenge.expiresInSeconds(), expiresAt);
    }

    @Transactional
    public UserLoginResponse verify(CertificateVerifyRequest request, String sceneType, String requestIp) {
        UserAccount user = findEnabledUser(request.username());
        AuthSession session = findSession(request.sessionId());
        try {
            validateSession(session, user, request.challengeId(), sceneType);
            TerminalInfo terminal = terminalInfoService.findEnabledTerminal(session.getTerminalId());
            if (request.terminalCode() != null && !request.terminalCode().equals(terminal.getTerminalCode())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "认证终端与挑战阶段不一致");
            }
            LocalDateTime startedAt = LocalDateTime.now();
            Map<String, Object> requestSummary = Map.of(
                    "userId", user.getId(),
                    "username", user.getUsername(),
                    "sessionId", session.getSessionId(),
                    "challengeId", request.challengeId(),
                    "sceneType", sceneType,
                    "terminalId", terminal.getId(),
                    "terminalCode", terminal.getTerminalCode()
            );
            VerifyResult result;
            try {
                result = certificateIntegrationClient.verifyChallenge(user.getUsername(), request.challengeId(), request.signatureB64());
            } catch (RuntimeException exception) {
                externalIntegrationCallService.recordFailure("CERTIFICATE_CHALLENGE_VERIFY", certificateIntegrationClient.targetSystem(), String.valueOf(user.getId()), requestSummary, exception.getMessage(), startedAt);
                failSession(session, exception.getMessage());
                auditLogService.recordUserLoginFailure(user.getId(), user.getUsername(), sceneType, requestIp, exception.getMessage(), requestSummary);
                throw exception;
            }
            externalIntegrationCallService.recordSuccess("CERTIFICATE_CHALLENGE_VERIFY", certificateIntegrationClient.targetSystem(), String.valueOf(user.getId()), requestSummary, Map.of(
                    "verified", result.verified(),
                    "serial", result.serial() == null ? "" : result.serial(),
                    "fingerprintSha256", result.fingerprintSha256() == null ? "" : result.fingerprintSha256()
            ), startedAt);

            if (!Boolean.TRUE.equals(result.verified())) {
                failSession(session, "证书签名验证失败");
                auditLogService.recordUserLoginFailure(user.getId(), user.getUsername(), sceneType, requestIp, "证书签名验证失败", requestSummary);
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "证书签名验证失败");
            }
            if (!certificateService.matchesValidCertificate(user.getId(), result.serial(), result.fingerprintSha256())) {
                failSession(session, "证书与用户不匹配或已失效");
                auditLogService.recordUserLoginFailure(user.getId(), user.getUsername(), sceneType, requestIp, "证书与用户不匹配或已失效", requestSummary);
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "证书与用户不匹配或已失效");
            }

            terminalBindingValidationService.validateUserTerminalForLogin(user.getId(), user.getUsername(), terminal.getId(), requestIp);
            PolicyResolveResponse policy = policyExecutionService.ensurePolicyAllowsLogin(user.getId(), terminal.getId(), terminal.getClientType(), sceneType);
            BiometricAuthRecord dynamicBiometricRecord = ensureDynamicBiometricSatisfied(policy, request, user);
            IotDeviceStatus iotDeviceStatus = ensureIotDeviceSatisfied(policy, terminal.getId());

            session.setStatus(SUCCESS);
            session.setCurrentStep(VERIFIED);
            session.setCompletedAt(LocalDateTime.now());
            authSessionMapper.updateById(session);
            String token = jwtTokenProvider.generateToken(String.valueOf(user.getId()), Map.of(
                    "principalType", "USER",
                    "username", user.getUsername(),
                    "realName", user.getRealName(),
                    "authMethod", sceneType,
                    "terminalId", terminal.getId(),
                    "terminalCode", terminal.getTerminalCode(),
                    "clientType", terminal.getClientType()
            ));
            auditLogService.recordUserLoginSuccess(user.getId(), user.getUsername(), sceneType, requestIp, loginAuditDetail(session, request, terminal, dynamicBiometricRecord, iotDeviceStatus));
            return new UserLoginResponse(token, "Bearer", user.getId(), user.getUsername(), user.getRealName(), sceneType, terminal.getId(), terminal.getTerminalCode(), terminal.getClientType());
        } catch (BusinessException exception) {
            if (!FAIL.equals(session.getStatus()) && !SUCCESS.equals(session.getStatus())) {
                failSession(session, exception.getMessage());
            }
            throw exception;
        }
    }

    private BiometricAuthRecord ensureDynamicBiometricSatisfied(PolicyResolveResponse policy, CertificateVerifyRequest request, UserAccount user) {
        if (!policy.authChain().contains(BiometricTemplateService.DYNAMIC_BIOMETRIC)) {
            return null;
        }
        if (request.dynamicBiometricAuthRecordId() == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前登录策略要求完成动态生物认证");
        }
        BiometricAuthRecord record = biometricTemplateService.findSuccessfulDynamicBiometricRecord(request.dynamicBiometricAuthRecordId(), user.getId());
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

    private Map<String, Object> loginAuditDetail(AuthSession session, CertificateVerifyRequest request, TerminalInfo terminal, BiometricAuthRecord dynamicBiometricRecord, IotDeviceStatus iotDeviceStatus) {
        Map<String, Object> detail = new java.util.LinkedHashMap<>();
        detail.put("sessionId", session.getSessionId());
        detail.put("challengeId", request.challengeId());
        detail.put("terminalId", terminal.getId());
        detail.put("terminalCode", terminal.getTerminalCode());
        if (dynamicBiometricRecord != null) {
            detail.put("dynamicBiometricAuthRecordId", dynamicBiometricRecord.getId());
            detail.put("dynamicBiometricAuthAt", dynamicBiometricRecord.getCreatedAt());
        }
        detail.putAll(iotDeviceStatusService.safeSummary(iotDeviceStatus));
        return detail;
    }

    private void validateSession(AuthSession session, UserAccount user, String challengeId, String sceneType) {
        if (!user.getId().equals(session.getUserId()) || !sceneType.equals(session.getSceneType())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "认证会话不匹配");
        }
        if (LocalDateTime.now().isAfter(session.getExpiresAt())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "挑战码已过期");
        }
        if (!INIT.equals(session.getStatus()) || !CHALLENGE_REQUESTED.equals(session.getCurrentStep())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "认证会话状态无效");
        }
        if (!challengeId.equals(authContext(session).get("challengeId"))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "挑战码不匹配");
        }
    }

    private Map<String, Object> authContext(AuthSession session) {
        try {
            return objectMapper.readValue(session.getAuthContext(), new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "认证会话上下文无效");
        }
    }

    private void failSession(AuthSession session, String failReason) {
        session.setStatus(FAIL);
        session.setFailReason(failReason);
        session.setCompletedAt(LocalDateTime.now());
        authSessionMapper.updateById(session);
    }

    private AuthSession findSession(String sessionId) {
        AuthSession session = authSessionMapper.selectOne(new LambdaQueryWrapper<AuthSession>().eq(AuthSession::getSessionId, sessionId));
        if (session == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "认证会话不存在");
        }
        return session;
    }

    private UserAccount findEnabledUser(String username) {
        UserAccount user = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getUsername, username)
                .eq(UserAccount::getDeleted, false));
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或证书错误");
        }
        if (!"ENABLED".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "用户账号不可用");
        }
        return user;
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "认证上下文序列化失败");
        }
    }
}
