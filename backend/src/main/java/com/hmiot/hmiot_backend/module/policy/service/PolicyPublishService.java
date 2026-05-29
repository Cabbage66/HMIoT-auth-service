package com.hmiot.hmiot_backend.module.policy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.common.security.CurrentAdmin;
import com.hmiot.hmiot_backend.common.security.CurrentUser;
import com.hmiot.hmiot_backend.module.policy.dto.PolicyAckRequest;
import com.hmiot.hmiot_backend.module.policy.dto.PolicyPublishRecordQueryRequest;
import com.hmiot.hmiot_backend.module.policy.dto.PolicyPublishRequest;
import com.hmiot.hmiot_backend.module.policy.entity.AuthPolicy;
import com.hmiot.hmiot_backend.module.policy.entity.PolicyPublishRecord;
import com.hmiot.hmiot_backend.module.policy.entity.SecurityBaseline;
import com.hmiot.hmiot_backend.module.policy.mapper.PolicyPublishRecordMapper;
import com.hmiot.hmiot_backend.module.policy.vo.PolicyPublishRecordVO;
import com.hmiot.hmiot_backend.module.user.vo.PageVO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class PolicyPublishService {
    public static final String AUTH_POLICY = "AUTH_POLICY";
    public static final String SECURITY_BASELINE = "SECURITY_BASELINE";
    public static final String PENDING = "PENDING";
    public static final String DELIVERED = "DELIVERED";
    public static final String ACKED = "ACKED";
    public static final String FAILED = "FAILED";
    private static final Set<String> TARGET_TYPES = Set.of("GLOBAL", "APP", "TERMINAL", "USER");

    private final PolicyPublishRecordMapper policyPublishRecordMapper;
    private final AuthPolicyService authPolicyService;
    private final SecurityBaselineService securityBaselineService;

    public PolicyPublishService(PolicyPublishRecordMapper policyPublishRecordMapper, AuthPolicyService authPolicyService, SecurityBaselineService securityBaselineService) {
        this.policyPublishRecordMapper = policyPublishRecordMapper;
        this.authPolicyService = authPolicyService;
        this.securityBaselineService = securityBaselineService;
    }

    @Transactional
    public PolicyPublishRecordVO publishAuthPolicy(Long id, PolicyPublishRequest request) {
        AuthPolicy policy = authPolicyService.findPolicy(id);
        if (!AuthPolicyService.ENABLED.equals(policy.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅启用策略允许发布");
        }
        return toVO(createRecord(AUTH_POLICY, policy.getId(), policy.getVersionNo(), request));
    }

    @Transactional
    public PolicyPublishRecordVO publishSecurityBaseline(Long id, PolicyPublishRequest request) {
        SecurityBaseline baseline = securityBaselineService.findBaseline(id);
        if (!SecurityBaselineService.ENABLED.equals(baseline.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅启用安全基线允许发布");
        }
        return toVO(createRecord(SECURITY_BASELINE, baseline.getId(), 1, request));
    }

    public PageVO<PolicyPublishRecordVO> listRecords(PolicyPublishRecordQueryRequest request) {
        LambdaQueryWrapper<PolicyPublishRecord> wrapper = new LambdaQueryWrapper<PolicyPublishRecord>().orderByDesc(PolicyPublishRecord::getPublishedAt);
        if (StringUtils.hasText(request.policyType())) {
            wrapper.eq(PolicyPublishRecord::getPolicyType, request.policyType());
        }
        if (request.policyId() != null) {
            wrapper.eq(PolicyPublishRecord::getPolicyId, request.policyId());
        }
        if (StringUtils.hasText(request.targetType())) {
            wrapper.eq(PolicyPublishRecord::getTargetType, request.targetType());
        }
        if (request.targetId() != null) {
            wrapper.eq(PolicyPublishRecord::getTargetId, request.targetId());
        }
        if (StringUtils.hasText(request.publishStatus())) {
            wrapper.eq(PolicyPublishRecord::getPublishStatus, request.publishStatus());
        }
        Page<PolicyPublishRecord> page = policyPublishRecordMapper.selectPage(Page.of(request.pageCurrent(), request.pageSize()), wrapper);
        List<PolicyPublishRecordVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageVO<>(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Transactional
    public PolicyPublishRecordVO ack(Long id, PolicyAckRequest request, CurrentUser currentUser) {
        PolicyPublishRecord record = findRecord(id);
        if (!targetMatchesCurrentUser(record, currentUser)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前终端无权确认该策略下发记录");
        }
        record.setExecuteResult(request.executeResult());
        record.setFailReason(request.failReason());
        record.setAckAt(LocalDateTime.now());
        record.setPublishStatus("SUCCESS".equals(request.executeResult()) ? ACKED : FAILED);
        policyPublishRecordMapper.updateById(record);
        return toVO(record);
    }

    public List<PolicyPublishRecordVO> pendingForClient(CurrentUser currentUser) {
        List<PolicyPublishRecord> records = policyPublishRecordMapper.selectList(new LambdaQueryWrapper<PolicyPublishRecord>()
                .in(PolicyPublishRecord::getPublishStatus, PENDING, DELIVERED)
                .and(wrapper -> wrapper.eq(PolicyPublishRecord::getTargetType, "GLOBAL")
                        .or().eq(PolicyPublishRecord::getTargetType, "USER").eq(PolicyPublishRecord::getTargetId, currentUser.userId())
                        .or().eq(PolicyPublishRecord::getTargetType, "TERMINAL").eq(PolicyPublishRecord::getTargetId, currentUser.terminalId()))
                .orderByDesc(PolicyPublishRecord::getPublishedAt));
        records.forEach(record -> {
            if (PENDING.equals(record.getPublishStatus())) {
                record.setPublishStatus(DELIVERED);
                policyPublishRecordMapper.updateById(record);
            }
        });
        return records.stream().map(this::toVO).toList();
    }

    public PolicyPublishRecordVO toVO(PolicyPublishRecord record) {
        return new PolicyPublishRecordVO(record.getId(), record.getPolicyType(), record.getPolicyId(), record.getVersionNo(), record.getTargetType(), record.getTargetId(), record.getPublishStatus(), record.getExecuteResult(), record.getFailReason(), record.getPublishedBy(), record.getPublishedAt(), record.getAckAt());
    }

    private PolicyPublishRecord createRecord(String policyType, Long policyId, Integer versionNo, PolicyPublishRequest request) {
        ensureTarget(request.targetType(), request.targetId());
        PolicyPublishRecord record = new PolicyPublishRecord();
        record.setPolicyType(policyType);
        record.setPolicyId(policyId);
        record.setVersionNo(versionNo == null ? 1 : versionNo);
        record.setTargetType(request.targetType());
        record.setTargetId(request.targetId());
        record.setPublishStatus(PENDING);
        record.setPublishedBy(currentAdminId());
        record.setPublishedAt(LocalDateTime.now());
        policyPublishRecordMapper.insert(record);
        return record;
    }

    private PolicyPublishRecord findRecord(Long id) {
        PolicyPublishRecord record = policyPublishRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "策略下发记录不存在");
        }
        return record;
    }

    private boolean targetMatchesCurrentUser(PolicyPublishRecord record, CurrentUser currentUser) {
        if ("GLOBAL".equals(record.getTargetType())) {
            return true;
        }
        if ("USER".equals(record.getTargetType())) {
            return record.getTargetId() != null && record.getTargetId().equals(currentUser.userId());
        }
        return "TERMINAL".equals(record.getTargetType()) && record.getTargetId() != null && record.getTargetId().equals(currentUser.terminalId());
    }

    private void ensureTarget(String targetType, Long targetId) {
        if (!TARGET_TYPES.contains(targetType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "发布目标类型不支持");
        }
        if (!"GLOBAL".equals(targetType) && targetId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "非全局发布必须指定目标ID");
        }
    }

    private Long currentAdminId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentAdmin currentAdmin)) {
            return null;
        }
        return currentAdmin.adminId();
    }
}
