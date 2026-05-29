package com.hmiot.hmiot_backend.module.ticket.service;

import com.hmiot.hmiot_backend.common.security.CurrentUser;
import com.hmiot.hmiot_backend.module.app.entity.AppResource;
import com.hmiot.hmiot_backend.module.app.service.AppResourceService;
import com.hmiot.hmiot_backend.module.ticket.config.TicketProperties;
import com.hmiot.hmiot_backend.module.ticket.dto.SsoAccessRequest;
import com.hmiot.hmiot_backend.module.ticket.dto.SsoValidateRequest;
import com.hmiot.hmiot_backend.module.ticket.dto.TicketIssueRequest;
import com.hmiot.hmiot_backend.module.ticket.dto.TicketVerifyRequest;
import com.hmiot.hmiot_backend.module.ticket.entity.SsoRecord;
import com.hmiot.hmiot_backend.module.ticket.mapper.SsoRecordMapper;
import com.hmiot.hmiot_backend.module.ticket.vo.IdentityTicketVO;
import com.hmiot.hmiot_backend.module.ticket.vo.SsoAccessResponse;
import com.hmiot.hmiot_backend.module.ticket.vo.SsoValidateResponse;
import com.hmiot.hmiot_backend.module.ticket.vo.TicketVerifyResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SsoService {
    private static final String SUCCESS = "SUCCESS";
    private static final String FAIL = "FAIL";

    private final SsoRecordMapper ssoRecordMapper;
    private final IdentityTicketService identityTicketService;
    private final AppResourceService appResourceService;
    private final TicketProperties ticketProperties;

    public SsoService(SsoRecordMapper ssoRecordMapper, IdentityTicketService identityTicketService, AppResourceService appResourceService, TicketProperties ticketProperties) {
        this.ssoRecordMapper = ssoRecordMapper;
        this.identityTicketService = identityTicketService;
        this.appResourceService = appResourceService;
        this.ticketProperties = ticketProperties;
    }

    @Transactional
    public SsoAccessResponse access(SsoAccessRequest request, CurrentUser currentUser) {
        AppResource app = appResourceService.findEnabledByCode(request.appCode());
        try {
            IdentityTicketVO ticket = identityTicketService.requireExistingValidTicket(new TicketIssueRequest(request.appCode(), request.targetSystem(), null, null, null, null, null), currentUser);
            record(currentUser.userId(), ticketProperties.getIssuerSystem(), request.targetSystem(), app.getId(), ticket.ticketId(), SUCCESS, null);
            return new SsoAccessResponse(true, "单点登录访问已确认", ticketProperties.getIssuerSystem(), request.targetSystem(), app.getAppCode(), app.getAccessUrl(), ticket);
        } catch (RuntimeException exception) {
            record(currentUser.userId(), ticketProperties.getIssuerSystem(), request.targetSystem(), app.getId(), null, FAIL, terminalFailReason(currentUser, exception.getMessage()));
            throw exception;
        }
    }

    @Transactional
    public SsoValidateResponse validate(SsoValidateRequest request, String requestIp) {
        AppResource app = null;
        TicketVerifyResponse verifyResponse = null;
        try {
            app = appResourceService.findEnabledByCode(request.appCode());
            verifyResponse = identityTicketService.verifyTicket(new TicketVerifyRequest(request.ticketId(), request.ticketPayload(), request.appCode(), request.targetSystem(), request.requiredTrustLevel()), requestIp);
            record(verifyResponse.userId(), request.sourceSystem(), request.targetSystem(), app.getId(), request.ticketId(), verifyResponse.verified() ? SUCCESS : FAIL, verifyResponse.verified() ? null : verifyResponse.message());
            return new SsoValidateResponse(verifyResponse.verified(), verifyResponse.verified() ? "SSO校验通过" : verifyResponse.message(), request.sourceSystem(), request.targetSystem(), request.appCode(), verifyResponse);
        } catch (RuntimeException exception) {
            record(null, request.sourceSystem(), request.targetSystem(), app == null ? null : app.getId(), request.ticketId(), FAIL, exception.getMessage());
            throw exception;
        }
    }

    private String terminalFailReason(CurrentUser currentUser, String message) {
        return "terminalId=" + currentUser.terminalId()
                + ", terminalCode=" + currentUser.terminalCode()
                + ", reason=" + message;
    }

    private void record(Long userId, String sourceSystem, String targetSystem, Long appResourceId, String ticketId, String result, String failReason) {
        SsoRecord record = new SsoRecord();
        record.setUserId(userId);
        record.setSourceSystem(sourceSystem);
        record.setTargetSystem(targetSystem);
        record.setAppResourceId(appResourceId);
        record.setTicketId(ticketId);
        record.setSsoResult(result);
        record.setFailReason(failReason);
        ssoRecordMapper.insert(record);
    }
}
