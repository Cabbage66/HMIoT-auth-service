package com.hmiot.hmiot_backend.module.binding.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.module.binding.dto.BindingRelationQueryRequest;
import com.hmiot.hmiot_backend.module.binding.dto.BindingUnbindRequest;
import com.hmiot.hmiot_backend.module.binding.dto.UserDynamicBiometricBindRequest;
import com.hmiot.hmiot_backend.module.binding.dto.UserTerminalBindRequest;
import com.hmiot.hmiot_backend.module.binding.entity.BindingRelation;
import com.hmiot.hmiot_backend.module.binding.mapper.BindingRelationMapper;
import com.hmiot.hmiot_backend.module.binding.vo.BindingRelationVO;
import com.hmiot.hmiot_backend.module.device.service.TerminalInfoService;
import com.hmiot.hmiot_backend.module.device.vo.TerminalInfoVO;
import com.hmiot.hmiot_backend.module.user.service.UserService;
import com.hmiot.hmiot_backend.module.user.vo.PageVO;
import com.hmiot.hmiot_backend.module.user.vo.UserVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
public class BindingRelationService {
    public static final String ACTIVE = "ACTIVE";
    public static final String INACTIVE = "INACTIVE";
    public static final String SUBJECT_USER = "USER";
    public static final String TARGET_TERMINAL = "TERMINAL";
    public static final String TARGET_DYNAMIC_BIOMETRIC = "DYNAMIC_BIOMETRIC";
    public static final String RELATION_LOGIN_TERMINAL = "LOGIN_TERMINAL";
    public static final String RELATION_DYNAMIC_BIOMETRIC_ID = "DYNAMIC_BIOMETRIC_ID";

    private final BindingRelationMapper bindingRelationMapper;
    private final UserService userService;
    private final TerminalInfoService terminalInfoService;

    public BindingRelationService(BindingRelationMapper bindingRelationMapper, UserService userService, TerminalInfoService terminalInfoService) {
        this.bindingRelationMapper = bindingRelationMapper;
        this.userService = userService;
        this.terminalInfoService = terminalInfoService;
    }

