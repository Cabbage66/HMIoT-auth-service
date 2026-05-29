package com.hmiot.hmiot_backend.module.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.common.security.JwtTokenProvider;
import com.hmiot.hmiot_backend.module.auth.dto.AdminLoginRequest;
import com.hmiot.hmiot_backend.module.auth.entity.SysAdmin;
import com.hmiot.hmiot_backend.module.auth.mapper.SysAdminMapper;
import com.hmiot.hmiot_backend.module.auth.vo.AdminLoginResponse;
import com.hmiot.hmiot_backend.module.audit.service.AuditLogService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AuthService {
    private final SysAdminMapper sysAdminMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditLogService auditLogService;

    public AuthService(SysAdminMapper sysAdminMapper, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider, AuditLogService auditLogService) {
        this.sysAdminMapper = sysAdminMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public AdminLoginResponse login(AdminLoginRequest request, String requestIp) {
        SysAdmin admin = sysAdminMapper.selectOne(new LambdaQueryWrapper<SysAdmin>()
                .eq(SysAdmin::getUsername, request.username())
                .eq(SysAdmin::getDeleted, false));
        if (admin == null || !passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (!"ENABLED".equals(admin.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "管理员账号已禁用");
        }
        admin.setLastLoginAt(LocalDateTime.now());
        admin.setLastLoginIp(requestIp);
        sysAdminMapper.updateById(admin);

        String token = jwtTokenProvider.generateToken(String.valueOf(admin.getId()), Map.of(
                "principalType", "ADMIN",
                "username", admin.getUsername(),
                "displayName", admin.getDisplayName(),
                "roleCode", admin.getRoleCode()
        ));
        auditLogService.recordLoginSuccess(admin.getId(), admin.getUsername(), requestIp);
        return new AdminLoginResponse(token, "Bearer", admin.getId(), admin.getUsername(), admin.getDisplayName(), admin.getRoleCode());
    }
}
