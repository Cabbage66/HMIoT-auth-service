package com.hmiot.hmiot_backend.module.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.common.security.CurrentAdmin;
import com.hmiot.hmiot_backend.common.security.CurrentUser;
import com.hmiot.hmiot_backend.module.app.entity.AppResource;
import com.hmiot.hmiot_backend.module.app.service.AppResourceService;
import com.hmiot.hmiot_backend.module.app.vo.AppResourceVO;
import com.hmiot.hmiot_backend.module.binding.service.TerminalBindingValidationService;
import com.hmiot.hmiot_backend.module.biometric.entity.BiometricAuthRecord;
import com.hmiot.hmiot_backend.module.biometric.service.BiometricTemplateService;
import com.hmiot.hmiot_backend.module.device.entity.IotDeviceStatus;
import com.hmiot.hmiot_backend.module.device.service.IotDeviceStatusService;
import com.hmiot.hmiot_backend.module.policy.service.PolicyExecutionService;
import com.hmiot.hmiot_backend.module.policy.vo.PolicyResolveResponse;
import com.hmiot.hmiot_backend.module.ticket.config.TicketProperties;
import com.hmiot.hmiot_backend.module.ticket.dto.TicketIssueRequest;
import com.hmiot.hmiot_backend.module.ticket.dto.TicketVerifyRequest;
import com.hmiot.hmiot_backend.module.ticket.entity.IdentityTicket;
import com.hmiot.hmiot_backend.module.ticket.entity.TicketVerifyRecord;
import com.hmiot.hmiot_backend.module.ticket.mapper.IdentityTicketMapper;
import com.hmiot.hmiot_backend.module.ticket.mapper.TicketVerifyRecordMapper;
import com.hmiot.hmiot_backend.module.ticket.vo.IdentityTicketVO;
import com.hmiot.hmiot_backend.module.ticket.vo.TicketVerifyResponse;
import com.hmiot.hmiot_backend.module.trust.service.TrustAssessmentService;
import com.hmiot.hmiot_backend.module.trust.vo.TrustAssessmentResultVO;
import com.hmiot.hmiot_backend.module.user.entity.UserAccount;
import com.hmiot.hmiot_backend.module.user.mapper.UserAccountMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Service
public class IdentityTicketService {
    public static final String VALID = "VALID";
    public static final String REVOKED = "REVOKED";
    public static final String EXPIRED = "EXPIRED";
    private static final String SUCCESS = "SUCCESS";
    private static final String FAIL = "FAIL";
    private static final long BIOMETRIC_AUTH_VALID_SECONDS = 300;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final IdentityTicketMapper identityTicketMapper;
    private final TicketVerifyRecordMapper ticketVerifyRecordMapper;
    private final UserAccountMapper userAccountMapper;
    private final AppResourceService appResourceService;
    private final TicketSignatureService ticketSignatureService;
    private final TicketProperties ticketProperties;
    private final TerminalBindingValidationService terminalBindingValidationService;
    private final PolicyExecutionService policyExecutionService;
    private final BiometricTemplateService biometricTemplateService;
    private final IotDeviceStatusService iotDeviceStatusService;
    private final TrustAssessmentService trustAssessmentService;

    public IdentityTicketService(IdentityTicketMapper identityTicketMapper, TicketVerifyRecordMapper ticketVerifyRecordMapper, UserAccountMapper userAccountMapper, AppResourceService appResourceService, TicketSignatureService ticketSignatureService, TicketProperties ticketProperties, TerminalBindingValidationService terminalBindingValidationService, PolicyExecutionService policyExecutionService, BiometricTemplateService biometricTemplateService, IotDeviceStatusService iotDeviceStatusService, TrustAssessmentService trustAssessmentService) {
        this.identityTicketMapper = identityTicketMapper;
        this.ticketVerifyRecordMapper = ticketVerifyRecordMapper;
        this.userAccountMapper = userAccountMapper;
        this.appResourceService = appResourceService;
        this.ticketSignatureService = ticketSignatureService;
        this.ticketProperties = ticketProperties;
        this.terminalBindingValidationService = terminalBindingValidationService;
        this.policyExecutionService = policyExecutionService;
        this.biometricTemplateService = biometricTemplateService;
        this.iotDeviceStatusService = iotDeviceStatusService;
        this.trustAssessmentService = trustAssessmentService;
    }

