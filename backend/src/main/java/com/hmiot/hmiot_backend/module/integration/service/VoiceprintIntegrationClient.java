package com.hmiot.hmiot_backend.module.integration.service;

import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
public class VoiceprintIntegrationClient {
    private final String voiceprintServiceUrl;
    private final RestClient restClient;

    public VoiceprintIntegrationClient(@Value("${hmiot.integrations.voiceprint-service-url:http://localhost:5003}") String voiceprintServiceUrl, RestClient.Builder restClientBuilder) {
        this.voiceprintServiceUrl = voiceprintServiceUrl;
        if (!voiceprintServiceUrl.startsWith("http://") && !voiceprintServiceUrl.startsWith("https://")) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "voiceprint-service-url必须为HTTP(S)地址，禁止mock协议");
        }
        this.restClient = restClientBuilder.baseUrl(voiceprintServiceUrl).build();
    }

    public HealthResult health() {
        HealthResponse response = restClient.get()
                .uri("/health")
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "声纹服务健康检查失败");
                })
                .body(HealthResponse.class);
        if (response == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "声纹服务健康检查响应不完整");
        }
        return new HealthResult(response.healthy() == null || Boolean.TRUE.equals(response.healthy()), response.model_version() == null ? "unknown" : response.model_version());
    }

    public EnrollResult enroll(Long userId, String username, String audioBase64, Map<String, Object> metadata) {
        EnrollResponse response = restClient.post()
                .uri("/voiceprint/enroll")
                .body(new EnrollRequest(userId, username, audioBase64, metadata))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "声纹注册服务调用失败");
                })
                .body(EnrollResponse.class);
        if (response == null || response.template_ref() == null || response.template_digest() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "声纹注册服务响应不完整");
        }
        return new EnrollResult(response.template_ref(), response.template_digest(), response.model_source() == null ? "voiceprint-engine" : response.model_source(), response.model_version(), response.threshold() == null ? new BigDecimal("0.750000") : response.threshold(), response.raw_response() == null ? Map.of() : response.raw_response());
    }

    public VerifyResult verify(String templateRef, String templateDigest, String audioBase64, BigDecimal threshold, Map<String, Object> metadata) {
        VerifyResponse response = restClient.post()
                .uri("/voiceprint/verify")
                .body(new VerifyRequest(templateRef, templateDigest, audioBase64, threshold, metadata))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "声纹验证服务调用失败");
                })
                .body(VerifyResponse.class);
        if (response == null || response.verified() == null || response.score() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "声纹验证服务响应不完整");
        }
        BigDecimal effectiveThreshold = response.threshold() == null ? threshold : response.threshold();
        return new VerifyResult(response.request_id() == null ? UUID.randomUUID().toString() : response.request_id(), Boolean.TRUE.equals(response.verified()), response.score(), effectiveThreshold, response.fail_reason(), response.model_source() == null ? "voiceprint-engine" : response.model_source(), response.model_version(), response.raw_response() == null ? Map.of() : response.raw_response());
    }

    public String targetSystem() {
        return voiceprintServiceUrl;
    }

    private record HealthResponse(Boolean healthy, String model_version) {
    }

    private record EnrollRequest(Long user_id, String username, String audio_base64, Map<String, Object> metadata) {
    }

    private record EnrollResponse(String template_ref, String template_digest, String model_source, String model_version, BigDecimal threshold, Map<String, Object> raw_response) {
    }

    private record VerifyRequest(String template_ref, String template_digest, String audio_base64, BigDecimal threshold, Map<String, Object> metadata) {
    }

    private record VerifyResponse(String request_id, Boolean verified, BigDecimal score, BigDecimal threshold, String fail_reason, String model_source, String model_version, Map<String, Object> raw_response) {
    }

    public record HealthResult(boolean healthy, String modelVersion) {
    }

    public record EnrollResult(String templateRef, String templateDigest, String modelSource, String modelVersion, BigDecimal threshold, Map<String, Object> rawResponse) {
    }

    public record VerifyResult(String requestId, boolean verified, BigDecimal score, BigDecimal threshold, String failReason, String modelSource, String modelVersion, Map<String, Object> rawResponse) {
    }
}
