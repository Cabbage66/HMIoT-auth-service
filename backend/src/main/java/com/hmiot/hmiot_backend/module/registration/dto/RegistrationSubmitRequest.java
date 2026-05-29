package com.hmiot.hmiot_backend.module.registration.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistrationSubmitRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 4, max = 64, message = "用户名长度需在4-64之间")
        String username,
        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 64, message = "密码长度需在8-64之间")
        String password,
        @NotBlank(message = "姓名不能为空")
        @Size(max = 64, message = "姓名长度不能超过64")
        String realName,
        @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
        String phone,
        @Email(message = "邮箱格式不正确")
        String email,
        @Size(max = 64, message = "证件号长度不能超过64")
        String idCardNo
) {
}
