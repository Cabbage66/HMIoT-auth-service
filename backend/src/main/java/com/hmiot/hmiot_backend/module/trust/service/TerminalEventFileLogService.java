package com.hmiot.hmiot_backend.module.trust.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmiot.hmiot_backend.common.exception.BusinessException;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class TerminalEventFileLogService {
    private final ObjectMapper objectMapper;
    private final Path baseDir;

    public TerminalEventFileLogService(ObjectMapper objectMapper, @Value("${hmiot.trust.terminal-log-dir:runtime/terminal-event-logs}") String logDir) {
        this.objectMapper = objectMapper;
        this.baseDir = Paths.get(logDir).toAbsolutePath().normalize();
    }

    public String logEventText(Long terminalId, String eventType, String eventLevel, Map<String, Object> payload, Map<String, Object> normalized) {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        Path dir = baseDir.resolve(date);
        String fileName = "terminal-" + (terminalId == null ? "unknown" : terminalId) + ".log";
        Path file = dir.resolve(fileName);
        String line = String.format(
                "[%s] type=%s level=%s payload=%s normalized=%s%n",
                LocalDateTime.now(),
                eventType == null ? "" : eventType,
                eventLevel == null ? "" : eventLevel,
                toJson(payload),
                toJson(normalized)
        );
        try {
            Files.createDirectories(dir);
            Files.writeString(file, line, StandardCharsets.UTF_8, Files.exists(file) ? java.nio.file.StandardOpenOption.APPEND : java.nio.file.StandardOpenOption.CREATE);
            return file.toString();
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "终端事件日志写入失败");
        }
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
