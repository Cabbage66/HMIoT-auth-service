package com.hmiot.hmiot_backend.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.module.integration.service.ExternalIntegrationCallService;
import com.hmiot.hmiot_backend.module.integration.service.PostQuantumAttributeMockAdapter;
import com.hmiot.hmiot_backend.module.integration.service.PostQuantumAttributeMockAdapter.AttributeResult;
import com.hmiot.hmiot_backend.module.user.dto.UserQueryRequest;
import com.hmiot.hmiot_backend.module.user.dto.UserSelfUpdateRequest;
import com.hmiot.hmiot_backend.module.user.dto.UserUpdateRequest;
import com.hmiot.hmiot_backend.module.user.entity.UserAccount;
import com.hmiot.hmiot_backend.module.user.entity.UserProfile;
import com.hmiot.hmiot_backend.module.user.mapper.UserAccountMapper;
import com.hmiot.hmiot_backend.module.user.mapper.UserProfileMapper;
import com.hmiot.hmiot_backend.module.user.vo.ExternalCallSummaryVO;
import com.hmiot.hmiot_backend.module.user.vo.PageVO;
import com.hmiot.hmiot_backend.module.user.vo.UserAttributeSyncResponse;
import com.hmiot.hmiot_backend.module.user.vo.UserProfileVO;
import com.hmiot.hmiot_backend.module.user.vo.UserSelfProfileVO;
import com.hmiot.hmiot_backend.module.user.vo.UserVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class UserService {
    private static final String POST_QUANTUM_ATTRIBUTE = "POST_QUANTUM_ATTRIBUTE";

    private final UserAccountMapper userAccountMapper;
    private final UserProfileMapper userProfileMapper;
    private final PostQuantumAttributeMockAdapter attributeMockAdapter;
    private final ExternalIntegrationCallService externalIntegrationCallService;
    private final ObjectMapper objectMapper;

    public UserService(UserAccountMapper userAccountMapper, UserProfileMapper userProfileMapper, PostQuantumAttributeMockAdapter attributeMockAdapter, ExternalIntegrationCallService externalIntegrationCallService, ObjectMapper objectMapper) {
        this.userAccountMapper = userAccountMapper;
        this.userProfileMapper = userProfileMapper;
        this.attributeMockAdapter = attributeMockAdapter;
        this.externalIntegrationCallService = externalIntegrationCallService;
        this.objectMapper = objectMapper;
    }

    public PageVO<UserVO> listUsers(UserQueryRequest request) {
        LambdaQueryWrapper<UserAccount> wrapper = new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getDeleted, false)
                .orderByDesc(UserAccount::getCreatedAt);
        if (StringUtils.hasText(request.keyword())) {
            wrapper.and(query -> query.like(UserAccount::getUsername, request.keyword())
                    .or().like(UserAccount::getRealName, request.keyword())
                    .or().like(UserAccount::getPhone, request.keyword()));
        }
        if (StringUtils.hasText(request.status())) {
            wrapper.eq(UserAccount::getStatus, request.status());
        }
        Page<UserAccount> page = userAccountMapper.selectPage(Page.of(request.pageCurrent(), request.pageSize()), wrapper);
        List<UserVO> records = page.getRecords().stream().map(user -> toVO(user, findProfile(user.getId()))).toList();
        return new PageVO<>(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    public UserVO getUser(Long id) {
        UserAccount user = findUser(id);
        return toVO(user, findProfile(id));
    }

    public UserSelfProfileVO getSelfProfile(Long userId) {
        UserAccount user = findUser(userId);
        return toSelfVO(user);
    }

    @Transactional
    public UserVO updateUser(Long id, UserUpdateRequest request) {
        UserAccount user = findUser(id);
        user.setRealName(request.realName());
        user.setPhone(request.phone());
        user.setEmail(request.email());
        user.setIdCardNo(request.idCardNo());
        if (StringUtils.hasText(request.status())) {
            user.setStatus(request.status());
        }
        userAccountMapper.updateById(user);
        return getUser(id);
    }

    @Transactional
    public UserSelfProfileVO updateSelfProfile(Long userId, UserSelfUpdateRequest request) {
        UserAccount user = findUser(userId);
        user.setRealName(request.realName().trim());
        user.setPhone(emptyToNull(request.phone()));
        user.setEmail(emptyToNull(request.email()));
        user.setIdCardNo(emptyToNull(request.idCardNo()));
        userAccountMapper.updateById(user);
        return toSelfVO(findUser(userId));
    }

    @Transactional
    public UserAttributeSyncResponse syncAttributes(Long id) {
        UserAccount user = findUser(id);
        UserProfile profile = findProfile(id);
        UserProfileVO beforeProfile = toProfileVO(profile);
        LocalDateTime startedAt = LocalDateTime.now();
        Map<String, Object> requestSummary = Map.of(
                "userId", user.getId(),
                "username", user.getUsername()
        );
        AttributeResult result;
        try {
            result = attributeMockAdapter.fetchAttributes(user.getId(), user.getUsername());
        } catch (RuntimeException exception) {
            externalIntegrationCallService.recordFailure(POST_QUANTUM_ATTRIBUTE, attributeMockAdapter.targetSystem(), String.valueOf(user.getId()), requestSummary, exception.getMessage(), startedAt);
            throw exception;
        }

        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(id);
        }
        profile.setDepartment(result.department());
        profile.setPositionName(result.positionName());
        profile.setAttributeVersion(result.attributeVersion());
        profile.setExternalSource(result.externalSource());
        profile.setExternalUserId(result.externalUserId());
        profile.setExternalAttributes(toJson(result.externalAttributes()));
        profile.setSyncedAt(LocalDateTime.now());
        if (profile.getId() == null) {
            userProfileMapper.insert(profile);
        } else {
            userProfileMapper.updateById(profile);
        }

        Map<String, Object> responseSummary = Map.of(
                "department", result.department(),
                "positionName", result.positionName(),
                "attributeVersion", result.attributeVersion(),
                "externalUserId", result.externalUserId()
        );
        Long callId = externalIntegrationCallService.recordSuccess(POST_QUANTUM_ATTRIBUTE, attributeMockAdapter.targetSystem(), String.valueOf(user.getId()), requestSummary, responseSummary, startedAt);
        UserProfileVO afterProfile = toProfileVO(profile);
        return new UserAttributeSyncResponse(
                toVO(user, profile),
                beforeProfile,
                afterProfile,
                new ExternalCallSummaryVO(callId, POST_QUANTUM_ATTRIBUTE, attributeMockAdapter.targetSystem(), String.valueOf(user.getId()), "SUCCESS")
        );
    }

    private UserAccount findUser(Long id) {
        UserAccount user = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getId, id)
                .eq(UserAccount::getDeleted, false));
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private UserProfile findProfile(Long userId) {
        return userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));
    }

    private UserVO toVO(UserAccount user, UserProfile profile) {
        return new UserVO(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getPhone(),
                user.getEmail(),
                user.getIdCardNo(),
                user.getStatus(),
                user.getRegisterSource(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                toProfileVO(profile)
        );
    }

    private UserSelfProfileVO toSelfVO(UserAccount user) {
        return new UserSelfProfileVO(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getPhone(),
                user.getEmail(),
                user.getIdCardNo()
        );
    }

    private UserProfileVO toProfileVO(UserProfile profile) {
        if (profile == null) {
            return null;
        }
        return new UserProfileVO(
                profile.getDepartment(),
                profile.getPositionName(),
                profile.getAttributeVersion(),
                profile.getExternalSource(),
                profile.getExternalUserId(),
                profile.getExternalAttributes(),
                profile.getSyncedAt()
        );
    }

    private String toJson(Map<String, Object> attributes) {
        try {
            return objectMapper.writeValueAsString(attributes);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "外部属性序列化失败");
        }
    }

    private String emptyToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