    @Transactional
    public IdentityTicketVO issueTicket(TicketIssueRequest request, CurrentUser currentUser) {
        AppResource app = appResourceService.findEnabledByCode(request.appCode());
        UserAccount user = findUser(currentUser.userId());
        ensureCurrentTerminal(request, currentUser);
        terminalBindingValidationService.validateUserTerminalForAccess(user.getId(), user.getUsername(), currentUser.terminalId(), app.getAppCode(), null, null);
        int trustLevel = resolveTrustLevel(currentUser.authMethod(), request.trustLevel());
        PolicyResolveResponse policy = policyExecutionService.ensurePolicyAllowsAccess(app, currentUser, trustLevel, currentUser.authMethod());
        BiometricAuthRecord voiceprintRecord = ensureVoiceprintSatisfied(policy, request, currentUser);
        BiometricAuthRecord dynamicBiometricRecord = ensureDynamicBiometricSatisfied(policy, request, currentUser);
        IotDeviceStatus iotDeviceStatus = ensureIotDeviceSatisfied(policy, currentUser);
        TrustAssessmentResultVO trustAssessment = trustAssessmentService.ensureAccessTrusted(app, currentUser, policy);
        LocalDateTime issuedAt = LocalDateTime.now();
        LocalDateTime expiresAt = issuedAt.plusSeconds(policy.sessionTtlSeconds());
        String ticketId = UUID.randomUUID().toString();
        BigDecimal trustScore = request.trustScore() == null ? BigDecimal.valueOf(trustLevel) : request.trustScore();
        String audienceSystem = StringUtils.hasText(request.audienceSystem()) ? request.audienceSystem() : ticketProperties.getIssuerSystem();
        Map<String, Object> payload = payload(ticketId, user, app, currentUser, trustLevel, trustScore, audienceSystem, issuedAt, expiresAt, voiceprintRecord, dynamicBiometricRecord, iotDeviceStatus, trustAssessment);
        String ticketPayload = ticketSignatureService.toPayload(payload);

        IdentityTicket ticket = new IdentityTicket();
        ticket.setTicketId(ticketId);
        ticket.setUserId(user.getId());
        ticket.setTerminalId(currentUser.terminalId());
        ticket.setIssuerSystem(ticketProperties.getIssuerSystem());
        ticket.setAudienceSystem(audienceSystem);
        ticket.setTrustLevel(trustLevel);
        ticket.setTrustScore(trustScore);
        ticket.setSignatureDigest(ticketSignatureService.sign(ticketPayload));
        ticket.setTicketPayload(ticketPayload);
        ticket.setStatus(VALID);
        ticket.setIssuedAt(issuedAt);
        ticket.setExpiresAt(expiresAt);
        identityTicketMapper.insert(ticket);
        return toVO(ticket, user, app, ticketPayload);
    }

    @Transactional
    public TicketVerifyResponse verifyTicket(TicketVerifyRequest request, String requestIp) {
        AppResource app = null;
        try {
            app = appResourceService.findEnabledByCode(request.appCode());
            IdentityTicket ticket = findByTicketId(request.ticketId());
            UserAccount user = findUser(ticket.getUserId());
            Map<String, Object> payload = ticketSignatureService.parsePayload(request.ticketPayload());
            validateTicket(ticket, payload, request, app);
            recordVerify(request.ticketId(), request.verifierSystem(), app.getId(), SUCCESS, null, requestIp);
            return new TicketVerifyResponse(true, SUCCESS, "票据验证通过", ticket.getTicketId(), user.getId(), user.getUsername(), user.getRealName(), ticket.getIssuerSystem(), ticket.getAudienceSystem(), request.verifierSystem(), app.getAppCode(), ticket.getTrustLevel(), ticket.getTrustScore(), appResourceService.toVO(app));
        } catch (BusinessException exception) {
            recordVerify(request.ticketId(), request.verifierSystem(), app == null ? null : app.getId(), FAIL, exception.getMessage(), requestIp);
            return new TicketVerifyResponse(false, exception.getErrorCode().getCode(), exception.getMessage(), request.ticketId(), null, null, null, null, null, request.verifierSystem(), request.appCode(), null, null, app == null ? null : appResourceService.toVO(app));
        }
    }

