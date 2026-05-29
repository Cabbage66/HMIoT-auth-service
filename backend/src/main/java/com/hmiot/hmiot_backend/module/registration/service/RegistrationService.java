package com.hmiot.hmiot_backend.module.registration.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.module.registration.dto.AdminRegistrationQueryRequest;
import com.hmiot.hmiot_backend.module.registration.dto.OfflineRegistrationBatchRequest;
import com.hmiot.hmiot_backend.module.registration.dto.OfflineRegistrationCreateRequest;
import com.hmiot.hmiot_backend.module.registration.dto.RegistrationProgressQueryRequest;
import com.hmiot.hmiot_backend.module.registration.dto.RegistrationSubmitRequest;
import com.hmiot.hmiot_backend.module.registration.entity.RegistrationRequest;
import com.hmiot.hmiot_backend.module.registration.mapper.RegistrationRequestMapper;
import com.hmiot.hmiot_backend.module.registration.vo.AdminRegistrationVO;
import com.hmiot.hmiot_backend.module.registration.vo.OfflineRegistrationBatchItemVO;
import com.hmiot.hmiot_backend.module.registration.vo.OfflineRegistrationBatchResponse;
import com.hmiot.hmiot_backend.module.registration.vo.RegistrationProgressVO;
import com.hmiot.hmiot_backend.module.registration.vo.RegistrationSubmitResponse;
import com.hmiot.hmiot_backend.module.user.entity.UserAccount;
import com.hmiot.hmiot_backend.module.user.mapper.UserAccountMapper;
import com.hmiot.hmiot_backend.module.user.vo.PageVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class RegistrationService {
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String USER_STATUS_ENABLED = "ENABLED";
    private static final String USER_STATUS_DISABLED = "DISABLED";
    private static final String REGISTER_SOURCE_ONLINE = "ONLINE";
    private static final String REGISTER_SOURCE_OFFLINE = "OFFLINE";

    private final UserAccountMapper userAccountMapper;
    private final RegistrationRequestMapper registrationRequestMapper;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public RegistrationService(
            UserAccountMapper userAccountMapper,
            RegistrationRequestMapper registrationRequestMapper,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper,
            TransactionTemplate transactionTemplate
    ) {
        this.userAccountMapper = userAccountMapper;
        this.registrationRequestMapper = registrationRequestMapper;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
    }

    @Transactional
    public RegistrationSubmitResponse submit(RegistrationSubmitRequest request) {
        return createRegistration(
                request.username(),
                request.password(),
                request.realName(),
                request.phone(),
                request.email(),
                request.idCardNo(),
                REGISTER_SOURCE_ONLINE,
                false,
                null
        );
    }

    public PageVO<RegistrationProgressVO> progress(RegistrationProgressQueryRequest request) {
        Page<RegistrationRequest> page = registrationRequestMapper.selectPage(
                Page.of(request.pageCurrent(), request.pageSize()),
                new LambdaQueryWrapper<RegistrationRequest>()
                        .eq(RegistrationRequest::getUsername, request.username().trim())
                        .orderByDesc(RegistrationRequest::getCreatedAt)
        );
        List<RegistrationProgressVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageVO<>(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    public PageVO<AdminRegistrationVO> listAdminRegistrations(AdminRegistrationQueryRequest request) {
        LambdaQueryWrapper<RegistrationRequest> wrapper = new LambdaQueryWrapper<RegistrationRequest>()
                .orderByDesc(RegistrationRequest::getCreatedAt);
        if (StringUtils.hasText(request.keyword())) {
            wrapper.and(condition -> condition.like(RegistrationRequest::getUsername, request.keyword())
                    .or().like(RegistrationRequest::getRealName, request.keyword())
                    .or().like(RegistrationRequest::getPhone, request.keyword())
                    .or().like(RegistrationRequest::getEmail, request.keyword())
                    .or().like(RegistrationRequest::getIdCardNo, request.keyword()));
        }
        if (StringUtils.hasText(request.status())) {
            wrapper.eq(RegistrationRequest::getStatus, request.status());
        }
        Page<RegistrationRequest> page = registrationRequestMapper.selectPage(Page.of(request.pageCurrent(), request.pageSize()), wrapper);
        Map<Long, UserAccount> userMap = loadUserMap(page.getRecords());
        List<AdminRegistrationVO> records = page.getRecords().stream()
                .map(registration -> toAdminVO(registration, userMap.get(registration.getUserId())))
                .toList();
        return new PageVO<>(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Transactional
    public AdminRegistrationVO approveRegistration(Long registrationId, Long reviewerId) {
        RegistrationRequest registration = findRegistration(registrationId);
        ensureReviewable(registration);
        UserAccount user = findUser(registration.getUserId());
        registration.setStatus(STATUS_APPROVED);
        registration.setReviewerId(reviewerId);
        registration.setReviewedAt(LocalDateTime.now());
        registration.setRejectReason(null);
        registrationRequestMapper.updateById(registration);
        user.setStatus(USER_STATUS_ENABLED);
        userAccountMapper.updateById(user);
        return toAdminVO(registration, user);
    }

    @Transactional
    public AdminRegistrationVO rejectRegistration(Long registrationId, Long reviewerId, String rejectReason) {
        RegistrationRequest registration = findRegistration(registrationId);
        ensureReviewable(registration);
        UserAccount user = findUser(registration.getUserId());
        registration.setStatus(STATUS_REJECTED);
        registration.setReviewerId(reviewerId);
        registration.setReviewedAt(LocalDateTime.now());
        registration.setRejectReason(rejectReason == null ? null : rejectReason.trim());
        registrationRequestMapper.updateById(registration);
        user.setStatus(USER_STATUS_DISABLED);
        userAccountMapper.updateById(user);
        return toAdminVO(registration, user);
    }

    @Transactional
    public RegistrationSubmitResponse createOfflineRegistration(OfflineRegistrationCreateRequest request, Long reviewerId) {
        boolean autoApprove = request.autoApprove() == null || request.autoApprove();
        return createRegistration(
                request.username(),
                request.password(),
                request.realName(),
                request.phone(),
                request.email(),
                request.idCardNo(),
                REGISTER_SOURCE_OFFLINE,
                autoApprove,
                reviewerId
        );
    }

    public OfflineRegistrationBatchResponse createOfflineBatch(OfflineRegistrationBatchRequest request, Long reviewerId) {
        List<OfflineRegistrationBatchItemVO> items = request.items().stream().map(item -> {
            try {
                RegistrationSubmitResponse response = transactionTemplate.execute(status ->
                        createRegistration(
                                item.username(),
                                item.password(),
                                item.realName(),
                                item.phone(),
                                item.email(),
                                item.idCardNo(),
                                REGISTER_SOURCE_OFFLINE,
                                item.autoApprove() == null || item.autoApprove(),
                                reviewerId
                        )
                );
                if (response == null) {
                    return new OfflineRegistrationBatchItemVO(item.username(), false, "创建失败", null, null);
                }
                return new OfflineRegistrationBatchItemVO(item.username(), true, "SUCCESS", response.registrationId(), response.userId());
            } catch (RuntimeException exception) {
                return new OfflineRegistrationBatchItemVO(item.username(), false, exception.getMessage(), null, null);
            }
        }).toList();
        int total = items.size();
        int successCount = (int) items.stream().filter(OfflineRegistrationBatchItemVO::success).count();
        return new OfflineRegistrationBatchResponse(total, successCount, total - successCount, items);
    }

    private RegistrationSubmitResponse createRegistration(
            String username,
            String password,
            String realName,
            String phone,
            String email,
            String idCardNo,
            String registerSource,
            boolean autoApprove,
            Long reviewerId
    ) {
        ensureUsernameAvailable(username);
        UserAccount user = new UserAccount();
        user.setUsername(username.trim());
        user.setPasswordHash(passwordEncoder.encode(StringUtils.hasText(password) ? password : fallbackPassword()));
        user.setRealName(realName.trim());
        user.setPhone(emptyToNull(phone));
        user.setEmail(emptyToNull(email));
        user.setIdCardNo(emptyToNull(idCardNo));
        user.setStatus(autoApprove ? USER_STATUS_ENABLED : STATUS_PENDING);
        user.setRegisterSource(registerSource);
        userAccountMapper.insert(user);

        RegistrationRequest registration = new RegistrationRequest();
        registration.setUserId(user.getId());
        registration.setUsername(user.getUsername());
        registration.setRealName(user.getRealName());
        registration.setPhone(user.getPhone());
        registration.setEmail(user.getEmail());
        registration.setIdCardNo(user.getIdCardNo());
        registration.setStatus(autoApprove ? STATUS_APPROVED : STATUS_PENDING);
        if (autoApprove) {
            registration.setReviewerId(reviewerId);
            registration.setReviewedAt(LocalDateTime.now());
            registration.setRejectReason(null);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("username", user.getUsername());
        payload.put("realName", user.getRealName());
        payload.put("phone", user.getPhone() == null ? "" : user.getPhone());
        payload.put("email", user.getEmail() == null ? "" : user.getEmail());
        payload.put("idCardNo", user.getIdCardNo() == null ? "" : user.getIdCardNo());
        payload.put("registerSource", registerSource);
        payload.put("autoApprove", autoApprove);
        registration.setRequestPayload(toJson(payload));
        registrationRequestMapper.insert(registration);
        return new RegistrationSubmitResponse(registration.getId(), user.getId(), user.getUsername(), registration.getStatus(), registration.getCreatedAt());
    }

    private RegistrationProgressVO toVO(RegistrationRequest request) {
        return new RegistrationProgressVO(
                request.getId(),
                request.getUsername(),
                request.getRealName(),
                request.getPhone(),
                request.getEmail(),
                request.getStatus(),
                request.getRejectReason(),
                request.getReviewedAt(),
                request.getCreatedAt()
        );
    }

    private AdminRegistrationVO toAdminVO(RegistrationRequest request, UserAccount user) {
        return new AdminRegistrationVO(
                request.getId(),
                request.getUserId(),
                request.getUsername(),
                request.getRealName(),
                request.getPhone(),
                request.getEmail(),
                request.getIdCardNo(),
                user == null ? null : user.getRegisterSource(),
                request.getStatus(),
                request.getReviewerId(),
                request.getReviewedAt(),
                request.getRejectReason(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }

    private RegistrationRequest findRegistration(Long id) {
        RegistrationRequest registration = registrationRequestMapper.selectById(id);
        if (registration == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "注册申请不存在");
        }
        return registration;
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

    private void ensureReviewable(RegistrationRequest request) {
        if (!STATUS_PENDING.equals(request.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前申请状态不可审核");
        }
    }

    private Map<Long, UserAccount> loadUserMap(List<RegistrationRequest> registrations) {
        List<Long> userIds = registrations.stream().map(RegistrationRequest::getUserId).filter(Objects::nonNull).distinct().toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<UserAccount> users = userAccountMapper.selectBatchIds(userIds);
        Map<Long, UserAccount> map = new HashMap<>(users.size());
        for (UserAccount user : users) {
            map.put(user.getId(), user);
        }
        return map;
    }

    private void ensureUsernameAvailable(String username) {
        UserAccount existing = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getUsername, username.trim())
                .eq(UserAccount::getDeleted, false));
        if (existing != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名已存在");
        }
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "注册载荷序列化失败");
        }
    }

    private String fallbackPassword() {
        return "Temp#" + UUID.randomUUID().toString().replace("-", "").substring(0, 10) + "A1";
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
