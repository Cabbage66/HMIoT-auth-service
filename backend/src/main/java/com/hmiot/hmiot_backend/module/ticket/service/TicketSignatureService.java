package com.hmiot.hmiot_backend.module.ticket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.module.ticket.config.TicketProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

@Service
public class TicketSignatureService {
    private final TicketProperties ticketProperties;
    private final ObjectMapper objectMapper;

    public TicketSignatureService(TicketProperties ticketProperties, ObjectMapper objectMapper) {
        this.ticketProperties = ticketProperties;
        this.objectMapper = objectMapper;
    }

    public String toPayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(new TreeMap<>(payload));
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "票据载荷序列化失败");
        }
    }

    public Map<String, Object> parsePayload(String payload) {
        try {
            return objectMapper.readValue(payload, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "票据载荷格式无效");
        }
    }

    public String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(ticketProperties.getSigningSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8))).toUpperCase();
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "票据签名失败");
        }
    }

    public boolean matches(String payload, String signatureDigest) {
        byte[] expected = sign(payload).getBytes(StandardCharsets.UTF_8);
        byte[] actual = signatureDigest.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    public String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8))).toUpperCase();
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "SHA-256算法不可用");
        }
    }
}
