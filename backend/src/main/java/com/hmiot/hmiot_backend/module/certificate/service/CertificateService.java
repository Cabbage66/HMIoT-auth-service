package com.hmiot.hmiot_backend.module.certificate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.module.certificate.dto.CertificateBindRequest;
import com.hmiot.hmiot_backend.module.certificate.dto.CertificateIssueRequest;
import com.hmiot.hmiot_backend.module.certificate.dto.CertificateQueryRequest;
import com.hmiot.hmiot_backend.module.certificate.dto.CertificateRevokeRequest;
import com.hmiot.hmiot_backend.module.certificate.entity.UserCertificate;
import com.hmiot.hmiot_backend.module.certificate.mapper.UserCertificateMapper;
import com.hmiot.hmiot_backend.module.certificate.vo.CertificateOperationResponse;
import com.hmiot.hmiot_backend.module.certificate.vo.UserCertificateVO;
import com.hmiot.hmiot_backend.module.integration.service.CertificateIntegrationClient;
import com.hmiot.hmiot_backend.module.integration.service.CertificateIntegrationClient.CertificateIssueResult;
import com.hmiot.hmiot_backend.module.integration.service.ExternalIntegrationCallService;
import com.hmiot.hmiot_backend.module.user.entity.UserAccount;
import com.hmiot.hmiot_backend.module.user.mapper.UserAccountMapper;
import com.hmiot.hmiot_backend.module.user.vo.ExternalCallSummaryVO;
import com.hmiot.hmiot_backend.module.user.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class CertificateService {
    private static final String VALID = "VALID";
    private static final String REVOKED = "REVOKED";

    private final UserCertificateMapper userCertificateMapper;
    private final UserAccountMapper userAccountMapper;
    private final CertificateIntegrationClient certificateIntegrationClient;
    private final ExternalIntegrationCallService externalIntegrationCallService;

    public CertificateService(UserCertificateMapper userCertificateMapper, UserAccountMapper userAccountMapper, CertificateIntegrationClient certificateIntegrationClient, ExternalIntegrationCallService externalIntegrationCallService) {
        this.userCertificateMapper = userCertificateMapper;
        this.userAccountMapper = userAccountMapper;
        this.certificateIntegrationClient = certificateIntegrationClient;
        this.externalIntegrationCallService = externalIntegrationCallService;
    }

    public PageVO<UserCertificateVO> listCertificates(CertificateQueryRequest request) {
        LambdaQueryWrapper<UserCertificate> wrapper = new LambdaQueryWrapper<UserCertificate>()
                .orderByDesc(UserCertificate::getCreatedAt);
        if (request.userId() != null) {
            wrapper.eq(UserCertificate::getUserId, request.userId());
        }
        if (StringUtils.hasText(request.status())) {
            wrapper.eq(UserCertificate::getStatus, request.status());
        }
        if (StringUtils.hasText(request.username())) {
            UserAccount user = findUserByUsername(request.username());
            wrapper.eq(UserCertificate::getUserId, user.getId());
        }
        Page<UserCertificate> page = userCertificateMapper.selectPage(Page.of(request.pageCurrent(), request.pageSize()), wrapper);
        List<UserCertificateVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageVO<>(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    public UserCertificateVO getCertificate(Long id) {
        return toVO(findCertificate(id));
    }

    @Transactional
    public CertificateOperationResponse issueCertificate(CertificateIssueRequest request) {
        UserAccount user = findUser(request.userId(), request.username());
        LocalDateTime startedAt = LocalDateTime.now();
        Map<String, Object> requestSummary = Map.of(
                "userId", user.getId(),
                "username", user.getUsername()
        );
        CertificateIssueResult result;
        try {
            result = certificateIntegrationClient.issueCertificate(user.getUsername(), request.publicKeyPem());
        } catch (RuntimeException exception) {
            externalIntegrationCallService.recordFailure("CERTIFICATE_ISSUE", certificateIntegrationClient.targetSystem(), String.valueOf(user.getId()), requestSummary, exception.getMessage(), startedAt);
            throw exception;
        }

        UserCertificate certificate = new UserCertificate();
        certificate.setUserId(user.getId());
        certificate.setSerialNumber(result.serial());
        certificate.setFingerprintSha256(result.fingerprintSha256());
        certificate.setCertificatePem(result.certificatePem());
        certificate.setSubjectDn("CN=" + user.getUsername());
        certificate.setStatus(VALID);
        certificate.setIssuedAt(certificateIntegrationClient.defaultIssuedAt());
        userCertificateMapper.insert(certificate);

        Long callId = externalIntegrationCallService.recordSuccess("CERTIFICATE_ISSUE", certificateIntegrationClient.targetSystem(), String.valueOf(user.getId()), requestSummary, Map.of(
                "serialNumber", result.serial(),
                "fingerprintSha256", result.fingerprintSha256()
        ), startedAt);
        return new CertificateOperationResponse(toVO(certificate, user), new ExternalCallSummaryVO(callId, "CERTIFICATE_ISSUE", certificateIntegrationClient.targetSystem(), String.valueOf(user.getId()), "SUCCESS"));
    }

    @Transactional
    public UserCertificateVO bindCertificate(CertificateBindRequest request) {
        UserAccount user = findUser(request.userId(), request.username());
        UserCertificate certificate = new UserCertificate();
        certificate.setUserId(user.getId());
        certificate.setSerialNumber(request.serialNumber());
        certificate.setFingerprintSha256(request.fingerprintSha256());
        certificate.setCertificatePem(request.certificatePem());
        certificate.setIssuer(request.issuer());
        certificate.setSubjectDn(request.subjectDn());
        certificate.setStatus(VALID);
        certificate.setIssuedAt(LocalDateTime.now());
        userCertificateMapper.insert(certificate);
        return toVO(certificate, user);
    }

    @Transactional
    public UserCertificateVO revokeCertificate(Long id, CertificateRevokeRequest request) {
        UserCertificate certificate = findCertificate(id);
        if (REVOKED.equals(certificate.getStatus())) {
            throw new BusinessException("证书已吊销");
        }
        certificate.setStatus(REVOKED);
        certificate.setRevokedAt(LocalDateTime.now());
        certificate.setRevokeReason(request.revokeReason());
        userCertificateMapper.updateById(certificate);
        return toVO(certificate);
    }

    public boolean hasValidCertificate(Long userId) {
        return userCertificateMapper.selectCount(new LambdaQueryWrapper<UserCertificate>()
                .eq(UserCertificate::getUserId, userId)
                .eq(UserCertificate::getStatus, VALID)) > 0;
    }

    public boolean matchesValidCertificate(Long userId, String serialNumber, String fingerprintSha256) {
        LambdaQueryWrapper<UserCertificate> wrapper = new LambdaQueryWrapper<UserCertificate>()
                .eq(UserCertificate::getUserId, userId)
                .eq(UserCertificate::getStatus, VALID);
        if (StringUtils.hasText(serialNumber)) {
            wrapper.eq(UserCertificate::getSerialNumber, serialNumber);
        }
        if (StringUtils.hasText(fingerprintSha256)) {
            wrapper.eq(UserCertificate::getFingerprintSha256, fingerprintSha256);
        }
        return userCertificateMapper.selectCount(wrapper) > 0;
    }

    private UserCertificate findCertificate(Long id) {
        UserCertificate certificate = userCertificateMapper.selectById(id);
        if (certificate == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "证书不存在");
        }
        return certificate;
    }

    private UserAccount findUser(Long userId, String username) {
        if (userId != null) {
            return findUserById(userId);
        }
        if (StringUtils.hasText(username)) {
            return findUserByUsername(username);
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "用户标识不能为空");
    }

    private UserAccount findUserById(Long userId) {
        UserAccount user = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getId, userId)
                .eq(UserAccount::getDeleted, false));
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private UserAccount findUserByUsername(String username) {
        UserAccount user = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getUsername, username)
                .eq(UserAccount::getDeleted, false));
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private UserCertificateVO toVO(UserCertificate certificate) {
        return toVO(certificate, findUserById(certificate.getUserId()));
    }

    private UserCertificateVO toVO(UserCertificate certificate, UserAccount user) {
        return new UserCertificateVO(
                certificate.getId(),
                certificate.getUserId(),
                user.getUsername(),
                user.getRealName(),
                certificate.getSerialNumber(),
                certificate.getFingerprintSha256(),
                certificate.getCertificatePem(),
                certificate.getIssuer(),
                certificate.getSubjectDn(),
                certificate.getStatus(),
                certificate.getIssuedAt(),
                certificate.getExpiresAt(),
                certificate.getRevokedAt(),
                certificate.getRevokeReason(),
                certificate.getCreatedAt(),
                certificate.getUpdatedAt()
        );
    }
}
