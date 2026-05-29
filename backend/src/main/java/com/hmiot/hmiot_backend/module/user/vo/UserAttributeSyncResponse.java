package com.hmiot.hmiot_backend.module.user.vo;

public record UserAttributeSyncResponse(
        UserVO user,
        UserProfileVO beforeProfile,
        UserProfileVO afterProfile,
        ExternalCallSummaryVO externalCall
) {
}
