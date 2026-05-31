package com.hmiot.hmiot_backend.module.biometric.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.common.security.CurrentUser;
import com.hmiot.hmiot_backend.module.auth.entity.AuthSession;
import com.hmiot.hmiot_backend.module.auth.mapper.AuthSessionMapper;
import com.hmiot.hmiot_backend.module.biometric.dto.FingerprintChallengeRequest;
import com.hmiot.hmiot_backend.module.biometric.dto.FingerprintSubmitRequest;
import com.hmiot.hmiot_backend.module.biometric.dto.FingerprintVerifyRequest;
import com.hmiot.hmiot_backend.module.biometric.entity.BiometricAuthRecord;
import com.hmiot.hmiot_backend.module.biometric.entity.BiometricTemplate;
import com.hmiot.hmiot_backend.module.biometric.mapper.BiometricAuthRecordMapper;
import com.hmiot.hmiot_backend.module.biometric.mapper.BiometricTemplateMapper;
import com.hmiot.hmiot_backend.module.biometric.vo.FingerprintChallengeResponse;
import com.hmiot.hmiot_backend.module.biometric.vo.FingerprintHealthResponse;
import com.hmiot.hmiot_backend.module.biometric.vo.FingerprintVerifyResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
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
public class FingerprintAuthService {
    private static final String SCENE_FINGERPRINT_ENROLL = "FINGERPRINT_ENROLL";
    private static final String SCENE_FINGERPRINT_RECOLLECT = "FINGERPRINT_RECOLLECT";
    private static final String SCENE_FINGERPRINT_VERIFY = "FINGERPRINT_VERIFY";

    private static final String INIT = "INIT";
    private static final String SUCCESS = "SUCCESS";
    private static final String FAIL = "FAIL";
    private static final String CHALLENGE_REQUESTED = "CHALLENGE_REQUESTED";
    private static final int EXPIRES_SECONDS = 120;
    private static final String DEFAULT_ALGORITHM = "SHA256withECDSA";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AuthSessionMapper authSessionMapper;
    private final BiometricTemplateMapper biometricTemplateMapper;
    private final BiometricAuthRecordMapper biometricAuthRecordMapper;
    private final BiometricTemplateService biometricTemplateService;
    private final ObjectMapper objectMapper;

    public FingerprintAuthService(
            AuthSessionMapper authSessionMapper,
            BiometricTemplateMapper biometricTemplateMapper,
            BiometricAuthRecordMapper biometricAuthRecordMapper,
            BiometricTemplateService biometricTemplateService,
            ObjectMapper objectMapper
    ) {
        this.authSessionMapper = authSessionMapper;
        this.biometricTemplateMapper = biometricTemplateMapper;
        this.biometricAuthRecordMapper = biometricAuthRecordMapper;
        this.biometricTemplateService = biometricTemplateService;
        this.objectMapper = objectMapper;
    }

    public FingerprintHealthResponse health() {
        return new FingerprintHealthResponse(true, "ANDROID_BIOMETRIC_LOCAL", "keystore-sign-v1");
    }

    @Transactional
    public FingerprintChallengeResponse requestChallenge(CurrentUser currentUser, FingerprintChallengeRequest request) {
        String purpose = normalizePurpose(request == null ? null : request.purpose());
        String sceneType = sceneForPurpose(purpose);
        String keyId = request == null ? null : trimToNull(request.keyId());

        if ("VERIFY".equals(purpose) && !StringUtils.hasText(keyId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "指纹验证需提供keyId");
        }

        String challenge = randomChallenge();
        String challengeId = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(EXPIRES_SECONDS);

        AuthSession session = new AuthSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setUserId(currentUser.userId());
        session.setSceneType(sceneType);
        session.setStatus(INIT);
        session.setCurrentStep(CHALLENGE_REQUESTED);
        session.setAuthContext(toJson(Map.of(
                "challengeId", challengeId,
                "challenge", challenge,
                "purpose", purpose,
                "keyId", keyId == null ? "" : keyId
        )));
        session.setStartedAt(LocalDateTime.now());
        session.setExpiresAt(expiresAt);
        authSessionMapper.insert(session);
        return new FingerprintChallengeResponse(session.getSessionId(), challengeId, challenge, EXPIRES_SECONDS, expiresAt, keyId);
    }