    @Transactional
    public IdentityTicketVO refreshTicket(String ticketId, CurrentUser currentUser) {
        IdentityTicket ticket = findByTicketId(ticketId);
        ensureTicketOwner(ticket, currentUser);
        ensureTicketTerminal(ticket, currentUser);
        ensureRefreshable(ticket);
        AppResource app = findAppFromPayload(ticket);
        UserAccount user = findUser(ticket.getUserId());
        terminalBindingValidationService.validateUserTerminalForAccess(user.getId(), user.getUsername(), currentUser.terminalId(), app.getAppCode(), ticket.getTicketId(), null);
        PolicyResolveResponse policy = policyExecutionService.ensurePolicyAllowsAccess(app, currentUser, ticket.getTrustLevel(), currentUser.authMethod());
        IotDeviceStatus iotDeviceStatus = ensureIotDeviceSatisfied(policy, currentUser);
        TrustAssessmentResultVO trustAssessment = trustAssessmentService.ensureAccessTrusted(app, currentUser, policy);
        LocalDateTime issuedAt = LocalDateTime.now();
        LocalDateTime expiresAt = issuedAt.plus(ticketProperties.getExpiration());
        Map<String, Object> payload = payload(ticket.getTicketId(), user, app, currentUser, ticket.getTrustLevel(), ticket.getTrustScore(), ticket.getAudienceSystem(), issuedAt, expiresAt, null, null, iotDeviceStatus, trustAssessment);
        String ticketPayload = ticketSignatureService.toPayload(payload);
        ticket.setTicketPayload(ticketPayload);
        ticket.setSignatureDigest(ticketSignatureService.sign(ticketPayload));
        ticket.setIssuedAt(issuedAt);
        ticket.setExpiresAt(expiresAt);
        ticket.setStatus(VALID);
        identityTicketMapper.updateById(ticket);
        return toVO(ticket, user, app, ticketPayload);
    }

    @Transactional
    public IdentityTicketVO keepAlive(String ticketId, CurrentUser currentUser) {
        IdentityTicket ticket = findByTicketId(ticketId);
        ensureTicketOwner(ticket, currentUser);
        ensureTicketTerminal(ticket, currentUser);
        ensureUsable(ticket);
        UserAccount user = findUser(ticket.getUserId());
        AppResource app = findAppFromPayload(ticket);
        PolicyResolveResponse policy = policyExecutionService.ensurePolicyAllowsAccess(app, currentUser, ticket.getTrustLevel(), currentUser.authMethod());
        ensureIotDeviceSatisfied(policy, currentUser);
        trustAssessmentService.ensureAccessTrusted(app, currentUser, policy);
        terminalBindingValidationService.validateUserTerminalForAccess(user.getId(), user.getUsername(), currentUser.terminalId(), app.getAppCode(), ticket.getTicketId(), null);
        return toVO(ticket, user, app, ticket.getTicketPayload());
    }

    @Transactional
    public IdentityTicketVO revokeTicket(String ticketId) {
        IdentityTicket ticket = findByTicketId(ticketId);
        ticket.setStatus(REVOKED);
        ticket.setRevokedAt(LocalDateTime.now());
        identityTicketMapper.updateById(ticket);
        UserAccount user = findUser(ticket.getUserId());
        AppResource app = findAppFromPayload(ticket);
        return toVO(ticket, user, app, ticket.getTicketPayload());
    }

