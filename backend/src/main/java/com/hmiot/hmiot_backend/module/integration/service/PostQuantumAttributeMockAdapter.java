package com.hmiot.hmiot_backend.module.integration.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class PostQuantumAttributeMockAdapter {
    private final String attributeServiceUrl;

    public PostQuantumAttributeMockAdapter(@Value("${hmiot.integrations.post-quantum-attribute-url:mock://post-quantum-attribute}") String attributeServiceUrl) {
        this.attributeServiceUrl = attributeServiceUrl;
    }

    public AttributeResult fetchAttributes(Long userId, String username) {
        if (!attributeServiceUrl.startsWith("mock://")) {
            throw new UnsupportedOperationException("后量子属性真实接口尚未接入，请配置 mock://post-quantum-attribute 或补充 HTTP 客户端实现");
        }
        String department = userId % 2 == 0 ? "物联网安全部" : "人机物认证部";
        String position = userId % 2 == 0 ? "设备安全员" : "认证管理员";
        String version = "mock-" + Instant.now().toEpochMilli();
        Map<String, Object> attributes = Map.of(
                "clearance", userId % 2 == 0 ? "L2" : "L3",
                "source", "post-quantum-attribute-mock",
                "username", username,
                "syncPolicy", "stage-2-demo"
        );
        return new AttributeResult(department, position, version, "POST_QUANTUM_ATTRIBUTE", "pq-" + userId, attributes);
    }

    public String targetSystem() {
        return attributeServiceUrl;
    }

    public record AttributeResult(
            String department,
            String positionName,
            String attributeVersion,
            String externalSource,
            String externalUserId,
            Map<String, Object> externalAttributes
    ) {
    }
}
