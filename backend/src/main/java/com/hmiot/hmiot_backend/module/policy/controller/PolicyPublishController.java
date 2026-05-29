package com.hmiot.hmiot_backend.module.policy.controller;

import com.hmiot.hmiot_backend.common.response.ApiResponse;
import com.hmiot.hmiot_backend.common.security.CurrentUser;
import com.hmiot.hmiot_backend.module.audit.service.AuditLogService;
import com.hmiot.hmiot_backend.module.policy.dto.ClientPolicyQueryRequest;
import com.hmiot.hmiot_backend.module.policy.dto.PolicyAckRequest;
import com.hmiot.hmiot_backend.module.policy.dto.PolicyPublishRecordQueryRequest;
import com.hmiot.hmiot_backend.module.policy.service.PolicyExecutionService;
import com.hmiot.hmiot_backend.module.policy.service.PolicyPublishService;
import com.hmiot.hmiot_backend.module.policy.vo.ClientPolicyBundleVO;
import com.hmiot.hmiot_backend.module.policy.vo.PolicyPublishRecordVO;
import com.hmiot.hmiot_backend.module.ticket.service.IdentityTicketService;
import com.hmiot.hmiot_backend.module.user.vo.PageVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/policies")
public class PolicyPublishController {
    private final PolicyExecutionService policyExecutionService;
    private final PolicyPublishService policyPublishService;
    private final IdentityTicketService identityTicketService;
    private final AuditLogService auditLogService;

    public PolicyPublishController(PolicyExecutionService policyExecutionService, PolicyPublishService policyPublishService, IdentityTicketService identityTicketService, AuditLogService auditLogService) {
        this.policyExecutionService = policyExecutionService;
        this.policyPublishService = policyPublishService;
        this.identityTicketService = identityTicketService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/client")
    public ApiResponse<ClientPolicyBundleVO> clientPolicies(ClientPolicyQueryRequest request) {
        CurrentUser currentUser = identityTicketService.currentUser();
        return ApiResponse.success(policyExecutionService.clientPolicies(request, currentUser));
    }

    @PostMapping("/publish-records/{id}/ack")
    public ApiResponse<PolicyPublishRecordVO> ack(@PathVariable Long id, @Valid @RequestBody PolicyAckRequest request, HttpServletRequest servletRequest) {
        CurrentUser currentUser = identityTicketService.currentUser();
        try {
            PolicyPublishRecordVO record = policyPublishService.ack(id, request, currentUser);
            auditLogService.recordUserOperationSuccess(currentUser.userId(), currentUser.username(), "POLICY_ACK", "POLICY_PUBLISH_RECORD", String.valueOf(id), servletRequest.getRemoteAddr(), ackDetail(record));
            return ApiResponse.success(record);
        } catch (RuntimeException exception) {
            auditLogService.recordUserOperationFailure(currentUser.userId(), currentUser.username(), "POLICY_ACK", "POLICY_PUBLISH_RECORD", String.valueOf(id), servletRequest.getRemoteAddr(), exception.getMessage(), Map.of("executeResult", request.executeResult(), "terminalId", currentUser.terminalId() == null ? "" : currentUser.terminalId()));
            throw exception;
        }
    }

    @GetMapping("/publish-records")
    public ApiResponse<PageVO<PolicyPublishRecordVO>> listRecords(PolicyPublishRecordQueryRequest request) {
        return ApiResponse.success(policyPublishService.listRecords(request));
    }

    private Map<String, Object> ackDetail(PolicyPublishRecordVO record) {
        return Map.of(
                "policyType", record.policyType(),
                "policyId", record.policyId(),
                "versionNo", record.versionNo(),
                "targetType", record.targetType(),
                "targetId", record.targetId() == null ? "" : record.targetId(),
                "publishStatus", record.publishStatus(),
                "executeResult", record.executeResult() == null ? "" : record.executeResult()
        );
    }
}