    public PageVO<BindingRelationVO> listBindings(BindingRelationQueryRequest request) {
        LambdaQueryWrapper<BindingRelation> wrapper = new LambdaQueryWrapper<BindingRelation>().orderByDesc(BindingRelation::getCreatedAt);
        if (StringUtils.hasText(request.subjectType())) {
            wrapper.eq(BindingRelation::getSubjectType, request.subjectType());
        }
        if (request.subjectId() != null) {
            wrapper.eq(BindingRelation::getSubjectId, request.subjectId());
        }
        if (StringUtils.hasText(request.targetType())) {
            wrapper.eq(BindingRelation::getTargetType, request.targetType());
        }
        if (request.targetId() != null) {
            wrapper.eq(BindingRelation::getTargetId, request.targetId());
        }
        if (StringUtils.hasText(request.relationType())) {
            wrapper.eq(BindingRelation::getRelationType, request.relationType());
        }
        if (StringUtils.hasText(request.status())) {
            wrapper.eq(BindingRelation::getStatus, request.status());
        }
        Page<BindingRelation> page = bindingRelationMapper.selectPage(Page.of(request.pageCurrent(), request.pageSize()), wrapper);
        List<BindingRelationVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageVO<>(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Transactional
    public BindingRelationVO bindUserTerminal(UserTerminalBindRequest request, Long adminId) {
        userService.getUser(request.userId());
        terminalInfoService.findTerminal(request.terminalId());
        BindingRelation existing = bindingRelationMapper.selectOne(activeUserTerminalWrapper(request.userId(), request.terminalId()).last("LIMIT 1"));
        if (existing != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户与终端已绑定");
        }
        BindingRelation binding = new BindingRelation();
        binding.setSubjectType(SUBJECT_USER);
        binding.setSubjectId(request.userId());
        binding.setTargetType(TARGET_TERMINAL);
        binding.setTargetId(request.terminalId());
        binding.setRelationType(RELATION_LOGIN_TERMINAL);
        binding.setStatus(ACTIVE);
        binding.setEffectiveAt(LocalDateTime.now());
        binding.setCreatedBy(adminId);
        binding.setReason(request.reason());
        bindingRelationMapper.insert(binding);
        return toVO(binding);
    }

    @Transactional
    public BindingRelationVO bindUserDynamicBiometric(UserDynamicBiometricBindRequest request, Long adminId) {
        userService.getUser(request.userId());
        BindingRelation existing = bindingRelationMapper.selectOne(activeUserDynamicBiometricWrapper(request.userId()).last("LIMIT 1"));
        if (existing != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户已存在有效动态生物绑定");
        }
        BindingRelation binding = new BindingRelation();
        binding.setSubjectType(SUBJECT_USER);
        binding.setSubjectId(request.userId());
        binding.setTargetType(TARGET_DYNAMIC_BIOMETRIC);
        binding.setTargetId(dynamicBiometricTargetId(normalizeExternalSubjectId(request.externalSubjectId())));
        binding.setRelationType(RELATION_DYNAMIC_BIOMETRIC_ID);
        binding.setStatus(ACTIVE);
        binding.setEffectiveAt(LocalDateTime.now());
        binding.setCreatedBy(adminId);
        binding.setReason(dynamicBiometricReason(request.externalSubjectId(), request.biometricType(), request.modelSource(), request.reason()));
        bindingRelationMapper.insert(binding);
        return toVO(binding);
    }

    @Transactional
    public BindingRelationVO unbind(Long id, BindingUnbindRequest request) {
        BindingRelation binding = findBinding(id);
        if (!ACTIVE.equals(binding.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "绑定关系已失效");
        }
        binding.setStatus(INACTIVE);
        binding.setExpiredAt(LocalDateTime.now());
        binding.setReason(StringUtils.hasText(request.reason()) ? request.reason() : binding.getReason());
        bindingRelationMapper.updateById(binding);
        return toVO(binding);
    }

    public List<BindingRelationVO> listUserTerminals(Long userId) {
        userService.getUser(userId);
        return bindingRelationMapper.selectList(activeUserBindingsWrapper(userId).orderByDesc(BindingRelation::getCreatedAt))
                .stream()
                .map(this::toVO)
                .toList();
    }

    public List<BindingRelationVO> listTerminalUsers(Long terminalId) {
        terminalInfoService.findTerminal(terminalId);
        return bindingRelationMapper.selectList(activeTerminalBindingsWrapper(terminalId).orderByDesc(BindingRelation::getCreatedAt))
                .stream()
                .map(this::toVO)
                .toList();
    }

    public List<BindingRelationVO> listUserDynamicBiometrics(Long userId) {
        userService.getUser(userId);
        return bindingRelationMapper.selectList(activeUserDynamicBiometricWrapper(userId).orderByDesc(BindingRelation::getCreatedAt))
                .stream()
                .map(this::toVO)
                .toList();
    }

    public List<BindingRelationVO> listDynamicBiometricUsers(String externalSubjectId) {
        Long targetId = dynamicBiometricTargetId(normalizeExternalSubjectId(externalSubjectId));
        return bindingRelationMapper.selectList(activeDynamicBiometricBindingsWrapper(targetId).orderByDesc(BindingRelation::getCreatedAt))
                .stream()
                .map(this::toVO)
                .toList();
    }

    public BindingRelation requireActiveDynamicBiometricBinding(Long userId, String externalSubjectId) {
        BindingRelation binding = bindingRelationMapper.selectOne(activeUserDynamicBiometricWrapper(userId)
                .eq(BindingRelation::getTargetId, dynamicBiometricTargetId(normalizeExternalSubjectId(externalSubjectId)))
                .last("LIMIT 1"));
        if (binding == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "用户动态生物绑定不存在或已失效");
        }
        return binding;
    }

    public BindingRelation findActiveDynamicBiometricBinding(Long userId) {
        BindingRelation binding = bindingRelationMapper.selectOne(activeUserDynamicBiometricWrapper(userId).last("LIMIT 1"));
        if (binding == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "用户未绑定动态生物特征");
        }
        return binding;
    }

    public Long dynamicBiometricTargetId(String externalSubjectId) {
        String digest = sha256(normalizeExternalSubjectId(externalSubjectId));
        return Long.parseUnsignedLong(digest.substring(0, 15), 16);
    }

