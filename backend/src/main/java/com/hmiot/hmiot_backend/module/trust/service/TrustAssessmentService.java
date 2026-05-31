package com.hmiot.hmiot_backend.module.trust.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.common.security.CurrentUser;
import com.hmiot.hmiot_backend.module.app.entity.AppResource;
import com.hmiot.hmiot_backend.module.app.service.AppResourceService;
import com.hmiot.hmiot_backend.module.integration.service.ExternalIntegrationCallService;
import com.hmiot.hmiot_backend.module.integration.service.UebaIntegrationClient;
import com.hmiot.hmiot_backend.module.integration.service.UebaIntegrationClient.AssessmentResult;
import com.hmiot.hmiot_backend.module.integration.service.UebaIntegrationClient.HealthResult;
import com.hmiot.hmiot_backend.module.policy.service.AuthPolicyService;
import com.hmiot.hmiot_backend.module.policy.service.SecurityBaselineService;
import com.hmiot.hmiot_backend.module.policy.vo.PolicyResolveResponse;
import com.hmiot.hmiot_backend.module.policy.vo.SecurityBaselineVO;
import com.hmiot.hmiot_backend.module.trust.dto.TrustAssessmentQueryRequest;
import com.hmiot.hmiot_backend.module.trust.dto.TrustAssessmentTriggerRequest;
import com.hmiot.hmiot_backend.module.trust.entity.TerminalEvent;
import com.hmiot.hmiot_backend.module.trust.entity.TrustAssessmentRequest;
import com.hmiot.hmiot_backend.module.trust.entity.TrustAssessmentResult;
import com.hmiot.hmiot_backend.module.trust.mapper.TrustAssessmentRequestMapper;
import com.hmiot.hmiot_backend.module.trust.mapper.TrustAssessmentResultMapper;
import com.hmiot.hmiot_backend.module.trust.vo.TrustAssessmentCheckResponse;
import com.hmiot.hmiot_backend.module.trust.vo.TrustAssessmentResultVO;
import com.hmiot.hmiot_backend.module.trust.vo.UebaHealthResponse;
import com.hmiot.hmiot_backend.module.user.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class TrustAssessmentService {
    public static final String ALLOW = "ALLOW";
    public static final String WARN = "WARN";
    public static final String STEP_UP = "STEP_UP";
    public static final String DENY = "DENY";

    private final TrustAssessmentRequestMapper trustAssessmentRequestMapper;
    private final TrustAssessmentResultMapper trustAssessmentResultMapper;
    private final TerminalEventService terminalEventService;
    private final SecurityBaselineService securityBaselineService;
    private final AppResourceService appResourceService;
    private final UebaIntegrationClient uebaIntegrationClient;
    private final ExternalIntegrationCallService externalIntegrationCallService;
    private final TrustAssessmentFileLogService trustAssessmentFileLogService;
    private final ObjectMapper objectMapper;

    public TrustAssessmentService(TrustAssessmentRequestMapper trustAssessmentRequestMapper, TrustAssessmentResultMapper trustAssessmentResultMapper, TerminalEventService terminalEventService, SecurityBaselineService securityBaselineService, AppResourceService appResourceService, UebaIntegrationClient uebaIntegrationClient, ExternalIntegrationCallService externalIntegrationCallService, TrustAssessmentFileLogService trustAssessmentFileLogService, ObjectMapper objectMapper) {
        this.trustAssessmentRequestMapper = trustAssessmentRequestMapper;
        this.trustAssessmentResultMapper = trustAssessmentResultMapper;
        this.terminalEventService = terminalEventService;
        this.securityBaselineService = securityBaselineService;
        this.appResourceService = appResourceService;
        this.uebaIntegrationClient = uebaIntegrationClient;
        this.externalIntegrationCallService = externalIntegrationCallService;
        this.trustAssessmentFileLogService = trustAssessmentFileLogService;
        this.objectMapper = objectMapper;
    }

    public UebaHealthResponse health() {
        LocalDateTime startedAt = LocalDateTime.now();
        try {
            HealthResult result = uebaIntegrationClient.health();
            externalIntegrationCallService.recordSuccess("UEBA_HEALTH", uebaIntegrationClient.targetSystem(), "UEBA4.2", Map.of(), Map.of("healthy", result.healthy(), "modelVersion", result.modelVersion()), startedAt);
            return new UebaHealthResponse(result.healthy(), uebaIntegrationClient.targetSystem(), result.modelVersion());
        } catch (RuntimeException exception) {
            externalIntegrationCallService.recordFailure("UEBA_HEALTH", uebaIntegrationClient.targetSystem(), "UEBA4.2", Map.of(), exception.getMessage(), startedAt);
            throw exception;
        }
    }

    @Transactional
    public TrustAssessmentCheckResponse check(TrustAssessmentTriggerRequest request, CurrentUser currentUser) {
        if (currentUser.terminalId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前用户缺少可信终端上下文，请重新登录");
        }
        AppResource app = resolveApp(request.appCode(), request.appResourceId());
        TrustAssessmentResultVO result = assess(app, currentUser, null, request.requestId(), false);
        return toCheckResponse(result);
    }

    @Transactional
    public TrustAssessmentResultVO ensureAccessTrusted(AppResource app, CurrentUser currentUser, PolicyResolveResponse policy) {
        TrustAssessmentResultVO result;
        try {
            result = assess(app, currentUser, policy, null, true);
        } catch (RuntimeException exception) {
            if (failClosed(app, policy)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "终端信任评估失败，高安全等级应用默认拒绝访问");
            }
            return degraded(app, currentUser, policy, exception.getMessage());
        }
        if (DENY.equals(result.decision())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, result.rejectReason() == null ? "当前终端风险过高，访问被拒绝" : result.rejectReason());
        }
        if (STEP_UP.equals(result.decision())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前终端风险较高，请完成二次认证或重新认证");
        }
        if (policy != null && result.trustLevel() != null && result.trustLevel() < policy.minTrustLevel()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前终端信任等级不足，请重新认证或增强认证");
        }
        return result;
    }

    public PageVO<TrustAssessmentResultVO> listAssessments(TrustAssessmentQueryRequest request) {
        LambdaQueryWrapper<TrustAssessmentRequest> requestWrapper = new LambdaQueryWrapper<TrustAssessmentRequest>()
                .orderByDesc(TrustAssessmentRequest::getRequestTime);
        if (request.userId() != null) {
            requestWrapper.eq(TrustAssessmentRequest::getUserId, request.userId());
        }
        if (request.terminalId() != null) {
            requestWrapper.eq(TrustAssessmentRequest::getTerminalId, request.terminalId());
        }
        if (request.appResourceId() != null) {
            requestWrapper.eq(TrustAssessmentRequest::getAppResourceId, request.appResourceId());
        }
        Page<TrustAssessmentRequest> page = trustAssessmentRequestMapper.selectPage(Page.of(request.pageCurrent(), request.pageSize()), requestWrapper);
        List<TrustAssessmentResultVO> records = page.getRecords().stream()
                .map(this::toLatestResultVO)
                .filter(vo -> !StringUtils.hasText(request.riskLevel()) || request.riskLevel().equals(vo.riskLevel()))
                .filter(vo -> !StringUtils.hasText(request.decision()) || request.decision().equals(vo.decision()))
                .toList();
        return new PageVO<>(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    public TrustAssessmentResultVO detail(Long resultId) {
        TrustAssessmentResult result = trustAssessmentResultMapper.selectById(resultId);
        if (result == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "信任评估结果不存在");
        }
        TrustAssessmentRequest request = trustAssessmentRequestMapper.selectById(result.getRequestId());
        if (request == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "信任评估请求不存在");
        }
        return toVO(request, result, true);
    }

    public Map<String, Object> safeSummary(TrustAssessmentResultVO result) {
        if (result == null) {
            return Map.of();
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("trustAssessmentId", result.id());
        summary.put("trustScore", result.trustScore() == null ? "" : result.trustScore());
        summary.put("trustLevel", result.trustLevel() == null ? "" : result.trustLevel());
        summary.put("riskLevel", result.riskLevel() == null ? "" : result.riskLevel());
        summary.put("trustDecision", result.decision() == null ? "" : result.decision());
        summary.put("assessedAt", result.assessedAt() == null ? "" : result.assessedAt());
        return summary;
    }

    private TrustAssessmentResultVO assess(AppResource app, CurrentUser currentUser, PolicyResolveResponse policy, String requestId, boolean forAccess) {
        String effectiveRequestId = StringUtils.hasText(requestId) ? requestId : UUID.randomUUID().toString();
        List<TerminalEvent> events = terminalEventService.recentEvents(currentUser.userId(), currentUser.terminalId(), 50);
        List<SecurityBaselineVO> baselines = securityBaselineService.listEnabledForClient(currentUser.clientType(), currentUser.terminalId());
        Map<String, Object> input = inputPayload(effectiveRequestId, app, currentUser, policy, events, baselines, forAccess);
        String requestLogPath = trustAssessmentFileLogService.writeRequestLog(effectiveRequestId, input);
        TrustAssessmentRequest assessmentRequest = new TrustAssessmentRequest();
        assessmentRequest.setRequestId(effectiveRequestId);
        assessmentRequest.setUserId(currentUser.userId());
        assessmentRequest.setTerminalId(currentUser.terminalId());
        assessmentRequest.setAppResourceId(app == null ? null : app.getId());
        assessmentRequest.setModelSource("UEBA4.2");
        assessmentRequest.setInputDigest(sha256(toJson(input)));
        assessmentRequest.setInputPayload(toJson(input));
        assessmentRequest.setRequestTime(LocalDateTime.now());
        trustAssessmentRequestMapper.insert(assessmentRequest);

        LocalDateTime startedAt = LocalDateTime.now();
        try {
            AssessmentResult uebaResult = uebaIntegrationClient.assessTrust(input);
            AssessmentResult adjustedResult = adjustDecision(uebaResult, baselines, app, policy);
            Map<String, Object> resultLogPayload = new LinkedHashMap<>();
            resultLogPayload.put("requestLogPath", requestLogPath);
            resultLogPayload.put("modelSource", adjustedResult.modelSource());
            resultLogPayload.put("modelVersion", adjustedResult.modelVersion());
            resultLogPayload.put("trustScore", adjustedResult.trustScore());
            resultLogPayload.put("trustLevel", adjustedResult.trustLevel());
            resultLogPayload.put("riskLevel", adjustedResult.riskLevel());
            resultLogPayload.put("decision", adjustedResult.decision());
            resultLogPayload.put("rejectReason", adjustedResult.rejectReason() == null ? "" : adjustedResult.rejectReason());
            resultLogPayload.put("anomaly", adjustedResult.anomaly());
            resultLogPayload.put("anomalyIndicators", adjustedResult.anomalyIndicators() == null ? Map.of() : adjustedResult.anomalyIndicators());
            resultLogPayload.put("alerts", adjustedResult.alerts() == null ? List.of() : adjustedResult.alerts());
            resultLogPayload.put("rawResponse", adjustedResult.rawResponse() == null ? Map.of() : adjustedResult.rawResponse());
            String resultLogPath = trustAssessmentFileLogService.writeResultLog(effectiveRequestId, resultLogPayload);
            externalIntegrationCallService.recordSuccess("UEBA_TRUST_ASSESSMENT", uebaIntegrationClient.targetSystem(), effectiveRequestId, requestSummary(assessmentRequest), responseSummary(uebaResult), startedAt);
            TrustAssessmentResult result = persistResult(assessmentRequest, adjustedResult, requestLogPath, resultLogPath);
            assessmentRequest.setModelSource(uebaResult.modelSource());
            assessmentRequest.setModelVersion(uebaResult.modelVersion());
            trustAssessmentRequestMapper.updateById(assessmentRequest);
            return toVO(assessmentRequest, result, false);
        } catch (RuntimeException exception) {
            externalIntegrationCallService.recordFailure("UEBA_TRUST_ASSESSMENT", uebaIntegrationClient.targetSystem(), effectiveRequestId, requestSummary(assessmentRequest), exception.getMessage(), startedAt);
            throw exception;
        }
    }

    private TrustAssessmentResultVO degraded(AppResource app, CurrentUser currentUser, PolicyResolveResponse policy, String failReason) {
        String requestId = UUID.randomUUID().toString();
        TrustAssessmentRequest assessmentRequest = new TrustAssessmentRequest();
        assessmentRequest.setRequestId(requestId);
        assessmentRequest.setUserId(currentUser.userId());
        assessmentRequest.setTerminalId(currentUser.terminalId());
        assessmentRequest.setAppResourceId(app == null ? null : app.getId());
        assessmentRequest.setModelSource("UEBA4.2");
        assessmentRequest.setInputDigest("DEGRADED");
        assessmentRequest.setInputPayload(toJson(Map.of("degraded", true, "failReason", failReason == null ? "" : failReason)));
        assessmentRequest.setRequestTime(LocalDateTime.now());
        trustAssessmentRequestMapper.insert(assessmentRequest);
        externalIntegrationCallService.recordFailure("TRUST_ASSESSMENT_DEGRADED", uebaIntegrationClient.targetSystem(), requestId, requestSummary(assessmentRequest), failReason, LocalDateTime.now());

        String requestLogPath = trustAssessmentFileLogService.writeRequestLog(requestId, Map.of("degraded", true, "failReason", failReason == null ? "" : failReason));
        String resultLogPath = trustAssessmentFileLogService.writeResultLog(requestId, Map.of("degraded", true, "failReason", failReason == null ? "" : failReason, "decision", WARN));
        TrustAssessmentResult result = new TrustAssessmentResult();
        result.setRequestId(assessmentRequest.getId());
        result.setTrustScore(BigDecimal.valueOf(policy == null ? 1 : Math.max(1, policy.minTrustLevel())));
        result.setTrustLevel(policy == null ? 1 : Math.max(1, policy.minTrustLevel()));
        result.setRiskLevel("UNKNOWN");
        result.setIsAnomaly(false);
        result.setAnomalyIndicators(toJson(Map.of()));
        result.setAlerts(toJson(List.of("UEBA 服务不可用，按低安全等级降级策略继续")));
        result.setDecision(WARN);
        result.setRejectReason(failReason);
        result.setRawResponse(toJson(Map.of("degraded", true, "failReason", failReason == null ? "" : failReason, "requestLogPath", requestLogPath, "resultLogPath", resultLogPath)));
        result.setAssessedAt(LocalDateTime.now());
        trustAssessmentResultMapper.insert(result);
        return toVO(assessmentRequest, result, false);
    }

    private AppResource resolveApp(String appCode, Long appResourceId) {
        if (StringUtils.hasText(appCode)) {
            return appResourceService.findEnabledByCode(appCode);
        }
        if (appResourceId != null) {
            AppResource app = appResourceService.findApp(appResourceId);
            if (!AppResourceService.ENABLED.equals(app.getStatus())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "应用资源不可用");
            }
            return app;
        }
        return null;
    }

    private Map<String, Object> inputPayload(String requestId, AppResource app, CurrentUser currentUser, PolicyResolveResponse policy, List<TerminalEvent> events, List<SecurityBaselineVO> baselines, boolean forAccess) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("requestId", requestId);
        input.put("userId", currentUser.userId());
        input.put("terminalId", currentUser.terminalId());
        input.put("clientType", currentUser.clientType() == null ? "" : currentUser.clientType());
        input.put("authMethod", currentUser.authMethod() == null ? "" : currentUser.authMethod());
        input.put("forAccess", forAccess);
        if (app != null) {
            input.put("appCode", app.getAppCode());
            input.put("appResourceId", app.getId());
            input.put("securityLevel", app.getSecurityLevel());
            input.put("requiredTrustLevel", app.getRequiredTrustLevel());
        }
        if (policy != null) {
            input.put("policyId", policy.policyId());
            input.put("policyCode", policy.policyCode() == null ? "" : policy.policyCode());
            input.put("minTrustLevel", policy.minTrustLevel());
            input.put("actionOnFail", policy.actionOnFail());
        }
        input.put("events", events.stream().map(event -> Map.of(
                "eventType", event.getEventType(),
                "eventLevel", event.getEventLevel(),
                "eventTime", event.getEventTime() == null ? "" : event.getEventTime(),
                "normalizedPayload", fromJson(event.getNormalizedPayload())
        )).toList());
        input.put("baselines", baselines.stream().map(baseline -> Map.of(
                "baselineId", baseline.id(),
                "baselineCode", baseline.baselineCode(),
                "riskLevel", baseline.riskLevel(),
                "actionOnHit", baseline.actionOnHit(),
                "ruleConfig", safeRuleConfig(baseline.ruleConfig())
        )).toList());
        return input;
    }

    private AssessmentResult adjustDecision(AssessmentResult result, List<SecurityBaselineVO> baselines, AppResource app, PolicyResolveResponse policy) {
        String decision = result.decision();
        String rejectReason = result.rejectReason();
        String marker = (result.rawResponse() == null ? "" : result.rawResponse().toString()).toLowerCase(Locale.ROOT);
        marker += " " + (result.anomalyIndicators() == null ? "" : result.anomalyIndicators().toString()).toLowerCase(Locale.ROOT);
        for (SecurityBaselineVO baseline : baselines) {
            String rule = baseline.ruleConfig() == null ? "" : baseline.ruleConfig().toLowerCase(Locale.ROOT);
            if (!rule.isBlank() && marker.contains(rule.replace("\"", "").replace("{", "").replace("}", ""))) {
                if (AuthPolicyService.DENY.equals(baseline.actionOnHit())) {
                    decision = DENY;
                    rejectReason = "命中安全基线拒绝策略：" + baseline.baselineName();
                } else if ("STEP_UP".equals(baseline.actionOnHit()) && !DENY.equals(decision)) {
                    decision = STEP_UP;
                    rejectReason = "命中安全基线增强认证策略：" + baseline.baselineName();
                } else if ("WARN".equals(baseline.actionOnHit()) && ALLOW.equals(decision)) {
                    decision = WARN;
                }
            }
        }
        if (policy != null && result.trustLevel() != null && result.trustLevel() < policy.minTrustLevel() && !DENY.equals(decision)) {
            decision = STEP_UP;
            rejectReason = "终端信任等级低于访问策略阈值";
        }
        if (app != null && app.getSecurityLevel() != null && app.getSecurityLevel() >= 3 && result.riskLevel() != null && result.riskLevel().equals("HIGH")) {
            decision = DENY;
            rejectReason = "高安全等级应用拒绝高风险终端访问";
        }
        return new AssessmentResult(result.requestId(), result.trustScore(), result.trustLevel(), result.riskLevel(), result.anomaly(), decision, rejectReason, result.modelSource(), result.modelVersion(), result.anomalyIndicators(), result.alerts(), result.rawResponse(), result.assessedAt());
    }

    private TrustAssessmentResult persistResult(TrustAssessmentRequest assessmentRequest, AssessmentResult assessmentResult, String requestLogPath, String resultLogPath) {
        TrustAssessmentResult result = new TrustAssessmentResult();
        result.setRequestId(assessmentRequest.getId());
        result.setTrustScore(assessmentResult.trustScore());
        result.setTrustLevel(assessmentResult.trustLevel());
        result.setRiskLevel(assessmentResult.riskLevel());
        result.setIsAnomaly(assessmentResult.anomaly());
        result.setAnomalyIndicators(toJson(assessmentResult.anomalyIndicators()));
        result.setAlerts(toJson(assessmentResult.alerts()));
        result.setDecision(assessmentResult.decision());
        result.setRejectReason(assessmentResult.rejectReason());
        Map<String, Object> rawResponse = new LinkedHashMap<>();
        rawResponse.putAll(assessmentResult.rawResponse() == null ? Map.of() : assessmentResult.rawResponse());
        rawResponse.put("requestLogPath", requestLogPath);
        rawResponse.put("resultLogPath", resultLogPath);
        result.setRawResponse(toJson(rawResponse));
        result.setAssessedAt(assessmentResult.assessedAt() == null ? LocalDateTime.now() : assessmentResult.assessedAt());
        trustAssessmentResultMapper.insert(result);
        return result;
    }

    private TrustAssessmentResultVO toLatestResultVO(TrustAssessmentRequest request) {
        TrustAssessmentResult result = trustAssessmentResultMapper.selectOne(new LambdaQueryWrapper<TrustAssessmentResult>()
                .eq(TrustAssessmentResult::getRequestId, request.getId())
                .orderByDesc(TrustAssessmentResult::getAssessedAt)
                .last("LIMIT 1"));
        if (result == null) {
            return new TrustAssessmentResultVO(null, request.getId(), request.getRequestId(), request.getUserId(), request.getTerminalId(), request.getAppResourceId(), request.getModelSource(), request.getModelVersion(), null, null, null, null, Map.of(), Map.of(), null, null, fromJson(request.getInputPayload()), Map.of(), request.getRequestTime(), null);
        }
        return toVO(request, result, true);
    }

    private TrustAssessmentResultVO toVO(TrustAssessmentRequest request, TrustAssessmentResult result, boolean includeSensitive) {
        return new TrustAssessmentResultVO(
                result.getId(),
                request.getId(),
                request.getRequestId(),
                request.getUserId(),
                request.getTerminalId(),
                request.getAppResourceId(),
                request.getModelSource(),
                request.getModelVersion(),
                result.getTrustScore(),
                result.getTrustLevel(),
                result.getRiskLevel(),
                result.getIsAnomaly(),
                fromJson(result.getAnomalyIndicators()),
                fromJson(result.getAlerts()),
                result.getDecision(),
                result.getRejectReason(),
                includeSensitive ? fromJson(request.getInputPayload()) : null,
                includeSensitive ? fromJson(result.getRawResponse()) : null,
                request.getRequestTime(),
                result.getAssessedAt()
        );
    }

    private TrustAssessmentCheckResponse toCheckResponse(TrustAssessmentResultVO result) {
        String decision = result.decision() == null ? ALLOW : result.decision();
        String requestLogPath = String.valueOf(result.rawResponse().getOrDefault("requestLogPath", ""));
        String resultLogPath = String.valueOf(result.rawResponse().getOrDefault("resultLogPath", ""));
        return new TrustAssessmentCheckResponse(
                result.id(),
                result.requestId(),
                ALLOW.equals(decision) || WARN.equals(decision),
                result.trustScore(),
                result.trustLevel(),
                result.riskLevel(),
                decision,
                decision,
                result.rejectReason(),
                result.assessedAt(),
                requestLogPath,
                resultLogPath
        );
    }

    private boolean failClosed(AppResource app, PolicyResolveResponse policy) {
        if (app != null && app.getSecurityLevel() != null && app.getSecurityLevel() >= 3) {
            return true;
        }
        return policy != null && policy.minTrustLevel() >= 2 && AuthPolicyService.DENY.equals(policy.actionOnFail());
    }

    private Map<String, Object> requestSummary(TrustAssessmentRequest request) {
        return Map.of(
                "requestId", request.getRequestId(),
                "userId", request.getUserId() == null ? "" : request.getUserId(),
                "terminalId", request.getTerminalId() == null ? "" : request.getTerminalId(),
                "appResourceId", request.getAppResourceId() == null ? "" : request.getAppResourceId()
        );
    }

    private Map<String, Object> responseSummary(AssessmentResult result) {
        return Map.of(
                "trustScore", result.trustScore(),
                "trustLevel", result.trustLevel(),
                "riskLevel", result.riskLevel(),
                "decision", result.decision(),
                "anomaly", result.anomaly()
        );
    }

    private String safeRuleConfig(String ruleConfig) {
        if (ruleConfig == null) {
            return "";
        }
        return ruleConfig.length() > 256 ? ruleConfig.substring(0, 256) : ruleConfig;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "信任评估输入摘要生成失败");
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "信任评估报文序列化失败");
        }
    }

    private Map<String, Object> fromJson(String value) {
        if (!StringUtils.hasText(value)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            return Map.of();
        }
    }
}
