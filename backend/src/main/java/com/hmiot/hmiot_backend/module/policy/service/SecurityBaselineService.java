package com.hmiot.hmiot_backend.module.policy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.common.security.CurrentAdmin;
import com.hmiot.hmiot_backend.module.policy.dto.PolicyStatusRequest;
import com.hmiot.hmiot_backend.module.policy.dto.SecurityBaselineCreateRequest;
import com.hmiot.hmiot_backend.module.policy.dto.SecurityBaselineQueryRequest;
import com.hmiot.hmiot_backend.module.policy.dto.SecurityBaselineUpdateRequest;
import com.hmiot.hmiot_backend.module.policy.entity.SecurityBaseline;
import com.hmiot.hmiot_backend.module.policy.mapper.SecurityBaselineMapper;
import com.hmiot.hmiot_backend.module.policy.vo.SecurityBaselineVO;
import com.hmiot.hmiot_backend.module.user.vo.PageVO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SecurityBaselineService {
    public static final String ENABLED = "ENABLED";
    public static final String DISABLED = "DISABLED";
    private static final Set<String> ACTIONS = Set.of("WARN", "DENY", "STEP_UP");

    private final SecurityBaselineMapper securityBaselineMapper;
    private final ObjectMapper objectMapper;

    public SecurityBaselineService(SecurityBaselineMapper securityBaselineMapper, ObjectMapper objectMapper) {
        this.securityBaselineMapper = securityBaselineMapper;
        this.objectMapper = objectMapper;
    }

    public PageVO<SecurityBaselineVO> listBaselines(SecurityBaselineQueryRequest request) {
        LambdaQueryWrapper<SecurityBaseline> wrapper = new LambdaQueryWrapper<SecurityBaseline>().orderByDesc(SecurityBaseline::getUpdatedAt);
        if (StringUtils.hasText(request.keyword())) {
            wrapper.and(condition -> condition.like(SecurityBaseline::getBaselineCode, request.keyword()).or().like(SecurityBaseline::getBaselineName, request.keyword()));
        }
        if (StringUtils.hasText(request.targetType())) {
            wrapper.eq(SecurityBaseline::getTargetType, request.targetType());
        }
        if (StringUtils.hasText(request.status())) {
            wrapper.eq(SecurityBaseline::getStatus, request.status());
        }
        if (request.riskLevel() != null) {
            wrapper.eq(SecurityBaseline::getRiskLevel, request.riskLevel());
        }
        Page<SecurityBaseline> page = securityBaselineMapper.selectPage(Page.of(request.pageCurrent(), request.pageSize()), wrapper);
        List<SecurityBaselineVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageVO<>(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    public SecurityBaselineVO getBaseline(Long id) {
        return toVO(findBaseline(id));
    }

    @Transactional
    public SecurityBaselineVO createBaseline(SecurityBaselineCreateRequest request) {
        ensureBaselineCodeUnique(request.baselineCode(), null);
        validateBaseline(request.targetScope(), request.ruleConfig(), request.riskLevel(), request.actionOnHit());
        SecurityBaseline baseline = new SecurityBaseline();
        baseline.setBaselineCode(request.baselineCode());
        baseline.setBaselineName(request.baselineName());
        applyMutableFields(baseline, request.targetType(), request.targetScope(), request.ruleConfig(), request.riskLevel(), request.actionOnHit(), request.status());
        baseline.setCreatedBy(currentAdminId());
        securityBaselineMapper.insert(baseline);
        return toVO(baseline);
    }

    @Transactional
    public SecurityBaselineVO updateBaseline(Long id, SecurityBaselineUpdateRequest request) {
        SecurityBaseline baseline = findBaseline(id);
        validateBaseline(request.targetScope(), request.ruleConfig(), request.riskLevel(), request.actionOnHit());
        baseline.setBaselineName(request.baselineName());
        applyMutableFields(baseline, request.targetType(), request.targetScope(), request.ruleConfig(), request.riskLevel(), request.actionOnHit(), request.status());
        securityBaselineMapper.updateById(baseline);
        return toVO(baseline);
    }

    @Transactional
    public SecurityBaselineVO updateStatus(Long id, PolicyStatusRequest request) {
        SecurityBaseline baseline = findBaseline(id);
        ensureStatus(request.status());
        baseline.setStatus(request.status());
        securityBaselineMapper.updateById(baseline);
        return toVO(baseline);
    }

    public List<SecurityBaselineVO> listEnabledForClient(String clientType, Long terminalId) {
        List<SecurityBaseline> baselines = securityBaselineMapper.selectList(new LambdaQueryWrapper<SecurityBaseline>()
                .eq(SecurityBaseline::getStatus, ENABLED)
                .orderByDesc(SecurityBaseline::getRiskLevel));
        return baselines.stream()
                .filter(baseline -> matchesScope(baseline.getTargetScope(), clientType, terminalId))
                .map(this::toVO)
                .toList();
    }

    public SecurityBaseline findBaseline(Long id) {
        SecurityBaseline baseline = securityBaselineMapper.selectById(id);
        if (baseline == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "安全基线不存在");
        }
        return baseline;
    }

    public SecurityBaselineVO toVO(SecurityBaseline baseline) {
        return new SecurityBaselineVO(baseline.getId(), baseline.getBaselineCode(), baseline.getBaselineName(), baseline.getTargetType(), baseline.getTargetScope(), baseline.getRuleConfig(), baseline.getRiskLevel(), baseline.getActionOnHit(), baseline.getStatus(), baseline.getCreatedBy(), baseline.getCreatedAt(), baseline.getUpdatedAt());
    }

    private void applyMutableFields(SecurityBaseline baseline, String targetType, String targetScope, String ruleConfig, Integer riskLevel, String actionOnHit, String status) {
        baseline.setTargetType(targetType);
        baseline.setTargetScope(targetScope);
        baseline.setRuleConfig(ruleConfig);
        baseline.setRiskLevel(riskLevel == null ? 0 : riskLevel);
        baseline.setActionOnHit(StringUtils.hasText(actionOnHit) ? actionOnHit : "WARN");
        baseline.setStatus(StringUtils.hasText(status) ? status : ENABLED);
    }

    private void validateBaseline(String targetScope, String ruleConfig, Integer riskLevel, String actionOnHit) {
        jsonObject(targetScope, "目标范围必须是JSON对象");
        jsonObject(ruleConfig, "规则配置必须是JSON对象");
        if (riskLevel != null && riskLevel < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "风险等级不能小于0");
        }
        if (StringUtils.hasText(actionOnHit) && !ACTIONS.contains(actionOnHit)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "命中处置动作不支持");
        }
    }

    private boolean matchesScope(String scope, String clientType, Long terminalId) {
        Map<String, Object> values = jsonObject(scope, "目标范围必须是JSON对象");
        if (values.isEmpty()) {
            return true;
        }
        Object clientTypes = values.get("clientTypes");
        if (clientTypes instanceof List<?> list && !list.isEmpty() && !list.contains(clientType)) {
            return false;
        }
        Object terminalIds = values.get("terminalIds");
        return !(terminalIds instanceof List<?> list) || list.isEmpty() || list.stream().map(String::valueOf).anyMatch(value -> value.equals(String.valueOf(terminalId)));
    }

    private Map<String, Object> jsonObject(String value, String errorMessage) {
        if (!StringUtils.hasText(value)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, errorMessage);
        }
    }

    private void ensureBaselineCodeUnique(String baselineCode, Long excludeId) {
        SecurityBaseline existing = securityBaselineMapper.selectOne(new LambdaQueryWrapper<SecurityBaseline>().eq(SecurityBaseline::getBaselineCode, baselineCode));
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "安全基线编码已存在");
        }
    }

    private void ensureStatus(String status) {
        if (!ENABLED.equals(status) && !DISABLED.equals(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "安全基线状态不支持");
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
