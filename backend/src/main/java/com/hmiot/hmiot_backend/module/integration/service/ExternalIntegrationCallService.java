package com.hmiot.hmiot_backend.module.integration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmiot.hmiot_backend.module.integration.entity.ExternalIntegrationCall;
import com.hmiot.hmiot_backend.module.integration.mapper.ExternalIntegrationCallMapper;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class ExternalIntegrationCallService {
    private final ExternalIntegrationCallMapper externalIntegrationCallMapper;
    private final ObjectMapper objectMapper;

    public ExternalIntegrationCallService(ExternalIntegrationCallMapper externalIntegrationCallMapper, ObjectMapper objectMapper) {
        this.externalIntegrationCallMapper = externalIntegrationCallMapper;
        this.objectMapper = objectMapper;
    }

    public Long recordSuccess(String integrationType, String targetSystem, String businessKey, Map<String, Object> requestSummary, Map<String, Object> responseSummary, LocalDateTime startedAt) {
        ExternalIntegrationCall call = baseCall(integrationType, targetSystem, businessKey, requestSummary, startedAt);
        call.setResponseSummary(toJson(responseSummary));
        call.setCallResult("SUCCESS");
        call.setFinishedAt(LocalDateTime.now());
        externalIntegrationCallMapper.insert(call);
        return call.getId();
    }

    public void recordFailure(String integrationType, String targetSystem, String businessKey, Map<String, Object> requestSummary, String failReason, LocalDateTime startedAt) {
        ExternalIntegrationCall call = baseCall(integrationType, targetSystem, businessKey, requestSummary, startedAt);
        call.setCallResult("FAIL");
        call.setFailReason(failReason);
        call.setFinishedAt(LocalDateTime.now());
        externalIntegrationCallMapper.insert(call);
    }

    private ExternalIntegrationCall baseCall(String integrationType, String targetSystem, String businessKey, Map<String, Object> requestSummary, LocalDateTime startedAt) {
        ExternalIntegrationCall call = new ExternalIntegrationCall();
        call.setIntegrationType(integrationType);
        call.setTargetSystem(targetSystem);
        call.setRequestId(MDC.get("traceId"));
        call.setBusinessKey(businessKey);
        call.setRequestSummary(toJson(requestSummary));
        call.setStartedAt(startedAt);
        return call;
    }

    private String toJson(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }
}