    public IdentityTicketVO requireExistingValidTicket(TicketIssueRequest request, CurrentUser currentUser) {
        AppResource app = appResourceService.findEnabledByCode(request.appCode());
        ensureCurrentTerminal(request, currentUser);
        terminalBindingValidationService.validateUserTerminalForAccess(currentUser.userId(), currentUser.username(), currentUser.terminalId(), app.getAppCode(), null, null);
        int currentTrustLevel = resolveTrustLevel(currentUser.authMethod(), request.trustLevel());
        PolicyResolveResponse policy = policyExecutionService.ensurePolicyAllowsAccess(app, currentUser, currentTrustLevel, currentUser.authMethod());
        ensureVoiceprintSatisfied(policy, request, currentUser);
        ensureDynamicBiometricSatisfied(policy, request, currentUser);
        ensureIotDeviceSatisfied(policy, currentUser);
        trustAssessmentService.ensureAccessTrusted(app, currentUser, policy);
        IdentityTicket ticket = identityTicketMapper.selectOne(new LambdaQueryWrapper<IdentityTicket>()
                .eq(IdentityTicket::getUserId, currentUser.userId())
                .eq(IdentityTicket::getTerminalId, currentUser.terminalId())
                .eq(IdentityTicket::getAudienceSystem, StringUtils.hasText(request.audienceSystem()) ? request.audienceSystem() : ticketProperties.getIssuerSystem())
                .eq(IdentityTicket::getStatus, VALID)
                .gt(IdentityTicket::getExpiresAt, LocalDateTime.now())
                .orderByDesc(IdentityTicket::getIssuedAt)
                .last("LIMIT 1"));
        if (ticket == null) {
            return issueTicket(request, currentUser);
        }
        Map<String, Object> payload = ticketSignatureService.parsePayload(ticket.getTicketPayload());
        if (!request.appCode().equals(payload.get("appCode"))) {
            return issueTicket(request, currentUser);
        }
        ensureTicketTerminal(ticket, currentUser);
        UserAccount user = findUser(ticket.getUserId());
        terminalBindingValidationService.validateUserTerminalForAccess(user.getId(), user.getUsername(), currentUser.terminalId(), app.getAppCode(), ticket.getTicketId(), null);
        if (ticket.getTrustLevel() < currentTrustLevel) {
            return issueTicket(request, currentUser);
        }
        return toVO(ticket, user, app, ticket.getTicketPayload());
    }

