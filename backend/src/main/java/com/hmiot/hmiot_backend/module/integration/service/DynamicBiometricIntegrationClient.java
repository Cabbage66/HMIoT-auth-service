package com.hmiot.hmiot_backend.module.integration.service;

import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.module.biometric.dto.DynamicBiometricReportRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
public class DynamicBiometricIntegrationClient {
    private final String dynamicBiometricUrl;
    private final RestClient restClient;

    public DynamicBiometricIntegrationClient(@Value("${hmiot.integrations.dynamic-biometric-url:mock://dynamic-biometric}") String dynamicBiometricUrl, RestClient.Builder restClientBuilder) {
        this.dynamicBiometricUrl = dynamicBiometricUrl;
        this.restClient = dynamicBiometricUrl.startsWith("http://") || dynamicBiometricUrl.startsWith("https://")
                ? restClientBuilder.baseUrl(dynamicBiometricUrl).build()
                : null;
    }

    public HealthResult health() {
        if (dynamicBiometricUrl.startsWith("mock://")) {
            return new HealthResult(true, "mock-dynamic-biometric-1.0");
        }
        HealthResponse response = restClient.get()
                .uri("/health")
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "动态生物服务健康检查失败");
                })
                .body(HealthResponse.class);
        if (response == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "动态生物服务健康检查响应不完整");
        }
        return new HealthResult(response.healthy() == null || Boolean.TRUE.equals(response.healthy()), response.model_version() == null ? "unknown" : response.model_version());
    }

    public MatchResult queryMatch(Long userId, String externalSubjectId, String biometricType, String requestId) {
        String effectiveRequestId = requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId;
        if (dynamicBiometricUrl.startsWith("mock://")) {
            boolean matched = !externalSubjectId.contains("fail") && !externalSubjectId.contains("mismatch");
            BigDecimal score = matched ? new BigDecimal("0.910000") : new BigDecimal("0.360000");
            BigDecimal threshold = new BigDecimal("0.700000");
            return new MatchResult(effectiveRequestId, matched, score, threshold, matched ? null : "动态生物特征不匹配", "mock-dynamic-biometric", "mock-dynamic-biometric-1.0", Map.of(
                    "mock", true,
                    "userId", userId,
                    "biometricType", biometricType == null ? "DYNAMIC_BIOMETRIC" : biometricType,
                    "matched", matched
            ));
        }
        QueryResponse response = restClient.post()
                .uri("/dynamic-biometric/query")
                .body(new QueryRequest(userId, externalSubjectId, biometricType, effectiveRequestId))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "动态生物查询服务调用失败");
                })
                .body(QueryResponse.class);
        if (response == null || response.matched() == null || response.score() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "动态生物查询服务响应不完整");
        }
        return new MatchResult(response.request_id() == null ? effectiveRequestId : response.request_id(), Boolean.TRUE.equals(response.matched()), response.score(), response.threshold() == null ? new BigDecimal("0.700000") : response.threshold(), response.fail_reason(), response.model_source() == null ? "dynamic-biometric" : response.model_source(), response.model_version(), response.raw_response() == null ? Map.of() : response.raw_response());
    }

    public MatchResult normalizeReport(DynamicBiometricReportRequest request) {
        if (request.matched() == null || request.score() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "动态生物回传结果不完整");
        }
        BigDecimal threshold = request.threshold() == null ? new BigDecimal("0.700000") : request.threshold();
        return new MatchResult(request.requestId(), Boolean.TRUE.equals(request.matched()), request.score(), threshold, request.failReason(), request.modelSource() == null ? "dynamic-biometric" : request.modelSource(), request.modelVersion(), request.rawResponse() == null ? Map.of() : request.rawResponse());
    }

    public String targetSystem() {
        return dynamicBiometricUrl;
    }

    private record HealthResponse(Boolean healthy, String model_version) {
    }

    private record QueryRequest(Long user_id, String external_subject_id, String biometric_type, String request_id) {
    }

    private record QueryResponse(String request_id, Boolean matched, BigDecimal score, BigDecimal threshold, String fail_reason, String model_source, String model_version, Map<String, Object> raw_response) {
    }

    public record HealthResult(boolean healthy, String modelVersion) {
    }

    public record MatchResult(String requestId, boolean matched, BigDecimal score, BigDecimal threshold, String failReason, String modelSource, String modelVersion, Map<String, Object> rawResponse) {
    }
}
