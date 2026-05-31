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
public class FaceIntegrationClient {
    private final String faceServiceUrl;
    private final RestClient restClient;

    public FaceIntegrationClient(@Value("${hmiot.integrations.face-service-url:http://localhost:5005}") String faceServiceUrl, RestClient.Builder restClientBuilder) {
        this.faceServiceUrl = faceServiceUrl;
        if (!faceServiceUrl.startsWith("http://") && !faceServiceUrl.startsWith("https://")) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "face-service-url必须为HTTP(S)地址，禁止mock协议");
        }
        this.restClient = restClientBuilder.baseUrl(faceServiceUrl).build();
    }

    public HealthResult health() {
        HealthResponse response = restClient.get()
                .uri("/health")
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "人脸服务健康检查失败");
                })
                .body(HealthResponse.class);
        if (response == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "人脸服务健康检查响应不完整");
        }
        return new HealthResult(response.healthy() == null || Boolean.TRUE.equals(response.healthy()), response.model_version() == null ? "unknown" : response.model_version());
    }

    public EnrollResult enroll(Long userId, String username, String imageBase64, String imageFormat, Map<String, Object> metadata) {
        EnrollResponse response = restClient.post()
                .uri("/face/enroll")
                .body(new EnrollRequest(userId, username, imageBase64, imageFormat, metadata))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "人脸注册服务调用失败");
                })
                .body(EnrollResponse.class);
        if (response == null || response.template_ref() == null || response.template_digest() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "人脸注册服务响应不完整");
        }
        return new EnrollResult(
                response.template_ref(),
                response.template_digest(),
                response.model_source() == null ? "face-engine" : response.model_source(),
                response.model_version(),
                response.threshold() == null ? new BigDecimal("0.800000") : response.threshold(),
                response.raw_response() == null ? Map.of() : response.raw_response()
        );
    }

    public VerifyResult verify(String templateRef, String templateDigest, String imageBase64, String imageFormat, BigDecimal threshold, Map<String, Object> metadata) {
        VerifyResponse response = restClient.post()
                .uri("/face/verify")
                .body(new VerifyRequest(templateRef, templateDigest, imageBase64, imageFormat, threshold, metadata))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "人脸验证服务调用失败");
                })
                .body(VerifyResponse.class);
        if (response == null || response.verified() == null || response.score() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "人脸验证服务响应不完整");
        }
        BigDecimal effectiveThreshold = response.threshold() == null ? threshold : response.threshold();
        return new VerifyResult(
                response.request_id() == null ? UUID.randomUUID().toString() : response.request_id(),
                Boolean.TRUE.equals(response.verified()),
                response.score(),
                effectiveThreshold,
                response.fail_reason(),
                response.model_source() == null ? "face-engine" : response.model_source(),
                response.model_version(),
                response.raw_response() == null ? Map.of() : response.raw_response()
        );
    }

    public String targetSystem() {
        return faceServiceUrl;
    }

    private record HealthResponse(Boolean healthy, String model_version) {
    }

    private record EnrollRequest(Long user_id, String username, String image_base64, String image_format, Map<String, Object> metadata) {
    }

    private record EnrollResponse(String template_ref, String template_digest, String model_source, String model_version, BigDecimal threshold, Map<String, Object> raw_response) {
    }

    private record VerifyRequest(String template_ref, String template_digest, String image_base64, String image_format, BigDecimal threshold, Map<String, Object> metadata) {
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