    @Transactional
    public BiometricTemplate enroll(CurrentUser currentUser, FingerprintSubmitRequest request) {
        return submitTemplate(currentUser, request, false);
    }

    @Transactional
    public BiometricTemplate recollect(CurrentUser currentUser, FingerprintSubmitRequest request) {
        return submitTemplate(currentUser, request, true);
    }

    @Transactional
    public FingerprintVerifyResponse verify(CurrentUser currentUser, FingerprintVerifyRequest request) {
        AuthSession session = findSession(request.sessionId());
        validateSession(session, currentUser.userId(), request.challengeId(), SCENE_FINGERPRINT_VERIFY, request.keyId());
        BiometricTemplate template = biometricTemplateService.approvedActiveFingerprintByKeyId(currentUser.userId(), request.keyId());

        String algorithm = normalizeAlgorithm(request.signAlgorithm());
        String publicKeyBase64 = templatePublicKey(template);
        boolean verified = verifySignature(publicKeyBase64, challenge(session), request.signatureBase64(), algorithm);
        String failReason = verified ? null : "指纹签名验证失败";
        BiometricAuthRecord record = insertRecord(currentUser.userId(), template, verified, failReason, algorithm, request.metadata());

        if (verified) {
            session.setStatus(SUCCESS);
        } else {
            session.setStatus(FAIL);
            session.setFailReason(failReason);
        }
        session.setCompletedAt(LocalDateTime.now());
        authSessionMapper.updateById(session);
        return new FingerprintVerifyResponse(record.getId(), verified, failReason);
    }

    public BiometricTemplate submitTemplate(CurrentUser currentUser, FingerprintSubmitRequest request, boolean recollect) {
        AuthSession session = findSession(request.sessionId());
        validateSession(
                session,
                currentUser.userId(),
                request.challengeId(),
                recollect ? SCENE_FINGERPRINT_RECOLLECT : SCENE_FINGERPRINT_ENROLL,
                request.keyId()
        );
        String algorithm = normalizeAlgorithm(request.signAlgorithm());
        String keyId = request.keyId().trim();
        String publicKeyBase64 = request.publicKeyBase64().trim();

        boolean verified = verifySignature(publicKeyBase64, challenge(session), request.signatureBase64(), algorithm);
        if (!verified) {
            session.setStatus(FAIL);
            session.setFailReason("指纹签名验证失败");
            session.setCompletedAt(LocalDateTime.now());
            authSessionMapper.updateById(session);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "指纹签名验证失败");
        }

        if (recollect) {
            biometricTemplateService.replaceActivePendingFingerprints(currentUser.userId());
        }

        BiometricTemplate template = new BiometricTemplate();
        template.setUserId(currentUser.userId());
        template.setBiometricType(BiometricTemplateService.FINGERPRINT);
        template.setTemplateRef(keyId);
        template.setTemplateDigest(sha256(publicKeyBase64));
        template.setModelSource("ANDROID_BIOMETRIC_LOCAL");
        template.setModelVersion("keystore-sign-v1");
        template.setThreshold(new BigDecimal("1.000000"));
        template.setAuditStatus(BiometricTemplateService.PENDING);
        template.setTemplateStatus(BiometricTemplateService.ACTIVE);
        template.setVersionNo(biometricTemplateService.nextFingerprintVersion(currentUser.userId()));
        template.setSubmittedAt(LocalDateTime.now());
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (request.metadata() != null) {
            metadata.putAll(request.metadata());
        }
        metadata.put("keyId", keyId);
        metadata.put("publicKeyBase64", publicKeyBase64);
        metadata.put("signAlgorithm", algorithm);
        metadata.put("recollect", recollect);
        template.setMetadata(biometricTemplateService.toJson(metadata));
        biometricTemplateMapper.insert(template);

