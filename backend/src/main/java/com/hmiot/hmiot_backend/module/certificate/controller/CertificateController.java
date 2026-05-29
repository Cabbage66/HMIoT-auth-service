package com.hmiot.hmiot_backend.module.certificate.controller;

import com.hmiot.hmiot_backend.common.response.ApiResponse;
import com.hmiot.hmiot_backend.module.audit.service.AuditLogService;
import com.hmiot.hmiot_backend.module.certificate.dto.CertificateBindRequest;
import com.hmiot.hmiot_backend.module.certificate.dto.CertificateIssueRequest;
import com.hmiot.hmiot_backend.module.certificate.dto.CertificateQueryRequest;
import com.hmiot.hmiot_backend.module.certificate.dto.CertificateRevokeRequest;
import com.hmiot.hmiot_backend.module.certificate.service.CertificateService;
import com.hmiot.hmiot_backend.module.certificate.vo.CertificateOperationResponse;
import com.hmiot.hmiot_backend.module.certificate.vo.UserCertificateVO;
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
@RequestMapping("/api/v1/certificates")
public class CertificateController {
    private final CertificateService certificateService;
    private final AuditLogService auditLogService;

    public CertificateController(CertificateService certificateService, AuditLogService auditLogService) {
        this.certificateService = certificateService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<PageVO<UserCertificateVO>> listCertificates(CertificateQueryRequest request) {
        return ApiResponse.success(certificateService.listCertificates(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserCertificateVO> getCertificate(@PathVariable Long id) {
        return ApiResponse.success(certificateService.getCertificate(id));
    }

    @PostMapping("/issue")
    public ApiResponse<CertificateOperationResponse> issueCertificate(@Valid @RequestBody CertificateIssueRequest request, HttpServletRequest servletRequest) {
        try {
            CertificateOperationResponse response = certificateService.issueCertificate(request);
            auditLogService.recordAdminSuccess("CERTIFICATE_ISSUE", "USER_CERTIFICATE", String.valueOf(response.certificate().id()), servletRequest.getRemoteAddr(), Map.of(
                    "userId", response.certificate().userId(),
                    "username", response.certificate().username(),
                    "serialNumber", response.certificate().serialNumber(),
                    "fingerprintSha256", response.certificate().fingerprintSha256()
            ));
            return ApiResponse.success(response);
        } catch (RuntimeException exception) {
            auditLogService.recordAdminFailure("CERTIFICATE_ISSUE", "USER_CERTIFICATE", null, servletRequest.getRemoteAddr(), exception.getMessage(), Map.of(
                    "userId", request.userId() == null ? "" : request.userId(),
                    "username", request.username() == null ? "" : request.username()
            ));
            throw exception;
        }
    }

    @PostMapping("/bind")
    public ApiResponse<UserCertificateVO> bindCertificate(@Valid @RequestBody CertificateBindRequest request, HttpServletRequest servletRequest) {
        try {
            UserCertificateVO certificate = certificateService.bindCertificate(request);
            auditLogService.recordAdminSuccess("CERTIFICATE_BIND", "USER_CERTIFICATE", String.valueOf(certificate.id()), servletRequest.getRemoteAddr(), Map.of(
                    "userId", certificate.userId(),
                    "username", certificate.username(),
                    "serialNumber", certificate.serialNumber(),
                    "fingerprintSha256", certificate.fingerprintSha256()
            ));
            return ApiResponse.success(certificate);
        } catch (RuntimeException exception) {
            auditLogService.recordAdminFailure("CERTIFICATE_BIND", "USER_CERTIFICATE", null, servletRequest.getRemoteAddr(), exception.getMessage(), Map.of(
                    "userId", request.userId() == null ? "" : request.userId(),
                    "username", request.username() == null ? "" : request.username(),
                    "serialNumber", request.serialNumber()
            ));
            throw exception;
        }
    }

    @PostMapping("/{id}/revoke")
    public ApiResponse<UserCertificateVO> revokeCertificate(@PathVariable Long id, @Valid @RequestBody CertificateRevokeRequest request, HttpServletRequest servletRequest) {
        try {
            UserCertificateVO certificate = certificateService.revokeCertificate(id, request);
            auditLogService.recordAdminSuccess("CERTIFICATE_REVOKE", "USER_CERTIFICATE", String.valueOf(id), servletRequest.getRemoteAddr(), Map.of(
                    "userId", certificate.userId(),
                    "username", certificate.username(),
                    "serialNumber", certificate.serialNumber(),
                    "revokeReason", request.revokeReason()
            ));
            return ApiResponse.success(certificate);
        } catch (RuntimeException exception) {
            auditLogService.recordAdminFailure("CERTIFICATE_REVOKE", "USER_CERTIFICATE", String.valueOf(id), servletRequest.getRemoteAddr(), exception.getMessage(), Map.of(
                    "revokeReason", request.revokeReason()
            ));
            throw exception;
        }
    }
}
