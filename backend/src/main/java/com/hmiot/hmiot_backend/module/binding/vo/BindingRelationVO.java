package com.hmiot.hmiot_backend.module.binding.vo;

import com.hmiot.hmiot_backend.module.device.vo.TerminalInfoVO;
import com.hmiot.hmiot_backend.module.user.vo.UserVO;

import java.time.LocalDateTime;

public record BindingRelationVO(
        Long id,
        String subjectType,
        Long subjectId,
        String targetType,
        Long targetId,
        String relationType,
        String status,
        LocalDateTime effectiveAt,
        LocalDateTime expiredAt,
        Long createdBy,
        String reason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        UserVO user,
        TerminalInfoVO terminal
) {
}
