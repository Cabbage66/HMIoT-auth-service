package com.hmiot.hmiot_backend.module.biometric.service;

import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.common.security.CurrentUser;
import com.hmiot.hmiot_backend.module.binding.entity.BindingRelation;
import com.hmiot.hmiot_backend.module.binding.service.BindingRelationService;
import com.hmiot.hmiot_backend.module.biometric.dto.DynamicBiometricQueryRequest;
import com.hmiot.hmiot_backend.module.biometric.dto.DynamicBiometricReportRequest;
import com.hmiot.hmiot_backend.module.biometric.entity.BiometricAuthRecord;
import com.hmiot.hmiot_backend.module.biometric.mapper.BiometricAuthRecordMapper;
import com.hmiot.hmiot_backend.module.biometric.vo.DynamicBiometricHealthResponse;
import com.hmiot.hmiot_backend.module.biometric.vo.DynamicBiometricVerifyResponse;
import com.hmiot.hmiot_backend.module.integration.service.DynamicBiometricIntegrationClient;
import com.hmiot.hmiot_backend.module.integration.service.DynamicBiometricIntegrationClient.HealthResult;
import com.hmiot.hmiot_backend.module.integration.service.DynamicBiometricIntegrationClient.MatchResult;
import com.hmiot.hmiot_backend.module.integration.service.ExternalIntegrationCallService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class DynamicBiometricAuthService {
    private static final String FAIL = "FAIL";

    private final BindingRelationService bindingRelationService;
    private final BiometricAuthRecordMapper biometricAuthRecordMapper;
    private final BiometricTemplateService biometricTemplateService;
    private final DynamicBiometricIntegrationClient dynamicBiometricIntegrationClient;
    private final ExternalIntegrationCallService externalIntegrationCallService;

    public DynamicBiometricAuthService(BindingRelationService bindingRelationService, BiometricAuthRecordMapper biometricAuthRecordMapper, BiometricTemplateService biometricTemplateService, DynamicBiometricIntegrationClient dynamicBiometricIntegrationClient, ExternalIntegrationCallService externalIntegrationCallService) {
        this.bindingRelationService = bindingRelationService;
        this.biometricAuthRecordMapper = biometricAuthRecordMapper;
        this.biometricTemplateService = biometricTemplateService;
        this.dynamicBiometricIntegrationClient = dynamicBiometricIntegrationClient;
        this.externalIntegrationCallService = externalIntegrationCallService;
    }

    public DynamicBiometricHealthResponse health() {
        LocalDateTime startedAt = LocalDateTime.now();
        try {
            HealthResult result = dynamicBiometricIntegrationClient.health();
            externalIntegrationCallService.recordSuccess("DYNAMIC_BIOMETRIC_HEALTH", dynamicBiometricIntegrationClient.targetSystem(), BiometricTemplateService.DYNAMIC_BIOMETRIC, Map.of(), Map.of("healthy", result.healthy(), "modelVersion", result.modelVersion()), startedAt);
            return new DynamicBiometricHealthResponse(result.healthy(), dynamicBiometricIntegrationClient.targetSystem(), result.modelVersion());
        } catch (RuntimeException exception) {
            externalIntegrationCallService.recordFailure("DYNAMIC_BIOMETRIC_HEALTH", dynamicBiometricIntegrationClient.targetSystem(), BiometricTemplateService.DYNAMIC_BIOMETRIC, Map.of(), exception.getMessage(), startedAt);
            throw exception;
        }
    }

    @Transactional
    public DynamicBiometricVerifyResponse query(CurrentUser currentUser, DynamicBiometricQueryRequest request) {
        BindingRelation binding = bindingRelationService.findActiveDynamicBiometricBinding(currentUser.userId());
        String biometricType = request.biometricType() == null || request.biometricType().isBlank() ? BiometricTemplateService.DYNAMIC_BIOMETRIC : request.biometricType();
        LocalDateTime startedAt = LocalDateTime.now();
        Map<String, Object> requestSummary = Map.of(
                "userId", currentUser.userId(),
                "username", currentUser.username(),
                "bindingId", binding.getId(),
                "targetId", binding.getTargetId(),
                "biometricType", biometricType,
                "requestId", request.requestId() == null ? "" : request.requestId()
        );
        MatchResult result;
        try {
            result = dynamicBiometricIntegrationClient.queryMatch(currentUser.userId(), String.valueOf(binding.getTargetId()), biometricType, request.requestId());
        } catch (RuntimeException exception) {
            BiometricAuthRecord record = insertAuthRecord(currentUser.userId(), request.requestId(), null, null, FAIL, exception.getMessage(), dynamicBiometricIntegrationClient.targetSystem(), Map.of("error", exception.getMessage(), "bindingId", binding.getId(), "targetId", binding.getTargetId()));
            externalIntegrationCallService.recordFailure("DYNAMIC_BIOMETRIC_QUERY", dynamicBiometricIntegrationClient.targetSystem(), String.valueOf(currentUser.userId()), requestSummary, exception.getMessage(), startedAt);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "动态生物查询失败，流水ID=" + record.getId());
        }
        BiometricAuthRecord record = recordResult(currentUser.userId(), binding.getTargetId(), result, "动态生物特征不匹配");
        externalIntegrationCallService.recordSuccess("DYNAMIC_BIOMETRIC_QUERY", dynamicBiometricIntegrationClient.targetSystem(), String.valueOf(currentUser.userId()), requestSummary, responseSummary(record, result), startedAt);
        return new DynamicBiometricVerifyResponse(record.getId(), result.requestId(), result.matched(), result.score(), result.threshold(), record.getFailReason());
    }

    @Transactional
    public DynamicBiometricVerifyResponse report(DynamicBiometricReportRequest request) {
        LocalDateTime startedAt = LocalDateTime.now();
        Map<String, Object> requestSummary = Map.of(
                "userId", request.userId(),
                "externalSubjectId", request.externalSubjectId(),
                "biometricType", request.biometricType() == null ? BiometricTemplateService.DYNAMIC_BIOMETRIC : request.biometricType(),
                "requestId", request.requestId()
        );
        BindingRelation binding;
        try {
            binding = bindingRelationService.requireActiveDynamicBiometricBinding(request.userId(), request.externalSubjectId());
        } catch (RuntimeException exception) {
            BiometricAuthRecord record = insertAuthRecord(request.userId(), request.requestId(), null, null, FAIL, exception.getMessage(), dynamicBiometricIntegrationClient.targetSystem(), Map.of("error", exception.getMessage()));
            externalIntegrationCallService.recordFailure("DYNAMIC_BIOMETRIC_REPORT", dynamicBiometricIntegrationClient.targetSystem(), String.valueOf(request.userId()), requestSummary, exception.getMessage(), startedAt);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "动态生物回传绑定校验失败，流水ID=" + record.getId());
        }
        MatchResult result;
        try {
            result = dynamicBiometricIntegrationClient.normalizeReport(request);
        } catch (RuntimeException exception) {
            BiometricAuthRecord record = insertAuthRecord(request.userId(), request.requestId(), null, null, FAIL, exception.getMessage(), dynamicBiometricIntegrationClient.targetSystem(), Map.of("error", exception.getMessage()));
            externalIntegrationCallService.recordFailure("DYNAMIC_BIOMETRIC_REPORT", dynamicBiometricIntegrationClient.targetSystem(), String.valueOf(request.userId()), requestSummary, exception.getMessage(), startedAt);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "动态生物回传处理失败，流水ID=" + record.getId());
        }
        BiometricAuthRecord record = recordResult(request.userId(), binding.getTargetId(), result, "动态生物回传结果未通过");
        externalIntegrationCallService.recordSuccess("DYNAMIC_BIOMETRIC_REPORT", dynamicBiometricIntegrationClient.targetSystem(), String.valueOf(request.userId()), requestSummary, responseSummary(record, result), startedAt);
        return new DynamicBiometricVerifyResponse(record.getId(), result.requestId(), result.matched(), result.score(), result.threshold(), record.getFailReason());
    }

    private BiometricAuthRecord recordResult(Long userId, Long targetId, MatchResult result, String defaultFailReason) {
        String authResult = result.matched() ? BiometricTemplateService.SUCCESS : FAIL;
        String failReason = result.matched() ? null : (result.failReason() == null ? defaultFailReason : result.failReason());
        Map<String, Object> rawResponse = new java.util.LinkedHashMap<>(result.rawResponse());
        rawResponse.put("dynamicBiometricTargetId", targetId);
        return insertAuthRecord(userId, result.requestId(), result.score(), result.threshold(), authResult, failReason, result.modelSource(), rawResponse);
    }

    private Map<String, Object> responseSummary(BiometricAuthRecord record, MatchResult result) {
        return Map.of(
                "recordId", record.getId(),
                "requestId", result.requestId() == null ? "" : result.requestId(),
                "matched", result.matched(),
                "score", result.score(),
                "threshold", result.threshold(),
                "failReason", record.getFailReason() == null ? "" : record.getFailReason()
        );
    }

    private BiometricAuthRecord insertAuthRecord(Long userId, String requestId, java.math.BigDecimal score, java.math.BigDecimal threshold, String authResult, String failReason, String modelSource, Map<String, Object> rawResponse) {
        BiometricAuthRecord record = new BiometricAuthRecord();
        record.setUserId(userId);
        record.setBiometricType(BiometricTemplateService.DYNAMIC_BIOMETRIC);
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