        session.setStatus(SUCCESS);
        session.setCompletedAt(LocalDateTime.now());
        authSessionMapper.updateById(session);
        return template;
    }

    private BiometricAuthRecord insertRecord(Long userId, BiometricTemplate template, boolean success, String failReason, String algorithm, Map<String, Object> metadata) {
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
                "keyId", template.getTemplateRef() == null ? "" : template.getTemplateRef(),
                "metadata", metadata == null ? Map.of() : metadata
        )));
        biometricAuthRecordMapper.insert(record);
        return record;
    }

    private void validateSession(AuthSession session, Long userId, String challengeId, String expectedScene, String keyId) {
        if (!userId.equals(session.getUserId()) || !expectedScene.equals(session.getSceneType())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "指纹挑战会话不匹配");
        }
        if (session.getExpiresAt() == null || LocalDateTime.now().isAfter(session.getExpiresAt())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "指纹挑战已过期");
        }
        if (!INIT.equals(session.getStatus()) || !CHALLENGE_REQUESTED.equals(session.getCurrentStep())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "指纹挑战会话状态无效");
        }
        Map<String, Object> context = authContext(session);
        if (!challengeId.equals(String.valueOf(context.get("challengeId")))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "challengeId不匹配");
        }
        Object expectedKeyId = context.get("keyId");
        if (expectedKeyId != null && StringUtils.hasText(String.valueOf(expectedKeyId)) && !String.valueOf(expectedKeyId).equals(keyId == null ? "" : keyId.trim())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "keyId不匹配");
        }
    }

    private String challenge(AuthSession session) {
        Object value = authContext(session).get("challenge");
        if (!StringUtils.hasText(value == null ? null : String.valueOf(value))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "会话challenge无效");
        }
        return String.valueOf(value);
    }

    private AuthSession findSession(String sessionId) {
        AuthSession session = authSessionMapper.selectOne(new LambdaQueryWrapper<AuthSession>().eq(AuthSession::getSessionId, sessionId));
        if (session == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "挑战会话不存在");
        }
        return session;
    }

    private String templatePublicKey(BiometricTemplate template) {
        Map<String, Object> metadata = parseMetadata(template.getMetadata());
        Object value = metadata.get("publicKeyBase64");
        if (!StringUtils.hasText(value == null ? null : String.valueOf(value))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "指纹模板公钥缺失");
        }
        return String.valueOf(value);
    }

    private Map<String, Object> parseMetadata(String metadata) {
        if (!StringUtils.hasText(metadata)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(metadata, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "指纹模板元数据无效");
        }
    }

    private Map<String, Object> authContext(AuthSession session) {
        if (!StringUtils.hasText(session.getAuthContext())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "指纹挑战上下文无效");
        }
        try {
            return objectMapper.readValue(session.getAuthContext(), new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "指纹挑战上下文无效");
        }
    }

    private String normalizePurpose(String purpose) {
        if (!StringUtils.hasText(purpose)) {
            return "ENROLL";
        }
        String value = purpose.trim().toUpperCase();
        if (!"ENROLL".equals(value) && !"RECOLLECT".equals(value) && !"VERIFY".equals(value)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "指纹challenge用途无效");
        }
        return value;
    }

    private String sceneForPurpose(String purpose) {
        return switch (purpose) {
            case "RECOLLECT" -> SCENE_FINGERPRINT_RECOLLECT;
            case "VERIFY" -> SCENE_FINGERPRINT_VERIFY;
            default -> SCENE_FINGERPRINT_ENROLL;
        };
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

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] data = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(data);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "指纹模板摘要计算失败");
        }
    }

    private String randomChallenge() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "指纹挑战上下文序列化失败");
        }
    }
}
