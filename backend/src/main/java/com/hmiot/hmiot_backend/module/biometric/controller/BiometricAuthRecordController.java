package com.hmiot.hmiot_backend.module.biometric.controller;

import com.hmiot.hmiot_backend.common.response.ApiResponse;
import com.hmiot.hmiot_backend.module.biometric.dto.BiometricAuthRecordQueryRequest;
import com.hmiot.hmiot_backend.module.biometric.service.BiometricTemplateService;
import com.hmiot.hmiot_backend.module.biometric.vo.BiometricAuthRecordVO;
import com.hmiot.hmiot_backend.module.user.vo.PageVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/biometric-auth-records")
public class BiometricAuthRecordController {
    private final BiometricTemplateService biometricTemplateService;

    public BiometricAuthRecordController(BiometricTemplateService biometricTemplateService) {
        this.biometricTemplateService = biometricTemplateService;
    }

    @GetMapping
    public ApiResponse<PageVO<BiometricAuthRecordVO>> listAuthRecords(BiometricAuthRecordQueryRequest request) {
        return ApiResponse.success(biometricTemplateService.listAuthRecords(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<BiometricAuthRecordVO> getAuthRecord(@PathVariable Long id) {
        return ApiResponse.success(biometricTemplateService.getAuthRecord(id));
    }
}
