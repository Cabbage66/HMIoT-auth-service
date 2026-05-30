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
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class TrustAssessmentFileLogService {
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private final ObjectMapper objectMapper;
    private final Path baseDir;

    public TrustAssessmentFileLogService(ObjectMapper objectMapper, @Value("${hmiot.trust.log-dir:runtime/trust-assessment-logs}") String logDir) {
        this.objectMapper = objectMapper;
        this.baseDir = Paths.get(logDir).toAbsolutePath().normalize();
    }

    public String writeRequestLog(String requestId, Map<String, Object> inputPayload) {
        return writeLog(requestId, "request", inputPayload);
    }

    public String writeResultLog(String requestId, Map<String, Object> resultPayload) {
        return writeLog(requestId, "result", resultPayload);
    }

    private String writeLog(String requestId, String type, Map<String, Object> payload) {
        String safeRequestId = requestId == null ? "unknown" : requestId.replaceAll("[^a-zA-Z0-9_-]", "_");
        String dateDir = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        Path outputDir = baseDir.resolve(dateDir);
        String fileName = safeRequestId + "-" + type + "-" + LocalDateTime.now().format(FILE_TS) + ".json";
        Path file = outputDir.resolve(fileName);
        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("requestId", requestId == null ? "" : requestId);
        wrapper.put("type", type);
        wrapper.put("loggedAt", LocalDateTime.now());
        wrapper.put("payload", payload == null ? Map.of() : payload);
        try {
            Files.createDirectories(outputDir);
            Files.writeString(file, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(wrapper), StandardCharsets.UTF_8);
            return file.toString();
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "信任评估日志文件写入失败");
        }
    }
}
