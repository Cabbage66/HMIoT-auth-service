package com.hmiot.hmiot_backend.module.biometric.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.common.security.CurrentAdmin;
import com.hmiot.hmiot_backend.module.biometric.dto.BiometricAuthRecordQueryRequest;
import com.hmiot.hmiot_backend.module.biometric.dto.BiometricTemplateQueryRequest;
import com.hmiot.hmiot_backend.module.biometric.dto.BiometricTemplateReviewRequest;
import com.hmiot.hmiot_backend.module.biometric.dto.BiometricTemplateRevokeRequest;
import com.hmiot.hmiot_backend.module.biometric.entity.BiometricAuthRecord;
import com.hmiot.hmiot_backend.module.biometric.entity.BiometricTemplate;
import com.hmiot.hmiot_backend.module.biometric.mapper.BiometricAuthRecordMapper;
import com.hmiot.hmiot_backend.module.biometric.mapper.BiometricTemplateMapper;
import com.hmiot.hmiot_backend.module.biometric.vo.BiometricAuthRecordVO;
import com.hmiot.hmiot_backend.module.biometric.vo.BiometricTemplateVO;
import com.hmiot.hmiot_backend.module.binding.entity.BindingRelation;
import com.hmiot.hmiot_backend.module.binding.service.BindingRelationService;
import com.hmiot.hmiot_backend.module.user.entity.UserAccount;
import com.hmiot.hmiot_backend.module.user.mapper.UserAccountMapper;
import com.hmiot.hmiot_backend.module.user.vo.PageVO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class BiometricTemplateService {
    public static final String VOICEPRINT = "VOICEPRINT";
    public static final String FINGERPRINT = "FINGERPRINT";
    public static final String FACE = "FACE";
    public static final String DYNAMIC_BIOMETRIC = "DYNAMIC_BIOMETRIC";
    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final String ACTIVE = "ACTIVE";
    public static final String REVOKED = "REVOKED";
    public static final String REPLACED = "REPLACED";
    public static final String SUCCESS = "SUCCESS";

    private final BiometricTemplateMapper biometricTemplateMapper;
    private final BiometricAuthRecordMapper biometricAuthRecordMapper;
    private final UserAccountMapper userAccountMapper;
    private final BindingRelationService bindingRelationService;
    private final ObjectMapper objectMapper;

    public BiometricTemplateService(BiometricTemplateMapper biometricTemplateMapper, BiometricAuthRecordMapper biometricAuthRecordMapper, UserAccountMapper userAccountMapper, BindingRelationService bindingRelationService, ObjectMapper objectMapper) {
        this.biometricTemplateMapper = biometricTemplateMapper;
        this.biometricAuthRecordMapper = biometricAuthRecordMapper;
        this.userAccountMapper = userAccountMapper;
        this.bindingRelationService = bindingRelationService;
        this.objectMapper = objectMapper;
    }

    public PageVO<BiometricTemplateVO> listTemplates(BiometricTemplateQueryRequest request) {
        LambdaQueryWrapper<BiometricTemplate> wrapper = new LambdaQueryWrapper<BiometricTemplate>()
                .orderByDesc(BiometricTemplate::getCreatedAt);
        if (request.userId() != null) {
            wrapper.eq(BiometricTemplate::getUserId, request.userId());
        }
        if (StringUtils.hasText(request.biometricType())) {
            wrapper.eq(BiometricTemplate::getBiometricType, request.biometricType());
        }
        if (StringUtils.hasText(request.auditStatus())) {
            wrapper.eq(BiometricTemplate::getAuditStatus, request.auditStatus());
        }
        if (StringUtils.hasText(request.templateStatus())) {
            wrapper.eq(BiometricTemplate::getTemplateStatus, request.templateStatus());
        }
        Page<BiometricTemplate> page = biometricTemplateMapper.selectPage(Page.of(request.pageCurrent(), request.pageSize()), wrapper);
        List<BiometricTemplateVO> records = page.getRecords().stream().map(this::toTemplateVO).toList();
        return new PageVO<>(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    public BiometricTemplateVO getTemplate(Long id) {
        return toTemplateVO(findTemplate(id));
    }

    public List<BiometricTemplateVO> listUserVoiceprints(Long userId) {
        return biometricTemplateMapper.selectList(new LambdaQueryWrapper<BiometricTemplate>()
                        .eq(BiometricTemplate::getUserId, userId)
                        .eq(BiometricTemplate::getBiometricType, VOICEPRINT)
                        .orderByDesc(BiometricTemplate::getCreatedAt))
                .stream().map(this::toTemplateVO).toList();
    }

    public List<BiometricTemplateVO> listUserFingerprints(Long userId) {
        return biometricTemplateMapper.selectList(new LambdaQueryWrapper<BiometricTemplate>()
                        .eq(BiometricTemplate::getUserId, userId)
                        .eq(BiometricTemplate::getBiometricType, FINGERPRINT)
                        .orderByDesc(BiometricTemplate::getCreatedAt))
                .stream().map(this::toTemplateVO).toList();
    }

    public List<BiometricTemplateVO> listUserFaces(Long userId) {
        return biometricTemplateMapper.selectList(new LambdaQueryWrapper<BiometricTemplate>()
                        .eq(BiometricTemplate::getUserId, userId)
                        .eq(BiometricTemplate::getBiometricType, FACE)
                        .orderByDesc(BiometricTemplate::getCreatedAt))
                .stream().map(this::toTemplateVO).toList();
    }

    @Transactional
    public BiometricTemplateVO reviewTemplate(Long id, BiometricTemplateReviewRequest request) {
        BiometricTemplate template = findTemplate(id);
        if (REVOKED.equals(template.getTemplateStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "已撤销模板不能审核");
        }
        if (!PENDING.equals(template.getAuditStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "模板已审核");
        }
        if (!APPROVED.equals(request.auditStatus()) && !REJECTED.equals(request.auditStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "审核结果无效");
        }
        if (REJECTED.equals(request.auditStatus()) && !StringUtils.hasText(request.rejectReason())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "拒绝原因不能为空");
        }
        template.setAuditStatus(request.auditStatus());
        template.setRejectReason(request.rejectReason());
        CurrentAdmin currentAdmin = currentAdmin();
        template.setReviewedBy(currentAdmin == null ? null : currentAdmin.adminId());
        template.setReviewedAt(LocalDateTime.now());
        biometricTemplateMapper.updateById(template);
        if (APPROVED.equals(request.auditStatus()) && VOICEPRINT.equals(template.getBiometricType())) {
            replaceOtherActiveVoiceprints(template.getUserId(), template.getId());
        }
        if (APPROVED.equals(request.auditStatus()) && FINGERPRINT.equals(template.getBiometricType())) {
            replaceOtherActiveFingerprints(template.getUserId(), template.getId());
        }
        if (APPROVED.equals(request.auditStatus()) && FACE.equals(template.getBiometricType())) {
            replaceOtherActiveFaces(template.getUserId(), template.getId());
        }
        return toTemplateVO(template);
    }

    @Transactional
    public BiometricTemplateVO revokeTemplate(Long id, BiometricTemplateRevokeRequest request) {
        BiometricTemplate template = findTemplate(id);
        if (REVOKED.equals(template.getTemplateStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "模板已撤销");
        }
        template.setTemplateStatus(REVOKED);
        template.setRevokedAt(LocalDateTime.now());
        template.setRejectReason(request.revokeReason());
        biometricTemplateMapper.updateById(template);
        return toTemplateVO(template);
    }

    public PageVO<BiometricAuthRecordVO> listAuthRecords(BiometricAuthRecordQueryRequest request) {
        LambdaQueryWrapper<BiometricAuthRecord> wrapper = new LambdaQueryWrapper<BiometricAuthRecord>()
                .orderByDesc(BiometricAuthRecord::getCreatedAt);
        if (request.userId() != null) {
            wrapper.eq(BiometricAuthRecord::getUserId, request.userId());
        }
        if (StringUtils.hasText(request.biometricType())) {
            wrapper.eq(BiometricAuthRecord::getBiometricType, request.biometricType());
        }
        if (StringUtils.hasText(request.authResult())) {
            wrapper.eq(BiometricAuthRecord::getAuthResult, request.authResult());
        }
        if (request.startTime() != null) {
            wrapper.ge(BiometricAuthRecord::getCreatedAt, request.startTime());
        }
        if (request.endTime() != null) {
            wrapper.le(BiometricAuthRecord::getCreatedAt, request.endTime());
        }
        Page<BiometricAuthRecord> page = biometricAuthRecordMapper.selectPage(Page.of(request.pageCurrent(), request.pageSize()), wrapper);
        List<BiometricAuthRecordVO> records = page.getRecords().stream().map(this::toAuthRecordVO).toList();
        return new PageVO<>(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    public BiometricAuthRecordVO getAuthRecord(Long id) {
        BiometricAuthRecord record = biometricAuthRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "生物认证流水不存在");
        }
        return toAuthRecordVO(record);
    }

    public PageVO<BiometricAuthRecordVO> listUserAuthRecords(Long userId, BiometricAuthRecordQueryRequest request) {
        BiometricAuthRecordQueryRequest scoped = new BiometricAuthRecordQueryRequest(userId, VOICEPRINT, request.authResult(), request.startTime(), request.endTime(), request.current(), request.size());
        return listAuthRecords(scoped);
    }

    public PageVO<BiometricAuthRecordVO> listUserDynamicBiometricAuthRecords(Long userId, BiometricAuthRecordQueryRequest request) {
        BiometricAuthRecordQueryRequest scoped = new BiometricAuthRecordQueryRequest(userId, DYNAMIC_BIOMETRIC, request.authResult(), request.startTime(), request.endTime(), request.current(), request.size());
        return listAuthRecords(scoped);
    }

    public BiometricAuthRecord findSuccessfulDynamicBiometricRecord(Long recordId, Long userId) {
        BiometricAuthRecord record = biometricAuthRecordMapper.selectOne(new LambdaQueryWrapper<BiometricAuthRecord>()
                .eq(BiometricAuthRecord::getId, recordId)
                .eq(BiometricAuthRecord::getUserId, userId)
                .eq(BiometricAuthRecord::getBiometricType, DYNAMIC_BIOMETRIC)
                .eq(BiometricAuthRecord::getAuthResult, SUCCESS));
        if (record == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "动态生物认证记录无效");
        }
        BindingRelation binding = bindingRelationService.findActiveDynamicBiometricBinding(userId);
        Object targetId = rawResponse(record).get("dynamicBiometricTargetId");
        if (targetId == null || !String.valueOf(binding.getTargetId()).equals(String.valueOf(targetId))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "动态生物认证记录与当前绑定不匹配");
        }
        return record;
    }

    public BiometricTemplate latestApprovedVoiceprint(Long userId) {
        BiometricTemplate template = biometricTemplateMapper.selectOne(new LambdaQueryWrapper<BiometricTemplate>()
                .eq(BiometricTemplate::getUserId, userId)
                .eq(BiometricTemplate::getBiometricType, VOICEPRINT)
                .eq(BiometricTemplate::getAuditStatus, APPROVED)
                .eq(BiometricTemplate::getTemplateStatus, ACTIVE)
                .orderByDesc(BiometricTemplate::getVersionNo)
                .orderByDesc(BiometricTemplate::getCreatedAt)
                .last("LIMIT 1"));
        if (template == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "用户无已审核通过的声纹模板");
        }
        return template;
    }

    public BiometricTemplate latestApprovedFingerprint(Long userId) {
        BiometricTemplate template = biometricTemplateMapper.selectOne(new LambdaQueryWrapper<BiometricTemplate>()
                .eq(BiometricTemplate::getUserId, userId)
                .eq(BiometricTemplate::getBiometricType, FINGERPRINT)
                .eq(BiometricTemplate::getAuditStatus, APPROVED)
                .eq(BiometricTemplate::getTemplateStatus, ACTIVE)
                .orderByDesc(BiometricTemplate::getVersionNo)
                .orderByDesc(BiometricTemplate::getCreatedAt)
                .last("LIMIT 1"));
        if (template == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "用户无已审核通过的指纹模板");
        }
        return template;
    }

    public BiometricTemplate latestApprovedFace(Long userId) {
        BiometricTemplate template = biometricTemplateMapper.selectOne(new LambdaQueryWrapper<BiometricTemplate>()
                .eq(BiometricTemplate::getUserId, userId)
                .eq(BiometricTemplate::getBiometricType, FACE)
                .eq(BiometricTemplate::getAuditStatus, APPROVED)
                .eq(BiometricTemplate::getTemplateStatus, ACTIVE)
                .orderByDesc(BiometricTemplate::getVersionNo)
                .orderByDesc(BiometricTemplate::getCreatedAt)
                .last("LIMIT 1"));
        if (template == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "用户无已审核通过的人脸模板");
        }
        return template;
    }

    public BiometricTemplate approvedActiveFingerprintByKeyId(Long userId, String keyId) {
        if (!StringUtils.hasText(keyId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "指纹KeyId不能为空");
        }
        BiometricTemplate template = biometricTemplateMapper.selectOne(new LambdaQueryWrapper<BiometricTemplate>()
                .eq(BiometricTemplate::getUserId, userId)
                .eq(BiometricTemplate::getBiometricType, FINGERPRINT)
                .eq(BiometricTemplate::getAuditStatus, APPROVED)
                .eq(BiometricTemplate::getTemplateStatus, ACTIVE)
                .eq(BiometricTemplate::getTemplateRef, keyId.trim())
                .orderByDesc(BiometricTemplate::getVersionNo)
                .orderByDesc(BiometricTemplate::getCreatedAt)
                .last("LIMIT 1"));
        if (template == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "指纹模板未绑定或未通过审核");
        }
        return template;
    }

    public BiometricAuthRecord findSuccessfulVoiceprintRecord(Long recordId, Long userId) {
        BiometricAuthRecord record = biometricAuthRecordMapper.selectOne(new LambdaQueryWrapper<BiometricAuthRecord>()
                .eq(BiometricAuthRecord::getId, recordId)
                .eq(BiometricAuthRecord::getUserId, userId)
                .eq(BiometricAuthRecord::getBiometricType, VOICEPRINT)
                .eq(BiometricAuthRecord::getAuthResult, SUCCESS));
        if (record == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "声纹认证记录无效");
        }
        return record;
    }

    public int nextVoiceprintVersion(Long userId) {
        BiometricTemplate template = biometricTemplateMapper.selectOne(new LambdaQueryWrapper<BiometricTemplate>()
                .eq(BiometricTemplate::getUserId, userId)
                .eq(BiometricTemplate::getBiometricType, VOICEPRINT)
                .orderByDesc(BiometricTemplate::getVersionNo)
                .last("LIMIT 1"));
        return template == null || template.getVersionNo() == null ? 1 : template.getVersionNo() + 1;
    }

    public int nextFingerprintVersion(Long userId) {
        BiometricTemplate template = biometricTemplateMapper.selectOne(new LambdaQueryWrapper<BiometricTemplate>()
                .eq(BiometricTemplate::getUserId, userId)
                .eq(BiometricTemplate::getBiometricType, FINGERPRINT)
                .orderByDesc(BiometricTemplate::getVersionNo)
                .last("LIMIT 1"));
        return template == null || template.getVersionNo() == null ? 1 : template.getVersionNo() + 1;
    }

    public int nextFaceVersion(Long userId) {
        BiometricTemplate template = biometricTemplateMapper.selectOne(new LambdaQueryWrapper<BiometricTemplate>()
                .eq(BiometricTemplate::getUserId, userId)
                .eq(BiometricTemplate::getBiometricType, FACE)
                .orderByDesc(BiometricTemplate::getVersionNo)
                .last("LIMIT 1"));
        return template == null || template.getVersionNo() == null ? 1 : template.getVersionNo() + 1;
    }

    public void replaceActivePendingVoiceprints(Long userId) {
        List<BiometricTemplate> templates = biometricTemplateMapper.selectList(new LambdaQueryWrapper<BiometricTemplate>()
                .eq(BiometricTemplate::getUserId, userId)
                .eq(BiometricTemplate::getBiometricType, VOICEPRINT)
                .eq(BiometricTemplate::getTemplateStatus, ACTIVE)
                .ne(BiometricTemplate::getAuditStatus, APPROVED));
        for (BiometricTemplate template : templates) {
            template.setTemplateStatus(REPLACED);
            biometricTemplateMapper.updateById(template);
        }
    }

    public void replaceActivePendingFingerprints(Long userId) {
        List<BiometricTemplate> templates = biometricTemplateMapper.selectList(new LambdaQueryWrapper<BiometricTemplate>()
                .eq(BiometricTemplate::getUserId, userId)
                .eq(BiometricTemplate::getBiometricType, FINGERPRINT)
                .eq(BiometricTemplate::getTemplateStatus, ACTIVE)
                .ne(BiometricTemplate::getAuditStatus, APPROVED));
        for (BiometricTemplate template : templates) {
            template.setTemplateStatus(REPLACED);
            biometricTemplateMapper.updateById(template);
        }
    }

    public void replaceActivePendingFaces(Long userId) {
        List<BiometricTemplate> templates = biometricTemplateMapper.selectList(new LambdaQueryWrapper<BiometricTemplate>()
                .eq(BiometricTemplate::getUserId, userId)
                .eq(BiometricTemplate::getBiometricType, FACE)
                .eq(BiometricTemplate::getTemplateStatus, ACTIVE)
                .ne(BiometricTemplate::getAuditStatus, APPROVED));
        for (BiometricTemplate template : templates) {
            template.setTemplateStatus(REPLACED);
            biometricTemplateMapper.updateById(template);
        }
    }

    private void replaceOtherActiveVoiceprints(Long userId, Long keepTemplateId) {
        List<BiometricTemplate> templates = biometricTemplateMapper.selectList(new LambdaQueryWrapper<BiometricTemplate>()
                .eq(BiometricTemplate::getUserId, userId)
                .eq(BiometricTemplate::getBiometricType, VOICEPRINT)
                .eq(BiometricTemplate::getTemplateStatus, ACTIVE)
                .ne(BiometricTemplate::getId, keepTemplateId));
        for (BiometricTemplate template : templates) {
            template.setTemplateStatus(REPLACED);
            biometricTemplateMapper.updateById(template);
        }
    }

    private void replaceOtherActiveFingerprints(Long userId, Long keepTemplateId) {
        List<BiometricTemplate> templates = biometricTemplateMapper.selectList(new LambdaQueryWrapper<BiometricTemplate>()
                .eq(BiometricTemplate::getUserId, userId)
                .eq(BiometricTemplate::getBiometricType, FINGERPRINT)
                .eq(BiometricTemplate::getTemplateStatus, ACTIVE)
                .ne(BiometricTemplate::getId, keepTemplateId));
        for (BiometricTemplate template : templates) {
            template.setTemplateStatus(REPLACED);
            biometricTemplateMapper.updateById(template);
        }
    }

    private void replaceOtherActiveFaces(Long userId, Long keepTemplateId) {
        List<BiometricTemplate> templates = biometricTemplateMapper.selectList(new LambdaQueryWrapper<BiometricTemplate>()
                .eq(BiometricTemplate::getUserId, userId)
                .eq(BiometricTemplate::getBiometricType, FACE)
                .eq(BiometricTemplate::getTemplateStatus, ACTIVE)
                .ne(BiometricTemplate::getId, keepTemplateId));
        for (BiometricTemplate template : templates) {
            template.setTemplateStatus(REPLACED);
            biometricTemplateMapper.updateById(template);
        }
    }

    public BiometricTemplateVO toTemplateVO(BiometricTemplate template) {
        UserAccount user = findUser(template.getUserId());
        return new BiometricTemplateVO(
                template.getId(),
                template.getUserId(),
                user.getUsername(),
                user.getRealName(),
                template.getBiometricType(),
                template.getTemplateDigest(),
                template.getTemplateRef(),
                template.getModelSource(),
                template.getModelVersion(),
                template.getThreshold(),
                template.getAuditStatus(),
                template.getTemplateStatus(),
                template.getVersionNo(),
                template.getSubmittedAt(),
                template.getReviewedBy(),
                template.getReviewedAt(),
                template.getRevokedAt(),
                template.getRejectReason(),
                template.getMetadata(),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }

    public BiometricAuthRecordVO toAuthRecordVO(BiometricAuthRecord record) {
        UserAccount user = record.getUserId() == null ? null : findUser(record.getUserId());
        return new BiometricAuthRecordVO(
                record.getId(),
                record.getUserId(),
                user == null ? null : user.getUsername(),
                user == null ? null : user.getRealName(),
                record.getBiometricType(),
                record.getTemplateId(),
                record.getRequestId(),
                record.getScore(),
                record.getThreshold(),
                record.getAuthResult(),
                record.getFailReason(),
                record.getModelSource(),
                record.getRawResponse(),
                record.getCreatedAt()
        );
    }

    public String toJson(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "生物特征元数据序列化失败");
        }
    }

    private Map<String, Object> rawResponse(BiometricAuthRecord record) {
        if (!StringUtils.hasText(record.getRawResponse())) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(record.getRawResponse(), new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "动态生物认证记录上下文无效");
        }
    }

    private BiometricTemplate findTemplate(Long id) {
        BiometricTemplate template = biometricTemplateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "生物模板不存在");
        }
        return template;
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

    private CurrentAdmin currentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentAdmin currentAdmin)) {
            return null;
        }
        return currentAdmin;
    }
}
