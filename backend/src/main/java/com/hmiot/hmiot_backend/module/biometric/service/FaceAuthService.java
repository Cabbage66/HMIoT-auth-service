package com.hmiot.hmiot_backend.module.biometric.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.common.security.CurrentUser;
import com.hmiot.hmiot_backend.module.biometric.dto.FaceSubmitRequest;
import com.hmiot.hmiot_backend.module.biometric.dto.FaceVerifyRequest;
import com.hmiot.hmiot_backend.module.biometric.entity.BiometricAuthRecord;
import com.hmiot.hmiot_backend.module.biometric.entity.BiometricTemplate;
import com.hmiot.hmiot_backend.module.biometric.mapper.BiometricAuthRecordMapper;
import com.hmiot.hmiot_backend.module.biometric.mapper.BiometricTemplateMapper;
import com.hmiot.hmiot_backend.module.biometric.vo.BiometricTemplateVO;
import com.hmiot.hmiot_backend.module.biometric.vo.FaceHealthResponse;
import com.hmiot.hmiot_backend.module.biometric.vo.FaceVerifyResponse;
import com.hmiot.hmiot_backend.module.integration.service.ExternalIntegrationCallService;
import com.hmiot.hmiot_backend.module.integration.service.FaceIntegrationClient;
import com.hmiot.hmiot_backend.module.integration.service.FaceIntegrationClient.EnrollResult;
import com.hmiot.hmiot_backend.module.integration.service.FaceIntegrationClient.HealthResult;
import com.hmiot.hmiot_backend.module.integration.service.FaceIntegrationClient.VerifyResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class FaceAuthService {
    private static final String FAIL = "FAIL";

    private final BiometricTemplateMapper biometricTemplateMapper;
    private final BiometricAuthRecordMapper biometricAuthRecordMapper;
    private final BiometricTemplateService biometricTemplateService;
    private final FaceIntegrationClient faceIntegrationClient;
    private final ExternalIntegrationCallService externalIntegrationCallService;

    public FaceAuthService(
            BiometricTemplateMapper biometricTemplateMapper,
            BiometricAuthRecordMapper biometricAuthRecordMapper,
            BiometricTemplateService biometricTemplateService,
            FaceIntegrationClient faceIntegrationClient,
            ExternalIntegrationCallService externalIntegrationCallService
    ) {
        this.biometricTemplateMapper = biometricTemplateMapper;
        this.biometricAuthRecordMapper = biometricAuthRecordMapper;
        this.biometricTemplateService = biometricTemplateService;
        this.faceIntegrationClient = faceIntegrationClient;
        this.externalIntegrationCallService = externalIntegrationCallService;
    }

    public FaceHealthResponse health() {
        LocalDateTime startedAt = LocalDateTime.now();
        try {
            HealthResult result = faceIntegrationClient.health();
            externalIntegrationCallService.recordSuccess("FACE_HEALTH", faceIntegrationClient.targetSystem(), BiometricTemplateService.FACE, Map.of(), Map.of("healthy", result.healthy(), "modelVersion", result.modelVersion()), startedAt);
            return new FaceHealthResponse(result.healthy(), faceIntegrationClient.targetSystem(), result.modelVersion());
        } catch (RuntimeException exception) {
            externalIntegrationCallService.recordFailure("FACE_HEALTH", faceIntegrationClient.targetSystem(), BiometricTemplateService.FACE, Map.of(), exception.getMessage(), startedAt);
            throw exception;
        }
    }

    @Transactional
    public BiometricTemplateVO enroll(CurrentUser currentUser, FaceSubmitRequest request) {
        return submitTemplate(currentUser, request, false);
    }

    @Transactional
    public BiometricTemplateVO recollect(CurrentUser currentUser, FaceSubmitRequest request) {
        return submitTemplate(currentUser, request, true);
    }

    @Transactional
    public FaceVerifyResponse verify(CurrentUser currentUser, FaceVerifyRequest request) {
        BiometricTemplate template = biometricTemplateService.latestApprovedFace(currentUser.userId());
        LocalDateTime startedAt = LocalDateTime.now();
        Map<String, Object> requestSummary = Map.of(
                "userId", currentUser.userId(),
                "username", currentUser.username(),
                "templateId", template.getId(),
                "imageFormat", normalizeImageFormat(request.imageFormat()),
                "requestId", request.requestId() == null ? "" : request.requestId()
        );
        VerifyResult result;
        try {
            result = faceIntegrationClient.verify(
                    template.getTemplateRef(),
                    template.getTemplateDigest(),
                    request.imageBase64(),
                    normalizeImageFormat(request.imageFormat()),
                    template.getThreshold(),
                    request.metadata()
            );
        } catch (RuntimeException exception) {
            BiometricAuthRecord record = insertAuthRecord(currentUser.userId(), template, null, null, template.getThreshold(), FAIL, exception.getMessage(), faceIntegrationClient.targetSystem(), Map.of("error", exception.getMessage()));
            externalIntegrationCallService.recordFailure("FACE_VERIFY", faceIntegrationClient.targetSystem(), String.valueOf(currentUser.userId()), requestSummary, exception.getMessage(), startedAt);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "人脸验证失败，流水ID=" + record.getId());
        }
        String authResult = result.verified() ? BiometricTemplateService.SUCCESS : FAIL;
        String failReason = result.verified() ? null : (result.failReason() == null ? "人脸验证未通过" : result.failReason());
        BiometricAuthRecord record = insertAuthRecord(currentUser.userId(), template, result.requestId(), result.score(), result.threshold(), authResult, failReason, result.modelSource(), result.rawResponse());
        externalIntegrationCallService.recordSuccess("FACE_VERIFY", faceIntegrationClient.targetSystem(), String.valueOf(currentUser.userId()), requestSummary, Map.of(
                "recordId", record.getId(),
                "verified", result.verified(),
                "score", result.score(),
                "threshold", result.threshold(),
                "failReason", failReason == null ? "" : failReason
        ), startedAt);
        return new FaceVerifyResponse(record.getId(), result.verified(), result.score(), result.threshold(), failReason);
    }

    private BiometricTemplateVO submitTemplate(CurrentUser currentUser, FaceSubmitRequest request, boolean recollect) {
        if (recollect) {
            biometricTemplateService.replaceActivePendingFaces(currentUser.userId());
        }
        LocalDateTime startedAt = LocalDateTime.now();
        Map<String, Object> requestSummary = Map.of(
                "userId", currentUser.userId(),
                "username", currentUser.username(),
                "recollect", recollect,
                "imageFormat", normalizeImageFormat(request.imageFormat())
        );
        EnrollResult result;
        try {
            result = faceIntegrationClient.enroll(
                    currentUser.userId(),
                    currentUser.username(),
                    request.imageBase64(),
                    normalizeImageFormat(request.imageFormat()),
                    request.metadata()
            );
        } catch (RuntimeException exception) {
            externalIntegrationCallService.recordFailure("FACE_ENROLL", faceIntegrationClient.targetSystem(), String.valueOf(currentUser.userId()), requestSummary, exception.getMessage(), startedAt);
            throw exception;
        }
        BiometricTemplate template = new BiometricTemplate();
        template.setUserId(currentUser.userId());
        template.setBiometricType(BiometricTemplateService.FACE);
        template.setTemplateDigest(result.templateDigest());
        template.setTemplateRef(result.templateRef());
        template.setModelSource(result.modelSource());
        template.setModelVersion(result.modelVersion());
        template.setThreshold(result.threshold());
        template.setAuditStatus(BiometricTemplateService.PENDING);
        template.setTemplateStatus(BiometricTemplateService.ACTIVE);
        template.setVersionNo(biometricTemplateService.nextFaceVersion(currentUser.userId()));
        template.setSubmittedAt(LocalDateTime.now());
        template.setMetadata(biometricTemplateService.toJson(metadata(request, result.rawResponse(), recollect)));
        biometricTemplateMapper.insert(template);
        externalIntegrationCallService.recordSuccess("FACE_ENROLL", faceIntegrationClient.targetSystem(), String.valueOf(currentUser.userId()), requestSummary, Map.of(
                "templateId", template.getId(),
                "templateRef", result.templateRef(),
                "modelSource", result.modelSource(),
                "modelVersion", result.modelVersion(),
                "threshold", result.threshold()
        ), startedAt);
        return biometricTemplateService.toTemplateVO(template);
    }

    public BiometricTemplate createPendingTemplateByRegistration(
            Long userId,
            String username,
            String imageBase64,
            String imageFormat,
            String registerSource
    ) {
        EnrollResult result = faceIntegrationClient.enroll(
                userId,
                username,
                imageBase64.trim(),
                normalizeImageFormat(imageFormat),
                Map.of(
                        "scene", "REGISTRATION_SUBMIT",
                        "registerSource", registerSource,
                        "imageFormat", normalizeImageFormat(imageFormat)
                )
        );
        BiometricTemplate template = new BiometricTemplate();
        template.setUserId(userId);
        template.setBiometricType(BiometricTemplateService.FACE);
        template.setTemplateDigest(result.templateDigest());
        template.setTemplateRef(result.templateRef());
        template.setModelSource(result.modelSource());
        template.setModelVersion(result.modelVersion());
        template.setThreshold(result.threshold());
        template.setAuditStatus(BiometricTemplateService.PENDING);
        template.setTemplateStatus(BiometricTemplateService.ACTIVE);
        template.setVersionNo(biometricTemplateService.nextFaceVersion(userId));
        template.setSubmittedAt(LocalDateTime.now());
        template.setMetadata(biometricTemplateService.toJson(Map.of(
                "scene", "REGISTRATION_SUBMIT",
                "registerSource", registerSource,
                "imageFormat", normalizeImageFormat(imageFormat),
                "modelResponse", result.rawResponse() == null ? Map.of() : result.rawResponse()
        )));
        biometricTemplateMapper.insert(template);
        return template;
    }

    private String normalizeImageFormat(String imageFormat) {
        if (!StringUtils.hasText(imageFormat)) {
            return "jpeg";
        }
        return imageFormat.trim().toLowerCase();
    }

    private Map<String, Object> metadata(FaceSubmitRequest request, Map<String, Object> rawResponse, boolean recollect) {
        Map<String, Object> metadata = new HashMap<>();
        if (request.metadata() != null) {
            metadata.putAll(request.metadata());
        }
        metadata.put("recollect", recollect);
        metadata.put("imageFormat", normalizeImageFormat(request.imageFormat()));
        metadata.put("modelResponse", rawResponse == null ? Map.of() : rawResponse);
        return metadata;
    }

    private BiometricAuthRecord insertAuthRecord(Long userId, BiometricTemplate template, String requestId, java.math.BigDecimal score, java.math.BigDecimal threshold, String authResult, String failReason, String modelSource, Map<String, Object> rawResponse) {
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
}

