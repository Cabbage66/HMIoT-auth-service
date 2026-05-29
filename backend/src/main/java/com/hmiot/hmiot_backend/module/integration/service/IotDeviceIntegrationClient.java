package com.hmiot.hmiot_backend.module.integration.service;

import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.module.device.dto.IotDeviceStatusReportRequest;
import com.hmiot.hmiot_backend.module.device.entity.DeviceEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class IotDeviceIntegrationClient {
    private final String lpwanServiceUrl;
    private final RestClient restClient;

    public IotDeviceIntegrationClient(@Value("${hmiot.integrations.lpwan-service-url:mock://lpwan}") String lpwanServiceUrl, RestClient.Builder restClientBuilder) {
        this.lpwanServiceUrl = lpwanServiceUrl;
        this.restClient = lpwanServiceUrl.startsWith("http://") || lpwanServiceUrl.startsWith("https://")
                ? restClientBuilder.baseUrl(lpwanServiceUrl).build()
                : null;
    }

    public HealthResult health() {
        if (lpwanServiceUrl.startsWith("mock://")) {
            return new HealthResult(true, "mock-lpwan-1.0");
        }
        HealthResponse response = restClient.get()
                .uri("/health")
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "物联网设备状态服务健康检查失败");
                })
                .body(HealthResponse.class);
        if (response == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "物联网设备状态服务健康检查响应不完整");
        }
        return new HealthResult(response.healthy() == null || Boolean.TRUE.equals(response.healthy()), response.model_version() == null ? "unknown" : response.model_version());
    }

    public StatusResult queryStatus(DeviceEntity device, String requestId) {
        String effectiveRequestId = requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId;
        if (lpwanServiceUrl.startsWith("mock://")) {
            String marker = ((device.getDeviceCode() == null ? "" : device.getDeviceCode()) + " " + effectiveRequestId).toLowerCase();
            String connectStatus = marker.contains("offline") ? "OFFLINE" : "ONLINE";
            String trustStatus = marker.contains("untrusted") ? "UNTRUSTED" : marker.contains("suspect") ? "SUSPECT" : "TRUSTED";
            return new StatusResult(effectiveRequestId, connectStatus, trustStatus, LocalDateTime.now(), "mock-lpwan", "mock-lpwan-1.0", Map.of(
                    "mock", true,
                    "deviceEntityId", device.getId(),
                    "deviceCode", device.getDeviceCode(),
                    "connectStatus", connectStatus,
                    "trustStatus", trustStatus
            ));
        }
        QueryResponse response = restClient.post()
                .uri("/iot-device-status/query")
                .body(new QueryRequest(device.getId(), device.getDeviceCode(), device.getDeviceFingerprint(), effectiveRequestId))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "物联网设备状态查询服务调用失败");
                })
                .body(QueryResponse.class);
        if (response == null || response.connect_status() == null || response.trust_status() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "物联网设备状态查询服务响应不完整");
        }
        return new StatusResult(response.request_id() == null ? effectiveRequestId : response.request_id(), response.connect_status(), response.trust_status(), response.report_at() == null ? LocalDateTime.now() : response.report_at(), response.model_source() == null ? "lpwan" : response.model_source(), response.model_version(), response.raw_response() == null ? Map.of() : response.raw_response());
    }

    public StatusResult normalizeReport(IotDeviceStatusReportRequest request) {
        Map<String, Object> rawResponse = new LinkedHashMap<>();
        if (request.payload() != null) {
            rawResponse.putAll(request.payload());
        }
        rawResponse.put("reportSource", "callback");
        return new StatusResult(request.requestId(), request.connectStatus(), request.trustStatus(), request.reportAt() == null ? LocalDateTime.now() : request.reportAt(), "lpwan-report", null, rawResponse);
    }

    public String targetSystem() {
        return lpwanServiceUrl;
    }

    private record HealthResponse(Boolean healthy, String model_version) {
    }

    private record QueryRequest(Long device_entity_id, String device_code, String device_fingerprint, String request_id) {
    }

    private record QueryResponse(String request_id, String connect_status, String trust_status, LocalDateTime report_at, String model_source, String model_version, Map<String, Object> raw_response) {
    }

    public record HealthResult(boolean healthy, String modelVersion) {
    }

    public record StatusResult(String requestId, String connectStatus, String trustStatus, LocalDateTime reportAt, String modelSource, String modelVersion, Map<String, Object> rawResponse) {
    }
}
