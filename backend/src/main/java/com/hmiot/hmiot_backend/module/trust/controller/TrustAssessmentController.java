package com.hmiot.hmiot_backend.module.trust.controller;

import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.common.response.ApiResponse;
import com.hmiot.hmiot_backend.common.security.CurrentUser;
import com.hmiot.hmiot_backend.module.audit.service.AuditLogService;
import com.hmiot.hmiot_backend.module.trust.dto.TrustAssessmentQueryRequest;
import com.hmiot.hmiot_backend.module.trust.dto.TrustAssessmentTriggerRequest;
import com.hmiot.hmiot_backend.module.trust.service.TrustAssessmentService;
import com.hmiot.hmiot_backend.module.trust.vo.TrustAssessmentCheckResponse;
import com.hmiot.hmiot_backend.module.trust.vo.TrustAssessmentResultVO;
import com.hmiot.hmiot_backend.module.trust.vo.UebaHealthResponse;
import com.hmiot.hmiot_backend.module.user.vo.PageVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/trust-assessments")
public class TrustAssessmentController {
    private final TrustAssessmentService trustAssessmentService;
    private final AuditLogService auditLogService;

    public TrustAssessmentController(TrustAssessmentService trustAssessmentService, AuditLogService auditLogService) {
        this.trustAssessmentService = trustAssessmentService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/check")
    public ApiResponse<TrustAssessmentCheckResponse> check(@RequestBody(required = false) TrustAssessmentTriggerRequest request, HttpServletRequest servletRequest) {
        CurrentUser currentUser = currentUser();
        TrustAssessmentTriggerRequest effectiveRequest = request == null ? new TrustAssessmentTriggerRequest(null, null, null) : request;
        try {
            TrustAssessmentCheckResponse response = trustAssessmentService.check(effectiveRequest, currentUser);
            if (response.trusted()) {
                auditLogService.recordUserOperationSuccess(currentUser.userId(), currentUser.username(), "TRUST_ASSESSMENT_CHECK", "TRUST_ASSESSMENT", String.valueOf(response.assessmentResultId()), servletRequest.getRemoteAddr(), checkAuditDetail(response, effectiveRequest));
            } else {
                auditLogService.recordUserOperationFailure(currentUser.userId(), currentUser.username(), "TRUST_ASSESSMENT_CHECK", "TRUST_ASSESSMENT", String.valueOf(response.assessmentResultId()), servletRequest.getRemoteAddr(), response.failReason(), checkAuditDetail(response, effectiveRequest));
            }
            return ApiResponse.success(response);
        } catch (RuntimeException exception) {
            auditLogService.recordUserOperationFailure(currentUser.userId(), currentUser.username(), "TRUST_ASSESSMENT_CHECK_FAIL", "TRUST_ASSESSMENT", null, servletRequest.getRemoteAddr(), exception.getMessage(), Map.of(
                    "appCode", effectiveRequest.appCode() == null ? "" : effectiveRequest.appCode(),
                    "appResourceId", effectiveRequest.appResourceId() == null ? "" : effectiveRequest.appResourceId(),
                    "requestId", effectiveRequest.requestId() == null ? "" : effectiveRequest.requestId()
            ));
            throw exception;
        }
    }

    @GetMapping
    public ApiResponse<PageVO<TrustAssessmentResultVO>> list(TrustAssessmentQueryRequest request) {
        return ApiResponse.success(trustAssessmentService.listAssessments(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<TrustAssessmentResultVO> detail(@PathVariable Long id) {
        return ApiResponse.success(trustAssessmentService.detail(id));
    }

    @GetMapping("/ueba/health")
    public ApiResponse<UebaHealthResponse> health() {
        return ApiResponse.success(trustAssessmentService.health());
    }

    private CurrentUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser currentUser)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前用户未登录");
        }
        return currentUser;
    }

    private Map<String, Object> checkAuditDetail(TrustAssessmentCheckResponse response, TrustAssessmentTriggerRequest request) {
        return Map.of(
                "assessmentResultId", response.assessmentResultId() == null ? "" : response.assessmentResultId(),
                "requestId", response.requestId() == null ? "" : response.requestId(),
                "appCode", request.appCode() == null ? "" : request.appCode(),
                "appResourceId", request.appResourceId() == null ? "" : request.appResourceId(),
                "trusted", response.trusted(),
                "riskLevel", response.riskLevel() == null ? "" : response.riskLevel(),
                "decision", response.decision() == null ? "" : response.decision(),
                "failReason", response.failReason() == null ? "" : response.failReason()
        );
    }
}
