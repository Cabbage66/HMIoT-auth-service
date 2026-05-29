package com.hmiot.hmiot_backend.common.response;

import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import org.slf4j.MDC;

public record ApiResponse<T>(String code, String message, T data, String traceId) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data, currentTraceId());
    }

    public static ApiResponse<Void> success() {
        return success(null);
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode) {
        return fail(errorCode.getCode(), errorCode.getMessage());
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode, String message) {
        return fail(errorCode.getCode(), message);
    }

    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(code, message, null, currentTraceId());
    }

    private static String currentTraceId() {
        return MDC.get("traceId");
    }
}
