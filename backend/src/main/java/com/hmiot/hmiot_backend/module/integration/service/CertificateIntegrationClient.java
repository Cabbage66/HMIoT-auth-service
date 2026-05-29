package com.hmiot.hmiot_backend.module.integration.service;

import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
public class CertificateIntegrationClient {
    private final String certificateServiceUrl;
    private final RestClient restClient;

    public CertificateIntegrationClient(@Value("${hmiot.integrations.certificate-service-url:http://localhost:5000}") String certificateServiceUrl, RestClient.Builder restClientBuilder) {
        this.certificateServiceUrl = certificateServiceUrl;
        this.restClient = certificateServiceUrl.startsWith("http://") || certificateServiceUrl.startsWith("https://")
                ? restClientBuilder.baseUrl(certificateServiceUrl).build()
                : null;
    }

    public CertificateIssueResult issueCertificate(String username, String publicKeyPem) {
        if (certificateServiceUrl.startsWith("mock://")) {
            String fingerprint = mockFingerprint(username);
            return new CertificateIssueResult(
                    username,
                    mockSerial(fingerprint),
                    fingerprint,
                    "-----BEGIN CERTIFICATE-----\nMOCK-" + username + "\n-----END CERTIFICATE-----"
            );
        }
        RegisterResponse response = restClient.post()
                .uri("/register")
                .body(new RegisterRequest(username, publicKeyPem))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "证书签发服务调用失败");
                })
                .body(RegisterResponse.class);
        if (response == null || response.serial() == null || response.fingerprint_sha256() == null || response.certificate_pem() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "证书签发服务响应不完整");
        }
        return new CertificateIssueResult(response.username(), response.serial(), response.fingerprint_sha256(), response.certificate_pem());
    }

    public ChallengeResult requestChallenge(String username) {
        if (certificateServiceUrl.startsWith("mock://")) {
            return new ChallengeResult(UUID.randomUUID().toString(), UUID.randomUUID().toString().replace("-", ""), 120);
        }
        LoginResponse response = restClient.post()
                .uri("/login")
                .body(Map.of("phase", "request", "username", username))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "证书挑战码服务调用失败");
                })
                .body(LoginResponse.class);
        if (response == null || response.challenge_id() == null || response.challenge() == null || response.expires_in_seconds() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "证书挑战码服务响应不完整");
        }
        return new ChallengeResult(response.challenge_id(), response.challenge(), response.expires_in_seconds());
    }

    public VerifyResult verifyChallenge(String username, String challengeId, String signatureB64) {
        if (certificateServiceUrl.startsWith("mock://")) {
            boolean verified = signatureB64.startsWith("mock-signature");
            String fingerprint = mockFingerprint(username);
            return new VerifyResult(verified, username, mockSerial(fingerprint), fingerprint);
        }
        LoginResponse response = restClient.post()
                .uri("/login")
                .body(Map.of(
                        "phase", "verify",
                        "username", username,
                        "challenge_id", challengeId,
                        "signature_b64", signatureB64
                ))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "证书验签服务调用失败");
                })
                .body(LoginResponse.class);
        if (response == null || response.verified() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "证书验签服务响应不完整");
        }
        return new VerifyResult(Boolean.TRUE.equals(response.verified()), response.username(), response.serial(), response.fingerprint_sha256());
    }

    public String targetSystem() {
        return certificateServiceUrl;
    }

    public LocalDateTime defaultIssuedAt() {
        return LocalDateTime.now();
    }

    private String mockFingerprint(String username) {
        return sha256(username).toUpperCase();
    }

    private String mockSerial(String fingerprint) {
        return "MOCK-" + fingerprint.substring(0, 16);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "SHA-256算法不可用");
        }
    }

    private record RegisterRequest(String username, String public_key_pem) {
    }

    private record RegisterResponse(String username, String serial, String fingerprint_sha256, String certificate_pem) {
    }

    private record LoginResponse(String challenge_id, String challenge, Integer expires_in_seconds, Boolean verified, String username, String serial, String fingerprint_sha256) {
    }

    public record CertificateIssueResult(String username, String serial, String fingerprintSha256, String certificatePem) {
    }

    public record ChallengeResult(String challengeId, String challenge, Integer expiresInSeconds) {
    }

    public record VerifyResult(Boolean verified, String username, String serial, String fingerprintSha256) {
    }
}
