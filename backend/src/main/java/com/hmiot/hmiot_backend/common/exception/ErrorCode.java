package com.hmiot.hmiot_backend.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    SUCCESS("0", "success", HttpStatus.OK),
    BAD_REQUEST("400", "请求参数错误", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("401", "未认证或认证已过期", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("403", "无访问权限", HttpStatus.FORBIDDEN),
    NOT_FOUND("404", "资源不存在", HttpStatus.NOT_FOUND),
    BUSINESS_ERROR("1000", "业务处理失败", HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR("500", "系统内部错误", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
