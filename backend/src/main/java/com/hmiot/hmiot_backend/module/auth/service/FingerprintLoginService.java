package com.hmiot.hmiot_backend.module.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.common.security.JwtTokenProvider;
import com.hmiot.hmiot_backend.module.audit.service.AuditLogService;
import com.hmiot.hmiot_backend.module.auth.dto.FingerprintChallengeRequest;
import com.hmiot.hmiot_backend.module.auth.dto.FingerprintLoginRequest;
import com.hmiot.hmiot_backend.module.auth.entity.AuthSession;
import com.hmiot.hmiot_backend.module.auth.mapper.AuthSessionMapper;
import com.hmiot.hmiot_backend.module.auth.vo.FingerprintChallengeResponse;
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
import com.hmiot.hmiot_backend.module.policy.service.PolicyExecutionService;
import com.hmiot.hmiot_backend.module.policy.vo.PolicyResolveResponse;
import com.hmiot.hmiot_backend.module.user.entity.UserAccount;
import com.hmiot.hmiot_backend.module.user.mapper.UserAccountMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class FingerprintLoginService {
    private static final String SCENE_FINGERPRINT_LOGIN = "FINGERPRINT_LOGIN";
    private static final String INIT = "INIT";
    private static final String SUCCESS = "SUCCESS";
    private static final String FAIL = "FAIL";
    private static final String CHALLENGE_REQUESTED = "CHALLENGE_REQUESTED";
    private static final String VERIFIED = "VERIFIED";
    private static final long DYNAMIC_BIOMETRIC_AUTH_VALID_SECONDS = 300;
    private static final int EXPIRES_SECONDS = 120;
    private static final String DEFAULT_ALGORITHM = "SHA256withECDSA";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserAccountMapper userAccountMapper;
    private final AuthSessionMapper authSessionMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditLogService auditLogService;
    private final TerminalInfoService terminalInfoService;
    private final TerminalBindingValidationService terminalBindingValidationService;
    private final PolicyExecutionService policyExecutionService;
    private final BiometricTemplateService biometricTemplateService;
    private final BiometricAuthRecordMapper biometricAuthRecordMapper;
    private final IotDeviceStatusService iotDeviceStatusService;
    private final ObjectMapper objectMapper;

    public FingerprintLoginService(
            UserAccountMapper userAccountMapper,
            AuthSessionMapper authSessionMapper,
            JwtTokenProvider jwtTokenProvider,
            AuditLogService auditLogService,
            TerminalInfoService terminalInfoService,
            TerminalBindingValidationService terminalBindingValidationService,
            PolicyExecutionService policyExecutionService,
            BiometricTemplateService biometricTemplateService,
            BiometricAuthRecordMapper biometricAuthRecordMapper,
            IotDeviceStatusService iotDeviceStatusService,
            ObjectMapper objectMapper
    ) {
        this.userAccountMapper = userAccountMapper;
        this.authSessionMapper = authSessionMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.auditLogService = auditLogService;
        this.terminalInfoService = terminalInfoService;
        this.terminalBindingValidationService = terminalBindingValidationService;
        this.policyExecutionService = policyExecutionService;
        this.biometricTemplateService = biometricTemplateService;
        this.biometricAuthRecordMapper = biometricAuthRecordMapper;
        this.iotDeviceStatusService = iotDeviceStatusService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public FingerprintChallengeResponse requestChallenge(FingerprintChallengeRequest request, String requestIp) {
        UserAccount user = findEnabledUser(request.username());
        BiometricTemplate template = biometricTemplateService.approvedActiveFingerprintByKeyId(user.getId(), request.keyId());
        TerminalInfo terminal = resolveTerminal(request, requestIp);
        terminalBindingValidationService.validateUserTerminalForLogin(user.getId(), user.getUsername(), terminal.getId(), requestIp);

        String challenge = randomChallenge();
        String challengeId = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(EXPIRES_SECONDS);

        AuthSession session = new AuthSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setUserId(user.getId());
        session.setTerminalId(terminal.getId());
        session.setSceneType(SCENE_FINGERPRINT_LOGIN);
        session.setStatus(INIT);
        session.setCurrentStep(CHALLENGE_REQUESTED);
        session.setAuthContext(toJson(Map.of(
                "challengeId", challengeId,
                "challenge", challenge,
                "keyId", request.keyId().trim(),
                "templateId", template.getId(),
                "terminalCode", terminal.getTerminalCode()
        )));
        session.setStartedAt(LocalDateTime.now());
        session.setExpiresAt(expiresAt);
        authSessionMapper.insert(session);

        return new FingerprintChallengeResponse(
                session.getSessionId(),
                challengeId,
                challenge,
                EXPIRES_SECONDS,
                expiresAt,
                request.keyId().trim()
        );
    }

    @Transactional
    public UserLoginResponse login(FingerprintLoginRequest request, String requestIp) {
        UserAccount user = findEnabledUser(request.username());
        AuthSession session = findSession(request.sessionId());
        try {
            validateSession(session, user.getId(), request.challengeId(), request.keyId(), request.terminalCode());
            TerminalInfo terminal = terminalInfoService.findEnabledTerminal(session.getTerminalId());
            terminalBindingValidationService.validateUserTerminalForLogin(user.getId(), user.getUsername(), terminal.getId(), requestIp);
            PolicyResolveResponse policy = policyExecutionService.ensurePolicyAllowsLogin(user.getId(), terminal.getId(), terminal.getClientType(), SCENE_FINGERPRINT_LOGIN);
            BiometricAuthRecord dynamicBiometricRecord = ensureDynamicBiometricSatisfied(policy, request.dynamicBiometricAuthRecordId(), user);
            IotDeviceStatus iotDeviceStatus = ensureIotDeviceSatisfied(policy, terminal.getId());

            BiometricTemplate template = biometricTemplateService.approvedActiveFingerprintByKeyId(user.getId(), request.keyId());
            String publicKeyBase64 = templatePublicKey(template);
            String algorithm = normalizeAlgorithm(request.signAlgorithm());
            boolean verified = verifySignature(publicKeyBase64, challenge(session), request.signatureBase64(), algorithm);
            String failReason = verified ? null : "指纹签名验证失败";
            BiometricAuthRecord fingerprintRecord = insertFingerprintRecord(user.getId(), template, verified, failReason, algorithm);
            if (!verified) {
                failSession(session, failReason);
                auditLogService.recordUserLoginFailure(user.getId(), user.getUsername(), SCENE_FINGERPRINT_LOGIN, requestIp, failReason, Map.of(
                        "terminalId", terminal.getId(),
                        "terminalCode", terminal.getTerminalCode(),
                        "fingerprintAuthRecordId", fingerprintRecord.getId()
                ));
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "指纹认证未通过");
            }

            session.setStatus(SUCCESS);
            session.setCurrentStep(VERIFIED);
            session.setCompletedAt(LocalDateTime.now());
            authSessionMapper.updateById(session);

            String token = jwtTokenProvider.generateToken(String.valueOf(user.getId()), Map.of(
                    "principalType", "USER",
                    "username", user.getUsername(),
                    "realName", user.getRealName(),
                    "authMethod", SCENE_FINGERPRINT_LOGIN,
                    "terminalId", terminal.getId(),
                    "terminalCode", terminal.getTerminalCode(),
                    "clientType", terminal.getClientType()
            ));
            auditLogService.recordUserLoginSuccess(user.getId(), user.getUsername(), SCENE_FINGERPRINT_LOGIN, requestIp, loginAuditDetail(terminal, dynamicBiometricRecord, iotDeviceStatus, fingerprintRecord));
            return new UserLoginResponse(token, "Bearer", user.getId(), user.getUsername(), user.getRealName(), SCENE_FINGERPRINT_LOGIN, terminal.getId(), terminal.getTerminalCode(), terminal.getClientType());
        } catch (BusinessException exception) {
            if (!FAIL.equals(session.getStatus()) && !SUCCESS.equals(session.getStatus())) {
                failSession(session, exception.getMessage());
            }
            throw exception;
        }
    }

    private TerminalInfo resolveTerminal(FingerprintChallengeRequest request, String requestIp) {
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

    private void validateSession(AuthSession session, Long userId, String challengeId, String keyId, String terminalCode) {
        if (!userId.equals(session.getUserId()) || !SCENE_FINGERPRINT_LOGIN.equals(session.getSceneType())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "指纹登录会话不匹配");
        }
        if (session.getExpiresAt() == null || LocalDateTime.now().isAfter(session.getExpiresAt())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "指纹登录挑战已过期");
        }
        if (!INIT.equals(session.getStatus()) || !CHALLENGE_REQUESTED.equals(session.getCurrentStep())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "指纹登录会话状态无效");
        }
        Map<String, Object> context = authContext(session);
        if (!challengeId.equals(String.valueOf(context.get("challengeId")))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "challengeId不匹配");
        }
        if (!keyId.trim().equals(String.valueOf(context.get("keyId")))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "keyId不匹配");
        }
        Object expectedTerminalCode = context.get("terminalCode");
        if (StringUtils.hasText(terminalCode) && expectedTerminalCode != null && !terminalCode.trim().equals(String.valueOf(expectedTerminalCode))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "terminalCode不匹配");
        }
    }

    private String challenge(AuthSession session) {
        Object value = authContext(session).get("challenge");
        if (!StringUtils.hasText(value == null ? null : String.valueOf(value))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "challenge无效");
        }
        return String.valueOf(value);
    }

    private Map<String, Object> authContext(AuthSession session) {
        if (!StringUtils.hasText(session.getAuthContext())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录会话上下文无效");
        }
        try {
            return objectMapper.readValue(session.getAuthContext(), new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录会话上下文无效");
        }
    }

    private String templatePublicKey(BiometricTemplate template) {
        if (!StringUtils.hasText(template.getMetadata())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "指纹模板缺少公钥");
        }
        try {
            Map<String, Object> metadata = objectMapper.readValue(template.getMetadata(), new TypeReference<>() {
            });
            Object publicKey = metadata.get("publicKeyBase64");
            if (!StringUtils.hasText(publicKey == null ? null : String.valueOf(publicKey))) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "指纹模板缺少公钥");
            }
            return String.valueOf(publicKey);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "指纹模板元数据无效");
        }
    }

    private String normalizeAlgorithm(String signAlgorithm) {
        if (!StringUtils.hasText(signAlgorithm)) {
            return DEFAULT_ALGORITHM;
        }
        return signAlgorithm.trim();
    }

    private boolean verifySignature(String publicKeyBase64, String challenge, String signatureBase64, String algorithm) {
        try {
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64.trim());
            PublicKey publicKey = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
            Signature signature = Signature.getInstance(algorithm);
            signature.initVerify(publicKey);
            signature.update(challenge.getBytes(StandardCharsets.UTF_8));
            byte[] signBytes = Base64.getDecoder().decode(signatureBase64.trim());
            return signature.verify(signBytes);
        } catch (Exception exception) {
            return false;
        }
    }

    private BiometricAuthRecord insertFingerprintRecord(Long userId, BiometricTemplate template, boolean success, String failReason, String algorithm) {
        BiometricAuthRecord record = new BiometricAuthRecord();
        record.setUserId(userId);
        record.setBiometricType(BiometricTemplateService.FINGERPRINT);
        record.setTemplateId(template.getId());
        record.setRequestId(UUID.randomUUID().toString());
        record.setScore(success ? new BigDecimal("1.000000") : new BigDecimal("0.000000"));
        record.setThreshold(new BigDecimal("1.000000"));
        record.setAuthResult(success ? BiometricTemplateService.SUCCESS : FAIL);
        record.setFailReason(failReason);
        record.setModelSource("ANDROID_BIOMETRIC_LOCAL");
        record.setRawResponse(biometricTemplateService.toJson(Map.of(
                "verified", success,
                "algorithm", algorithm,
                "keyId", template.getTemplateRef() == null ? "" : template.getTemplateRef()
        )));
        biometricAuthRecordMapper.insert(record);
        return record;
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

    private Map<String, Object> loginAuditDetail(TerminalInfo terminal, BiometricAuthRecord dynamicBiometricRecord, IotDeviceStatus iotDeviceStatus, BiometricAuthRecord biometricRecord) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("terminalId", terminal.getId());
        detail.put("terminalCode", terminal.getTerminalCode());
        detail.put("biometricAuthRecordId", biometricRecord.getId());
        detail.put("biometricType", biometricRecord.getBiometricType());
        if (dynamicBiometricRecord != null) {
            detail.put("dynamicBiometricAuthRecordId", dynamicBiometricRecord.getId());
            detail.put("dynamicBiometricAuthAt", dynamicBiometricRecord.getCreatedAt());
        }
        detail.putAll(iotDeviceStatusService.safeSummary(iotDeviceStatus));
        return detail;
    }

    private void failSession(AuthSession session, String reason) {
        session.setStatus(FAIL);
        session.setFailReason(reason);
        session.setCompletedAt(LocalDateTime.now());
        authSessionMapper.updateById(session);
    }

    private AuthSession findSession(String sessionId) {
        AuthSession session = authSessionMapper.selectOne(new LambdaQueryWrapper<AuthSession>().eq(AuthSession::getSessionId, sessionId));
        if (session == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录会话不存在");
        }
        return session;
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

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "登录会话序列化失败");
        }
    }

    private String randomChallenge() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