    public CurrentUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser currentUser)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前用户未登录");
        }
        return currentUser;
    }

    public Object currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前用户未登录");
        }
        return authentication.getPrincipal();
    }

    public void ensureCanRevoke(IdentityTicketVO ticket, Object principal) {
        if (principal instanceof CurrentUser currentUser && currentUser.userId().equals(ticket.userId())) {
            return;
        }
        if (principal instanceof CurrentAdmin) {
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "无权失效该票据");
    }

    public IdentityTicketVO getTicket(String ticketId) {
        IdentityTicket ticket = findByTicketId(ticketId);
        return toVO(ticket, findUser(ticket.getUserId()), findAppFromPayload(ticket), ticket.getTicketPayload());
    }

    private void validateTicket(IdentityTicket ticket, Map<String, Object> payload, TicketVerifyRequest request, AppResource app) {
        if (!VALID.equals(ticket.getStatus())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "票据已失效");
        }
        if (LocalDateTime.now().isAfter(ticket.getExpiresAt())) {
            ticket.setStatus(EXPIRED);
            identityTicketMapper.updateById(ticket);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "票据已过期");
        }
        if (!ticket.getTicketPayload().equals(request.ticketPayload()) || !ticketSignatureService.matches(request.ticketPayload(), ticket.getSignatureDigest())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "票据签名无效或载荷被篡改");
        }
        if (!request.appCode().equals(payload.get("appCode")) || !app.getId().equals(asLong(payload.get("appResourceId")))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "票据应用范围不匹配");
        }
        if (StringUtils.hasText(ticket.getAudienceSystem()) && !ticket.getAudienceSystem().equals(request.verifierSystem())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "票据受众系统不匹配");
        }
        int requiredTrustLevel = request.requiredTrustLevel() == null ? app.getRequiredTrustLevel() : Math.max(request.requiredTrustLevel(), app.getRequiredTrustLevel());
        if (ticket.getTrustLevel() < requiredTrustLevel) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "票据信任等级不足，请重新认证或增强认证");
        }
    }

    private void recordVerify(String ticketId, String verifierSystem, Long appResourceId, String result, String failReason, String requestIp) {
        TicketVerifyRecord record = new TicketVerifyRecord();
        record.setTicketId(ticketId);
        record.setVerifierSystem(verifierSystem);
        record.setAppResourceId(appResourceId);
        record.setVerifyResult(result);
        record.setFailReason(failReason);
        record.setRequestIp(requestIp);
        record.setVerifiedAt(LocalDateTime.now());
        ticketVerifyRecordMapper.insert(record);
    }

    private void ensureTicketOwner(IdentityTicket ticket, CurrentUser currentUser) {
        if (!ticket.getUserId().equals(currentUser.userId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作该票据");
        }
    }

    private void ensureCurrentTerminal(TicketIssueRequest request, CurrentUser currentUser) {
        if (currentUser.terminalId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前用户缺少可信终端上下文，请重新登录");
        }
        if (request.terminalId() != null && !request.terminalId().equals(currentUser.terminalId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "请求终端与当前登录终端不一致");
        }
    }

    private void ensureTicketTerminal(IdentityTicket ticket, CurrentUser currentUser) {
        if (ticket.getTerminalId() == null || currentUser.terminalId() == null || !ticket.getTerminalId().equals(currentUser.terminalId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "票据终端与当前登录终端不一致");
        }
    }

    private BiometricAuthRecord ensureVoiceprintSatisfied(PolicyResolveResponse policy, TicketIssueRequest request, CurrentUser currentUser) {
        if (!policy.authChain().contains(BiometricTemplateService.VOICEPRINT)) {
            return null;
        }
        if (request.voiceprintAuthRecordId() == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前访问策略要求完成声纹认证");
        }
        BiometricAuthRecord record = biometricTemplateService.findSuccessfulVoiceprintRecord(request.voiceprintAuthRecordId(), currentUser.userId());
        if (record.getCreatedAt() == null || record.getCreatedAt().isBefore(LocalDateTime.now().minusSeconds(BIOMETRIC_AUTH_VALID_SECONDS))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "声纹认证已过期，请重新验证");
        }
        return record;
    }

    private BiometricAuthRecord ensureDynamicBiometricSatisfied(PolicyResolveResponse policy, TicketIssueRequest request, CurrentUser currentUser) {
        if (!policy.authChain().contains(BiometricTemplateService.DYNAMIC_BIOMETRIC)) {
            return null;
        }
        if (request.dynamicBiometricAuthRecordId() == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前访问策略要求完成动态生物认证");
        }
        BiometricAuthRecord record = biometricTemplateService.findSuccessfulDynamicBiometricRecord(request.dynamicBiometricAuthRecordId(), currentUser.userId());
        if (record.getCreatedAt() == null || record.getCreatedAt().isBefore(LocalDateTime.now().minusSeconds(BIOMETRIC_AUTH_VALID_SECONDS))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "动态生物认证已过期，请重新验证");
        }
        return record;
    }

    private IotDeviceStatus ensureIotDeviceSatisfied(PolicyResolveResponse policy, CurrentUser currentUser) {
        if (!policy.authChain().contains(IotDeviceStatusService.IOT_DEVICE)) {
            return null;
        }
        return iotDeviceStatusService.ensureCurrentTerminalDeviceTrusted(currentUser);
    }

    private void ensureRefreshable(IdentityTicket ticket) {
        if (!VALID.equals(ticket.getStatus())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "票据已失效");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(ticket.getExpiresAt().plus(ticketProperties.getRefreshWindow()))) {
            ticket.setStatus(EXPIRED);
            identityTicketMapper.updateById(ticket);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "票据已超过刷新窗口");
        }
    }

    private void ensureUsable(IdentityTicket ticket) {
        if (!VALID.equals(ticket.getStatus())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "票据已失效");
        }
        if (LocalDateTime.now().isAfter(ticket.getExpiresAt())) {
            ticket.setStatus(EXPIRED);
            identityTicketMapper.updateById(ticket);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "票据已过期");
        }
    }

    private IdentityTicket findByTicketId(String ticketId) {
        IdentityTicket ticket = identityTicketMapper.selectOne(new LambdaQueryWrapper<IdentityTicket>().eq(IdentityTicket::getTicketId, ticketId));
        if (ticket == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "票据不存在");
        }
        return ticket;
    }

    private UserAccount findUser(Long userId) {
        UserAccount user = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getId, userId)
                .eq(UserAccount::getDeleted, false));
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private int resolveTrustLevel(String authMethod, Integer requestedTrustLevel) {
        int maxTrustLevel = "USB_LOGIN".equals(authMethod)
                || "CERTIFICATE_LOGIN".equals(authMethod)
                || "FINGERPRINT_LOGIN".equals(authMethod)
                || "FACE_LOGIN".equals(authMethod)
                || "VOICEPRINT_LOGIN".equals(authMethod) ? 2 : 1;
        if (requestedTrustLevel == null) {
            return maxTrustLevel;
        }
        if (requestedTrustLevel < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "信任等级不能小于0");
        }
        return Math.min(requestedTrustLevel, maxTrustLevel);
    }

    private Map<String, Object> payload(String ticketId, UserAccount user, AppResource app, CurrentUser currentUser, Integer trustLevel, BigDecimal trustScore, String audienceSystem, LocalDateTime issuedAt, LocalDateTime expiresAt, BiometricAuthRecord voiceprintRecord, BiometricAuthRecord dynamicBiometricRecord, IotDeviceStatus iotDeviceStatus, TrustAssessmentResultVO trustAssessment) {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("ticketId", ticketId);
        payload.put("userId", user.getId());
        payload.put("username", user.getUsername());
        payload.put("realName", user.getRealName() == null ? "" : user.getRealName());
        payload.put("issuerSystem", ticketProperties.getIssuerSystem());
        payload.put("audienceSystem", audienceSystem);
        payload.put("appCode", app.getAppCode());
        payload.put("appResourceId", app.getId());
        payload.put("terminalId", currentUser.terminalId());
        payload.put("terminalCode", currentUser.terminalCode() == null ? "" : currentUser.terminalCode());
        payload.put("clientType", currentUser.clientType() == null ? "" : currentUser.clientType());
        payload.put("trustLevel", trustLevel);
        payload.put("trustScore", trustScore);
        payload.put("authMethod", currentUser.authMethod() == null ? "UNKNOWN" : currentUser.authMethod());
        if (voiceprintRecord != null) {
            payload.put("voiceprintAuthRecordId", voiceprintRecord.getId());
            payload.put("voiceprintAuthAt", voiceprintRecord.getCreatedAt() == null ? "" : voiceprintRecord.getCreatedAt().format(FORMATTER));
        }
        if (dynamicBiometricRecord != null) {
            payload.put("dynamicBiometricAuthRecordId", dynamicBiometricRecord.getId());
            payload.put("dynamicBiometricAuthAt", dynamicBiometricRecord.getCreatedAt() == null ? "" : dynamicBiometricRecord.getCreatedAt().format(FORMATTER));
        }
        payload.putAll(formattedIotSummary(iotDeviceStatus));
        payload.putAll(formattedTrustSummary(trustAssessment));
        payload.put("issuedAt", issuedAt.format(FORMATTER));
        payload.put("expiresAt", expiresAt.format(FORMATTER));
        return payload;
    }

    private Map<String, Object> formattedIotSummary(IotDeviceStatus status) {
        if (status == null) {
            return Map.of();
        }
        Map<String, Object> summary = new java.util.LinkedHashMap<>(iotDeviceStatusService.safeSummary(status));
        if (status.getLastReportAt() != null) {
            summary.put("iotDeviceStatusAt", status.getLastReportAt().format(FORMATTER));
        }
        return summary;
    }

    private Map<String, Object> formattedTrustSummary(TrustAssessmentResultVO trustAssessment) {
        if (trustAssessment == null) {
            return Map.of();
        }
        Map<String, Object> summary = new java.util.LinkedHashMap<>(trustAssessmentService.safeSummary(trustAssessment));
        if (trustAssessment.assessedAt() != null) {
            summary.put("trustAssessedAt", trustAssessment.assessedAt().format(FORMATTER));
        }
        return summary;
    }

    private IdentityTicketVO toVO(IdentityTicket ticket, UserAccount user, AppResource app, String payload) {
        long expiresInSeconds = Math.max(0, Duration.between(LocalDateTime.now(), ticket.getExpiresAt()).toSeconds());
        return new IdentityTicketVO(ticket.getTicketId(), payload, user.getId(), user.getUsername(), user.getRealName(), ticket.getTerminalId(), stringPayload(payload, "terminalCode"), stringPayload(payload, "clientType"), ticket.getIssuerSystem(), ticket.getAudienceSystem(), app.getAppCode(), app.getId(), ticket.getTrustLevel(), ticket.getTrustScore(), stringPayload(payload, "authMethod"), ticket.getStatus(), ticket.getIssuedAt(), ticket.getExpiresAt(), expiresInSeconds, appResourceService.toVO(app));
    }

    private AppResource findAppFromPayload(IdentityTicket ticket) {
        String appCode = stringPayload(ticket.getTicketPayload(), "appCode");
        return appResourceService.findEnabledByCode(appCode);
    }

    private String stringPayload(String payload, String key) {
        Object value = ticketSignatureService.parsePayload(payload).get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }
}