    public BindingRelation findBinding(Long id) {
        BindingRelation binding = bindingRelationMapper.selectById(id);
        if (binding == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "绑定关系不存在");
        }
        return binding;
    }

    public BindingRelationVO toVO(BindingRelation binding) {
        UserVO user = null;
        TerminalInfoVO terminal = null;
        if (SUBJECT_USER.equals(binding.getSubjectType())) {
            user = userService.getUser(binding.getSubjectId());
        }
        if (TARGET_TERMINAL.equals(binding.getTargetType())) {
            terminal = terminalInfoService.getTerminal(binding.getTargetId());
        }
        return new BindingRelationVO(
                binding.getId(),
                binding.getSubjectType(),
                binding.getSubjectId(),
                binding.getTargetType(),
                binding.getTargetId(),
                binding.getRelationType(),
                binding.getStatus(),
                binding.getEffectiveAt(),
                binding.getExpiredAt(),
                binding.getCreatedBy(),
                binding.getReason(),
                binding.getCreatedAt(),
                binding.getUpdatedAt(),
                user,
                terminal
        );
    }

    private LambdaQueryWrapper<BindingRelation> activeUserTerminalWrapper(Long userId, Long terminalId) {
        return activeUserBindingsWrapper(userId).eq(BindingRelation::getTargetId, terminalId);
    }

    private LambdaQueryWrapper<BindingRelation> activeUserBindingsWrapper(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return new LambdaQueryWrapper<BindingRelation>()
                .eq(BindingRelation::getSubjectType, SUBJECT_USER)
                .eq(BindingRelation::getSubjectId, userId)
                .eq(BindingRelation::getTargetType, TARGET_TERMINAL)
                .eq(BindingRelation::getRelationType, RELATION_LOGIN_TERMINAL)
                .eq(BindingRelation::getStatus, ACTIVE)
                .le(BindingRelation::getEffectiveAt, now)
                .and(wrapper -> wrapper.isNull(BindingRelation::getExpiredAt).or().gt(BindingRelation::getExpiredAt, now));
    }

    private LambdaQueryWrapper<BindingRelation> activeTerminalBindingsWrapper(Long terminalId) {
        LocalDateTime now = LocalDateTime.now();
        return new LambdaQueryWrapper<BindingRelation>()
                .eq(BindingRelation::getSubjectType, SUBJECT_USER)
                .eq(BindingRelation::getTargetType, TARGET_TERMINAL)
                .eq(BindingRelation::getTargetId, terminalId)
                .eq(BindingRelation::getRelationType, RELATION_LOGIN_TERMINAL)
                .eq(BindingRelation::getStatus, ACTIVE)
                .le(BindingRelation::getEffectiveAt, now)
                .and(wrapper -> wrapper.isNull(BindingRelation::getExpiredAt).or().gt(BindingRelation::getExpiredAt, now));
    }

    private LambdaQueryWrapper<BindingRelation> activeUserDynamicBiometricWrapper(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return new LambdaQueryWrapper<BindingRelation>()
                .eq(BindingRelation::getSubjectType, SUBJECT_USER)
                .eq(BindingRelation::getSubjectId, userId)
                .eq(BindingRelation::getTargetType, TARGET_DYNAMIC_BIOMETRIC)
                .eq(BindingRelation::getRelationType, RELATION_DYNAMIC_BIOMETRIC_ID)
                .eq(BindingRelation::getStatus, ACTIVE)
                .le(BindingRelation::getEffectiveAt, now)
                .and(wrapper -> wrapper.isNull(BindingRelation::getExpiredAt).or().gt(BindingRelation::getExpiredAt, now));
    }

    private LambdaQueryWrapper<BindingRelation> activeDynamicBiometricBindingsWrapper(Long targetId) {
        LocalDateTime now = LocalDateTime.now();
        return new LambdaQueryWrapper<BindingRelation>()
                .eq(BindingRelation::getSubjectType, SUBJECT_USER)
                .eq(BindingRelation::getTargetType, TARGET_DYNAMIC_BIOMETRIC)
                .eq(BindingRelation::getTargetId, targetId)
                .eq(BindingRelation::getRelationType, RELATION_DYNAMIC_BIOMETRIC_ID)
                .eq(BindingRelation::getStatus, ACTIVE)
                .le(BindingRelation::getEffectiveAt, now)
                .and(wrapper -> wrapper.isNull(BindingRelation::getExpiredAt).or().gt(BindingRelation::getExpiredAt, now));
    }

    private String dynamicBiometricReason(String externalSubjectId, String biometricType, String modelSource, String reason) {
        String digest = sha256(normalizeExternalSubjectId(externalSubjectId));
        return "externalSubjectDigest=" + digest
                + ", biometricType=" + (StringUtils.hasText(biometricType) ? biometricType : TARGET_DYNAMIC_BIOMETRIC)
                + ", modelSource=" + (StringUtils.hasText(modelSource) ? modelSource : "dynamic-biometric")
                + (StringUtils.hasText(reason) ? ", reason=" + reason : "");
    }

    private String normalizeExternalSubjectId(String externalSubjectId) {
        if (!StringUtils.hasText(externalSubjectId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "动态生物外部标识不能为空");
        }
        return externalSubjectId.trim();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "SHA-256算法不可用");
        }
    }
}