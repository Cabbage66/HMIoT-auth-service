package com.hmiot.hmiot_backend.module.biometric.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.common.security.CurrentUser;
import com.hmiot.hmiot_backend.module.biometric.dto.VoiceprintEnrollRequest;
import com.hmiot.hmiot_backend.module.biometric.dto.VoiceprintVerifyRequest;
import com.hmiot.hmiot_backend.module.biometric.entity.BiometricAuthRecord;
import com.hmiot.hmiot_backend.module.biometric.entity.BiometricTemplate;
import com.hmiot.hmiot_backend.module.biometric.mapper.BiometricAuthRecordMapper;
import com.hmiot.hmiot_backend.module.biometric.mapper.BiometricTemplateMapper;
import com.hmiot.hmiot_backend.module.biometric.vo.BiometricTemplateVO;
import com.hmiot.hmiot_backend.module.biometric.vo.VoiceprintHealthResponse;
import com.hmiot.hmiot_backend.module.biometric.vo.VoiceprintVerifyResponse;
import com.hmiot.hmiot_backend.module.integration.service.ExternalIntegrationCallService;
import com.hmiot.hmiot_backend.module.integration.service.VoiceprintIntegrationClient;
import com.hmiot.hmiot_backend.module.integration.service.VoiceprintIntegrationClient.EnrollResult;
import com.hmiot.hmiot_backend.module.integration.service.VoiceprintIntegrationClient.HealthResult;
import com.hmiot.hmiot_backend.module.integration.service.VoiceprintIntegrationClient.VerifyResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VoiceprintAuthService {
    private static final String FAIL = "FAIL";

    private final BiometricTemplateMapper biometricTemplateMapper;
    private final BiometricAuthRecordMapper biometricAuthRecordMapper;
    private final BiometricTemplateService biometricTemplateService;
    private final VoiceprintIntegrationClient voiceprintIntegrationClient;
    private final ExternalIntegrationCallService externalIntegrationCallService;

    public VoiceprintAuthService(BiometricTemplateMapper biometricTemplateMapper, BiometricAuthRecordMapper biometricAuthRecordMapper, BiometricTemplateService biometricTemplateService, VoiceprintIntegrationClient voiceprintIntegrationClient, ExternalIntegrationCallService externalIntegrationCallService) {
        this.biometricTemplateMapper = biometricTemplateMapper;
        this.biometricAuthRecordMapper = biometricAuthRecordMapper;
        this.biometricTemplateService = biometricTemplateService;
        this.voiceprintIntegrationClient = voiceprintIntegrationClient;
        this.externalIntegrationCallService = externalIntegrationCallService;
    }

    public VoiceprintHealthResponse health() {
        LocalDateTime startedAt = LocalDateTime.now();
        try {
            HealthResult result = voiceprintIntegrationClient.health();
            externalIntegrationCallService.recordSuccess("VOICEPRINT_HEALTH", voiceprintIntegrationClient.targetSystem(), "VOICEPRINT", Map.of(), Map.of("healthy", result.healthy(), "modelVersion", result.modelVersion()), startedAt);
            return new VoiceprintHealthResponse(result.healthy(), voiceprintIntegrationClient.targetSystem(), result.modelVersion());
        } catch (RuntimeException exception) {
            externalIntegrationCallService.recordFailure("VOICEPRINT_HEALTH", voiceprintIntegrationClient.targetSystem(), "VOICEPRINT", Map.of(), exception.getMessage(), startedAt);
            throw exception;
        }
    }

    @Transactional
    public BiometricTemplateVO enroll(CurrentUser currentUser, VoiceprintEnrollRequest request) {
        return submitTemplate(currentUser, request, false);
    }

    @Transactional
    public BiometricTemplateVO recollect(CurrentUser currentUser, VoiceprintEnrollRequest request) {
        return submitTemplate(currentUser, request, true);
    }

    public List<BiometricTemplateVO> myTemplates(CurrentUser currentUser) {
        return biometricTemplateService.listUserVoiceprints(currentUser.userId());
    }

    @Transactional
    public VoiceprintVerifyResponse verify(CurrentUser currentUser, VoiceprintVerifyRequest request) {
        BiometricTemplate template = biometricTemplateService.latestApprovedVoiceprint(currentUser.userId());
        LocalDateTime startedAt = LocalDateTime.now();
        Map<String, Object> requestSummary = Map.of(
                "userId", currentUser.userId(),
                "username", currentUser.username(),
                "templateId", template.getId(),
                "audioFormat", request.audioFormat() == null ? "" : request.audioFormat(),
                "sampleRate", request.sampleRate() == null ? "" : request.sampleRate()
        );
        VerifyResult result;
        try {
            result = voiceprintIntegrationClient.verify(template.getTemplateRef(), template.getTemplateDigest(), request.audioBase64(), template.getThreshold(), request.metadata());
        } catch (RuntimeException exception) {
            BiometricAuthRecord record = insertAuthRecord(currentUser.userId(), template, null, null, template.getThreshold(), FAIL, exception.getMessage(), voiceprintIntegrationClient.targetSystem(), Map.of("error", exception.getMessage()));
            externalIntegrationCallService.recordFailure("VOICEPRINT_VERIFY", voiceprintIntegrationClient.targetSystem(), String.valueOf(currentUser.userId()), requestSummary, exception.getMessage(), startedAt);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "声纹验证失败，流水ID=" + record.getId());
        }
        String authResult = result.verified() ? BiometricTemplateService.SUCCESS : FAIL;
        String failReason = result.verified() ? null : (result.failReason() == null ? "声纹验证未通过" : result.failReason());
        BiometricAuthRecord record = insertAuthRecord(currentUser.userId(), template, result.requestId(), result.score(), result.threshold(), authResult, failReason, result.modelSource(), result.rawResponse());
        externalIntegrationCallService.recordSuccess("VOICEPRINT_VERIFY", voiceprintIntegrationClient.targetSystem(), String.valueOf(currentUser.userId()), requestSummary, Map.of(
                "recordId", record.getId(),
                "verified", result.verified(),
                "score", result.score(),
                "threshold", result.threshold(),
                "failReason", failReason == null ? "" : failReason
        ), startedAt);
        return new VoiceprintVerifyResponse(record.getId(), result.verified(), result.score(), result.threshold(), failReason);
    }

    private BiometricTemplateVO submitTemplate(CurrentUser currentUser, VoiceprintEnrollRequest request, boolean recollect) {
        if (recollect) {
            biometricTemplateService.replaceActivePendingVoiceprints(currentUser.userId());
        }
        LocalDateTime startedAt = LocalDateTime.now();
        Map<String, Object> requestSummary = Map.of(
                "userId", currentUser.userId(),
                "username", currentUser.username(),
                "recollect", recollect,
                "audioFormat", request.audioFormat() == null ? "" : request.audioFormat(),
                "sampleRate", request.sampleRate() == null ? "" : request.sampleRate()
        );
        EnrollResult result;
        try {
            result = voiceprintIntegrationClient.enroll(currentUser.userId(), currentUser.username(), request.audioBase64(), request.metadata());
        } catch (RuntimeException exception) {
            externalIntegrationCallService.recordFailure("VOICEPRINT_ENROLL", voiceprintIntegrationClient.targetSystem(), String.valueOf(currentUser.userId()), requestSummary, exception.getMessage(), startedAt);
            throw exception;
        }
        BiometricTemplate template = new BiometricTemplate();
        template.setUserId(currentUser.userId());
        template.setBiometricType(BiometricTemplateService.VOICEPRINT);
        template.setTemplateDigest(result.templateDigest());
        template.setTemplateRef(result.templateRef());
        template.setModelSource(result.modelSource());
        template.setModelVersion(result.modelVersion());
        template.setThreshold(result.threshold());
        template.setAuditStatus(BiometricTemplateService.PENDING);
        template.setTemplateStatus(BiometricTemplateService.ACTIVE);
        template.setVersionNo(biometricTemplateService.nextVoiceprintVersion(currentUser.userId()));
        template.setSubmittedAt(LocalDateTime.now());
        template.setMetadata(biometricTemplateService.toJson(metadata(request, result.rawResponse(), recollect)));
        biometricTemplateMapper.insert(template);
        externalIntegrationCallService.recordSuccess("VOICEPRINT_ENROLL", voiceprintIntegrationClient.targetSystem(), String.valueOf(currentUser.userId()), requestSummary, Map.of(
                "templateId", template.getId(),
                "templateRef", result.templateRef(),
                "modelSource", result.modelSource(),
                "modelVersion", result.modelVersion(),
                "threshold", result.threshold()
        ), startedAt);
        return biometricTemplateService.toTemplateVO(template);
    }

    private Map<String, Object> metadata(VoiceprintEnrollRequest request, Map<String, Object> rawResponse, boolean recollect) {
        Map<String, Object> metadata = new HashMap<>();
        if (request.metadata() != null) {
            metadata.putAll(request.metadata());
        }
        metadata.put("recollect", recollect);
        if (request.audioFormat() != null) {
            metadata.put("audioFormat", request.audioFormat());
        }
        if (request.sampleRate() != null) {
            metadata.put("sampleRate", request.sampleRate());
        }
        metadata.put("modelResponse", rawResponse == null ? Map.of() : rawResponse);
        return metadata;
    }

    private BiometricAuthRecord insertAuthRecord(Long userId, BiometricTemplate template, String requestId, java.math.BigDecimal score, java.math.BigDecimal threshold, String authResult, String failReason, String modelSource, Map<String, Object> rawResponse) {
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
}
