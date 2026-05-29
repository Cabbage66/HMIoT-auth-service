package com.hmiot.hmiot_backend.module.binding.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.module.audit.service.AuditLogService;
import com.hmiot.hmiot_backend.module.binding.entity.BindingRelation;
import com.hmiot.hmiot_backend.module.binding.mapper.BindingRelationMapper;
import com.hmiot.hmiot_backend.module.device.entity.TerminalInfo;
import com.hmiot.hmiot_backend.module.device.service.TerminalInfoService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TerminalBindingValidationService {
    private final BindingRelationMapper bindingRelationMapper;
    private final TerminalInfoService terminalInfoService;
    private final AuditLogService auditLogService;

    public TerminalBindingValidationService(BindingRelationMapper bindingRelationMapper, TerminalInfoService terminalInfoService, AuditLogService auditLogService) {
        this.bindingRelationMapper = bindingRelationMapper;
        this.terminalInfoService = terminalInfoService;
        this.auditLogService = auditLogService;
    }

    public void validateUserTerminalForLogin(Long userId, String username, Long terminalId, String requestIp) {
        validate(userId, username, terminalId, "USER_LOGIN_TERMINAL_BINDING_FAIL", "LOGIN", null, null, requestIp);
    }

    public void validateUserTerminalForAccess(Long userId, String username, Long terminalId, String appCode, String ticketId, String requestIp) {
        validate(userId, username, terminalId, "USER_ACCESS_TERMINAL_BINDING_FAIL", "ACCESS", appCode, ticketId, requestIp);
    }

    private void validate(Long userId, String username, Long terminalId, String auditOperation, String scene, String appCode, String ticketId, String requestIp) {
        if (terminalId == null) {
            fail(userId, username, terminalId, null, appCode, ticketId, false, false, requestIp, auditOperation, "当前登录缺少可信终端上下文");
        }
        TerminalInfo terminal;
        try {
            terminal = terminalInfoService.findEnabledTerminal(terminalId);
        } catch (BusinessException exception) {
            fail(userId, username, terminalId, null, appCode, ticketId, false, false, requestIp, auditOperation, exception.getMessage());
            return;
        }
        List<BindingRelation> activeBindings = activeUserBindings(userId);
        if (activeBindings.isEmpty()) {
            return;
        }
        boolean matched = activeBindings.stream().anyMatch(binding -> terminalId.equals(binding.getTargetId()));
        if (!matched) {
            fail(userId, username, terminalId, terminal, appCode, ticketId, true, false, requestIp, auditOperation, "当前用户未绑定该终端，禁止" + ("LOGIN".equals(scene) ? "登录" : "访问"));
        }
    }

    private List<BindingRelation> activeUserBindings(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return bindingRelationMapper.selectList(new LambdaQueryWrapper<BindingRelation>()
                .eq(BindingRelation::getSubjectType, BindingRelationService.SUBJECT_USER)
                .eq(BindingRelation::getSubjectId, userId)
                .eq(BindingRelation::getTargetType, BindingRelationService.TARGET_TERMINAL)
                .eq(BindingRelation::getRelationType, BindingRelationService.RELATION_LOGIN_TERMINAL)
                .eq(BindingRelation::getStatus, BindingRelationService.ACTIVE)
                .le(BindingRelation::getEffectiveAt, now)
                .and(wrapper -> wrapper.isNull(BindingRelation::getExpiredAt).or().gt(BindingRelation::getExpiredAt, now)));
    }

    private void fail(Long userId, String username, Long terminalId, TerminalInfo terminal, String appCode, String ticketId, boolean bindingRequired, boolean matchedBinding, String requestIp, String auditOperation, String failReason) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("userId", userId);
        detail.put("username", username);
        detail.put("terminalId", terminalId);
        detail.put("terminalCode", terminal == null ? "" : terminal.getTerminalCode());
        detail.put("deviceEntityId", terminal == null ? "" : terminal.getDeviceEntityId());
        detail.put("appCode", appCode == null ? "" : appCode);
        detail.put("ticketId", ticketId == null ? "" : ticketId);
        detail.put("bindingRequired", bindingRequired);
        detail.put("matchedBinding", matchedBinding);
        detail.put("failReason", failReason);
        auditLogService.recordUserOperationFailure(userId, username, auditOperation, "TERMINAL_BINDING", terminalId == null ? null : String.valueOf(terminalId), requestIp, failReason, detail);
        throw new BusinessException(ErrorCode.FORBIDDEN, failReason);
    }
}
