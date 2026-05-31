package com.hmiot.hmiot_backend.module.trust.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.common.security.CurrentUser;
import com.hmiot.hmiot_backend.module.trust.dto.TerminalEventQueryRequest;
import com.hmiot.hmiot_backend.module.trust.dto.TerminalEventReportRequest;
import com.hmiot.hmiot_backend.module.trust.entity.TerminalEvent;
import com.hmiot.hmiot_backend.module.trust.mapper.TerminalEventMapper;
import com.hmiot.hmiot_backend.module.trust.vo.TerminalEventVO;
import com.hmiot.hmiot_backend.module.user.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class TerminalEventService {
    private static final int MAX_VALUE_LENGTH = 512;
    private static final int MAX_PAYLOAD_KEYS = 40;
    private static final Set<String> LEVELS = Set.of("INFO", "WARN", "ERROR", "CRITICAL");
    private static final Set<String> SENSITIVE_KEY_MARKERS = Set.of("password", "passwd", "secret", "token", "privatekey", "private_key", "rawaudio", "raw_audio", "video", "image", "sample", "biometric", "fingerprint");

    private final TerminalEventMapper terminalEventMapper;
    private final TerminalEventFileLogService terminalEventFileLogService;
    private final ObjectMapper objectMapper;

    public TerminalEventService(TerminalEventMapper terminalEventMapper, TerminalEventFileLogService terminalEventFileLogService, ObjectMapper objectMapper) {
        this.terminalEventMapper = terminalEventMapper;
        this.terminalEventFileLogService = terminalEventFileLogService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TerminalEventVO report(TerminalEventReportRequest request, CurrentUser currentUser) {
        if (currentUser.terminalId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前用户缺少可信终端上下文，请重新登录");
        }
        Map<String, Object> payload = sanitize(request.eventPayload());
        Map<String, Object> normalized = normalize(request, payload, currentUser);
        TerminalEvent event = new TerminalEvent();
        event.setTerminalId(currentUser.terminalId());
        event.setUserId(currentUser.userId());
        event.setEventType(normalizeType(request.eventType()));
        event.setEventLevel(normalizeLevel(request.eventLevel(), payload));
        event.setEventTime(request.eventTime() == null ? LocalDateTime.now() : request.eventTime());
        event.setEventPayload(toJson(payload));
        event.setNormalizedPayload(toJson(normalized));
        event.setReportedAt(LocalDateTime.now());
        terminalEventMapper.insert(event);
        terminalEventFileLogService.logEventText(currentUser.terminalId(), event.getEventType(), event.getEventLevel(), payload, normalized);
        return toVO(event, true);
    }

    public PageVO<TerminalEventVO> listEvents(TerminalEventQueryRequest request) {
        LambdaQueryWrapper<TerminalEvent> wrapper = new LambdaQueryWrapper<TerminalEvent>()
                .orderByDesc(TerminalEvent::getEventTime);
        if (request.userId() != null) {
            wrapper.eq(TerminalEvent::getUserId, request.userId());
        }
        if (request.terminalId() != null) {
            wrapper.eq(TerminalEvent::getTerminalId, request.terminalId());
        }
        if (StringUtils.hasText(request.eventType())) {
            wrapper.eq(TerminalEvent::getEventType, normalizeType(request.eventType()));
        }
        if (StringUtils.hasText(request.eventLevel())) {
            wrapper.eq(TerminalEvent::getEventLevel, normalizeLevel(request.eventLevel(), Map.of()));
        }
        Page<TerminalEvent> page = terminalEventMapper.selectPage(Page.of(request.pageCurrent(), request.pageSize()), wrapper);
        List<TerminalEventVO> records = page.getRecords().stream().map(event -> toVO(event, true)).toList();
        return new PageVO<>(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    public TerminalEventVO detail(Long id) {
        TerminalEvent event = terminalEventMapper.selectById(id);
        if (event == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "终端事件不存在");
        }
        return toVO(event, true);
    }

    public List<TerminalEventVO> recentForCurrentUser(CurrentUser currentUser) {
        if (currentUser.terminalId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前用户缺少可信终端上下文，请重新登录");
        }
        return recentEvents(currentUser.userId(), currentUser.terminalId(), 20).stream()
                .map(event -> toVO(event, false))
                .toList();
    }

    public List<TerminalEvent> recentEvents(Long userId, Long terminalId, int limit) {
        return terminalEventMapper.selectList(new LambdaQueryWrapper<TerminalEvent>()
                .eq(TerminalEvent::getUserId, userId)
                .eq(TerminalEvent::getTerminalId, terminalId)
                .orderByDesc(TerminalEvent::getEventTime)
                .last("LIMIT " + Math.max(1, Math.min(limit, 100))));
    }

    public TerminalEventVO toVO(TerminalEvent event, boolean includePayload) {
        return new TerminalEventVO(
                event.getId(),
                event.getTerminalId(),
                event.getUserId(),
                event.getEventType(),
                event.getEventLevel(),
                event.getEventTime(),
                includePayload ? fromJson(event.getEventPayload()) : null,
                fromJson(event.getNormalizedPayload()),
                event.getReportedAt()
        );
    }

    private Map<String, Object> normalize(TerminalEventReportRequest request, Map<String, Object> payload, CurrentUser currentUser) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("eventType", normalizeType(request.eventType()));
        normalized.put("eventLevel", normalizeLevel(request.eventLevel(), payload));
        normalized.put("terminalId", currentUser.terminalId());
        normalized.put("clientType", currentUser.clientType() == null ? "" : currentUser.clientType());
        normalized.put("eventTime", request.eventTime() == null ? LocalDateTime.now() : request.eventTime());
        normalized.put("payload", payload);
        return normalized;
    }

    private String normalizeType(String eventType) {
        String normalized = eventType == null ? "UNKNOWN" : eventType.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_:-]", "_");
        return normalized.length() > 64 ? normalized.substring(0, 64) : normalized;
    }

    private String normalizeLevel(String eventLevel, Map<String, Object> payload) {
        String normalized = eventLevel == null ? "" : eventLevel.trim().toUpperCase(Locale.ROOT);
        if (LEVELS.contains(normalized)) {
            return normalized;
        }
        String marker = payload == null ? "" : payload.toString().toLowerCase(Locale.ROOT);
        if (marker.contains("critical") || marker.contains("malware") || marker.contains("tamper")) {
            return "CRITICAL";
        }
        if (marker.contains("error") || marker.contains("deny")) {
            return "ERROR";
        }
        if (marker.contains("warn") || marker.contains("risk") || marker.contains("suspicious")) {
            return "WARN";
        }
        return "INFO";
    }

    private Map<String, Object> sanitize(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> sanitized = new LinkedHashMap<>();
        int count = 0;
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            if (count >= MAX_PAYLOAD_KEYS) {
                break;
            }
            String key = entry.getKey() == null ? "unknown" : entry.getKey();
            if (isSensitiveKey(key)) {
                continue;
            }
            sanitized.put(key, sanitizeValue(entry.getValue()));
            count++;
        }
        return sanitized;
    }

    private Object sanitizeValue(Object value) {
        if (value == null || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            int count = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (count >= MAX_PAYLOAD_KEYS) {
                    break;
                }
                String key = String.valueOf(entry.getKey());
                if (!isSensitiveKey(key)) {
                    sanitized.put(key, sanitizeValue(entry.getValue()));
                    count++;
                }
            }
            return sanitized;
        }
        if (value instanceof Iterable<?> iterable) {
            java.util.ArrayList<Object> sanitized = new java.util.ArrayList<>();
            int count = 0;
            for (Object item : iterable) {
                if (count >= 20) {
                    break;
                }
                sanitized.add(sanitizeValue(item));
                count++;
            }
            return sanitized;
        }
        String stringValue = String.valueOf(value);
        return stringValue.length() > MAX_VALUE_LENGTH ? stringValue.substring(0, MAX_VALUE_LENGTH) : stringValue;
    }

    private boolean isSensitiveKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT).replace("-", "_");
        return SENSITIVE_KEY_MARKERS.stream().anyMatch(normalized::contains);
    }

    private String toJson(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "终端事件报文序列化失败");
        }
    }

    private Map<String, Object> fromJson(String value) {
        if (!StringUtils.hasText(value)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            return Map.of();
        }
    }
}
