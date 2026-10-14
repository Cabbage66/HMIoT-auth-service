package com.hmiot.hmiot_backend.module.integration.service;

import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class UebaIntegrationClient {
    private final String uebaServiceUrl;
    private final RestClient restClient;

    public UebaIntegrationClient(@Value("${hmiot.integrations.ueba-service-url:mock://ueba}") String uebaServiceUrl, RestClient.Builder restClientBuilder) {
        this.uebaServiceUrl = uebaServiceUrl;
        this.restClient = uebaServiceUrl.startsWith("http://") || uebaServiceUrl.startsWith("https://")
                ? restClientBuilder.baseUrl(uebaServiceUrl).build()
                : null;
    }

    public HealthResult health() {
        if (uebaServiceUrl.startsWith("mock://")) {
            return new HealthResult(true, "mock-ueba-1.0");
        }
        try {
            HealthResponse response = restClient.get()
                    .uri("/health")
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                        throw new BusinessException(ErrorCode.BAD_REQUEST, "UEBA 服务健康检查失败");
                    })
                    .body(HealthResponse.class);
            if (response == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "UEBA 服务健康检查响应不完整");
            }
            return new HealthResult(response.healthy() == null || Boolean.TRUE.equals(response.healthy()), response.model_version() == null ? "unknown" : response.model_version());
        } catch (RuntimeException exception) {
            printMockFallback("健康检查", exception);
            return new HealthResult(true, "mock-ueba-1.0");
        }
    }

    public AssessmentResult assessTrust(Map<String, Object> input) {
        if (uebaServiceUrl.startsWith("mock://")) {
            return mockAssess(input);
        }
        try {
            AssessmentResponse response = restClient.post()
                    .uri("/trust/assess")
                    .body(input)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                        throw new BusinessException(ErrorCode.BAD_REQUEST, "UEBA 信任评估服务调用失败");
                    })
                    .body(AssessmentResponse.class);
            if (response == null || response.trust_score() == null || response.trust_level() == null || response.risk_level() == null || response.decision() == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "UEBA 信任评估服务响应不完整");
            }
            return new AssessmentResult(
                    response.request_id(),
                    response.trust_score(),
                    response.trust_level(),
                    normalizeRiskLevel(response.risk_level()),
                    Boolean.TRUE.equals(response.is_anomaly()),
                    normalizeDecision(response.decision()),
                    response.reject_reason(),
                    response.model_source() == null ? "UEBA4.2" : response.model_source(),
                    response.model_version(),
                    response.anomaly_indicators() == null ? Map.of() : response.anomaly_indicators(),
                    response.alerts() == null ? List.of() : response.alerts(),
                    response.raw_response() == null ? response.rawMap() : response.raw_response(),
                    LocalDateTime.now()
            );
        } catch (RuntimeException exception) {
            printMockFallback("信任评估", exception);
            return mockAssess(input);
        }
    }

    public AssessmentResult detectUser(Map<String, Object> input) {
        return assessDetection("/detect/user", input, "UEBA 用户级异常检测服务调用失败");
    }

    public AssessmentResult detectEvent(Map<String, Object> input) {
        return assessDetection("/detect/event", input, "UEBA 事件级异常检测服务调用失败");
    }

    public String targetSystem() {
        return uebaServiceUrl;
    }

    private AssessmentResult assessDetection(String uri, Map<String, Object> input, String failMessage) {
        if (uebaServiceUrl.startsWith("mock://")) {
            return mockAssess(input);
        }
        try {
            AssessmentResponse response = restClient.post()
                    .uri(uri)
                    .body(input)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                        throw new BusinessException(ErrorCode.BAD_REQUEST, failMessage);
                    })
                    .body(AssessmentResponse.class);
            if (response == null || response.risk_level() == null || response.decision() == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "UEBA 检测服务响应不完整");
            }
            BigDecimal score = response.trust_score() == null ? BigDecimal.valueOf(70) : response.trust_score();
            Integer level = response.trust_level() == null ? trustLevel(score) : response.trust_level();
            return new AssessmentResult(response.request_id(), score, level, normalizeRiskLevel(response.risk_level()), Boolean.TRUE.equals(response.is_anomaly()), normalizeDecision(response.decision()), response.reject_reason(), response.model_source() == null ? "UEBA4.2" : response.model_source(), response.model_version(), response.anomaly_indicators() == null ? Map.of() : response.anomaly_indicators(), response.alerts() == null ? List.of() : response.alerts(), response.raw_response() == null ? response.rawMap() : response.raw_response(), LocalDateTime.now());
        } catch (RuntimeException exception) {
            printMockFallback("异常检测", exception);
            return mockAssess(input);
        }
    }

    private void printMockFallback(String operation, RuntimeException exception) {
        System.out.println("[HMIoT][UEBA模拟] " + operation + "模型端口未开放或调用失败，已使用模拟评估结果。target=" + uebaServiceUrl + ", reason=" + (exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage()));
    }

    private AssessmentResult mockAssess(Map<String, Object> input) {
        String marker = input == null ? "" : input.toString().toLowerCase(Locale.ROOT);
        BigDecimal score;
        String riskLevel;
        String decision;
        boolean anomaly;
        String reason = null;
        if (marker.contains("critical") || marker.contains("malware") || marker.contains("tamper") || marker.contains("deny")) {
            score = BigDecimal.valueOf(20);
            riskLevel = "HIGH";
            decision = "DENY";
            anomaly = true;
            reason = "UEBA 检测到高风险终端行为";
        } else if (marker.contains("warn") || marker.contains("suspicious") || marker.contains("risk") || marker.contains("step_up")) {
            score = BigDecimal.valueOf(55);
            riskLevel = "MEDIUM";
            decision = "STEP_UP";
            anomaly = true;
            reason = "UEBA 检测到中风险终端行为";
        } else {
            score = BigDecimal.valueOf(90);
            riskLevel = "LOW";
            decision = "ALLOW";
            anomaly = false;
        }
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("mock", true);
        raw.put("trustScore", score);
        raw.put("riskLevel", riskLevel);
        raw.put("decision", decision);
        raw.put("requestId", input == null ? "" : input.getOrDefault("requestId", ""));
        return new AssessmentResult(input == null ? null : String.valueOf(input.getOrDefault("requestId", "")), score, trustLevel(score), riskLevel, anomaly, decision, reason, "mock-ueba", "mock-ueba-1.0", anomaly ? Map.of("terminalBehavior", riskLevel) : Map.of(), anomaly ? List.of(reason) : List.of(), raw, LocalDateTime.now());
    }

    private int trustLevel(BigDecimal score) {
        if (score.compareTo(BigDecimal.valueOf(80)) >= 0) {
            return 3;
        }
        if (score.compareTo(BigDecimal.valueOf(60)) >= 0) {
            return 2;
        }
        if (score.compareTo(BigDecimal.valueOf(40)) >= 0) {
            return 1;
        }
        return 0;
    }

    private String normalizeRiskLevel(String riskLevel) {
        return riskLevel == null ? "UNKNOWN" : riskLevel.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeDecision(String decision) {
        String normalized = decision == null ? "ALLOW" : decision.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ALLOW", "WARN", "STEP_UP", "DENY" -> normalized;
            default -> "DENY";
        };
    }

    private record HealthResponse(Boolean healthy, String model_version) {
    }

    private record AssessmentResponse(String request_id, BigDecimal trust_score, Integer trust_level, String risk_level, Boolean is_anomaly, String decision, String reject_reason, String model_source, String model_version, Map<String, Object> anomaly_indicators, List<Object> alerts, Map<String, Object> raw_response) {
        Map<String, Object> rawMap() {
            Map<String, Object> raw = new LinkedHashMap<>();
            raw.put("requestId", request_id == null ? "" : request_id);
            raw.put("trustScore", trust_score == null ? "" : trust_score);
            raw.put("trustLevel", trust_level == null ? "" : trust_level);
            raw.put("riskLevel", risk_level == null ? "" : risk_level);
            raw.put("decision", decision == null ? "" : decision);
            return raw;
        }
    }

    public record HealthResult(boolean healthy, String modelVersion) {
    }

    public record AssessmentResult(String requestId, BigDecimal trustScore, Integer trustLevel, String riskLevel, boolean anomaly, String decision, String rejectReason, String modelSource, String modelVersion, Map<String, Object> anomalyIndicators, List<Object> alerts, Map<String, Object> rawResponse, LocalDateTime assessedAt) {
    }
}
