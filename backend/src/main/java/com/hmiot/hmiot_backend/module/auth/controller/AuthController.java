package com.hmiot.hmiot_backend.module.auth.controller;

import com.hmiot.hmiot_backend.common.response.ApiResponse;
import com.hmiot.hmiot_backend.module.auth.dto.AdminLoginRequest;
import com.hmiot.hmiot_backend.module.auth.dto.BiometricLoginRequest;
import com.hmiot.hmiot_backend.module.auth.dto.CertificateChallengeRequest;
import com.hmiot.hmiot_backend.module.auth.dto.CertificateVerifyRequest;
import com.hmiot.hmiot_backend.module.auth.dto.FaceLoginRequest;
import com.hmiot.hmiot_backend.module.auth.dto.FingerprintChallengeRequest;
import com.hmiot.hmiot_backend.module.auth.dto.FingerprintLoginRequest;
import com.hmiot.hmiot_backend.module.auth.dto.PasswordLoginRequest;
import com.hmiot.hmiot_backend.module.auth.dto.VoiceprintLoginRequest;
import com.hmiot.hmiot_backend.module.auth.service.AuthService;
import com.hmiot.hmiot_backend.module.auth.service.BiometricLoginService;
import com.hmiot.hmiot_backend.module.auth.service.CertificateLoginService;
import com.hmiot.hmiot_backend.module.auth.service.FaceLoginService;
import com.hmiot.hmiot_backend.module.auth.service.FingerprintLoginService;
import com.hmiot.hmiot_backend.module.auth.service.PasswordLoginService;
import com.hmiot.hmiot_backend.module.auth.vo.AdminLoginResponse;
import com.hmiot.hmiot_backend.module.auth.vo.CertificateChallengeResponse;
import com.hmiot.hmiot_backend.module.auth.vo.FingerprintChallengeResponse;
import com.hmiot.hmiot_backend.module.auth.vo.UserLoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final CertificateLoginService certificateLoginService;
    private final FingerprintLoginService fingerprintLoginService;
    private final FaceLoginService faceLoginService;
    private final PasswordLoginService passwordLoginService;
    private final BiometricLoginService biometricLoginService;

    public AuthController(AuthService authService, CertificateLoginService certificateLoginService, FingerprintLoginService fingerprintLoginService, FaceLoginService faceLoginService, PasswordLoginService passwordLoginService, BiometricLoginService biometricLoginService) {
        this.authService = authService;
        this.certificateLoginService = certificateLoginService;
        this.fingerprintLoginService = fingerprintLoginService;
        this.faceLoginService = faceLoginService;
        this.passwordLoginService = passwordLoginService;
        this.biometricLoginService = biometricLoginService;
    }

    @PostMapping("/admin/login")
    public ApiResponse<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.login(request, servletRequest.getRemoteAddr()));
    }

    @PostMapping("/password/login")
    public ApiResponse<UserLoginResponse> passwordLogin(@Valid @RequestBody PasswordLoginRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.success(passwordLoginService.login(request, servletRequest.getRemoteAddr()));
    }

    @PostMapping("/usb/challenge")
    public ApiResponse<CertificateChallengeResponse> requestUsbChallenge(@Valid @RequestBody CertificateChallengeRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.success(certificateLoginService.requestChallenge(request, "USB_LOGIN", servletRequest.getRemoteAddr()));
    }

    @PostMapping("/usb/verify")
    public ApiResponse<UserLoginResponse> verifyUsbLogin(@Valid @RequestBody CertificateVerifyRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.success(certificateLoginService.verify(request, "USB_LOGIN", servletRequest.getRemoteAddr()));
    }

    @PostMapping("/certificate/challenge")
    public ApiResponse<CertificateChallengeResponse> requestCertificateChallenge(@Valid @RequestBody CertificateChallengeRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.success(certificateLoginService.requestChallenge(request, "CERTIFICATE_LOGIN", servletRequest.getRemoteAddr()));
    }

    @PostMapping("/certificate/verify")
    public ApiResponse<UserLoginResponse> verifyCertificateLogin(@Valid @RequestBody CertificateVerifyRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.success(certificateLoginService.verify(request, "CERTIFICATE_LOGIN", servletRequest.getRemoteAddr()));
    }

    @PostMapping("/fingerprint/challenge")
    public ApiResponse<FingerprintChallengeResponse> fingerprintChallenge(@Valid @RequestBody FingerprintChallengeRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.success(fingerprintLoginService.requestChallenge(request, servletRequest.getRemoteAddr()));
    }

    @PostMapping("/fingerprint/login")
    public ApiResponse<UserLoginResponse> fingerprintLogin(@Valid @RequestBody FingerprintLoginRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.success(fingerprintLoginService.login(request, servletRequest.getRemoteAddr()));
    }

    @PostMapping("/face/login")
    public ApiResponse<UserLoginResponse> faceLogin(@Valid @RequestBody FaceLoginRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.success(faceLoginService.login(request, servletRequest.getRemoteAddr()));
    }

    @PostMapping("/voiceprint/login")
    public ApiResponse<UserLoginResponse> voiceprintLogin(@Valid @RequestBody VoiceprintLoginRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.success(biometricLoginService.loginByVoiceprint(request, servletRequest.getRemoteAddr()));
    }
}
