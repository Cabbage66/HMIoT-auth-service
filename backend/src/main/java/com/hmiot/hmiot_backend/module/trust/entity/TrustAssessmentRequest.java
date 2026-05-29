package com.hmiot.hmiot_backend.module.trust.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("trust_assessment_request")
public class TrustAssessmentRequest {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String requestId;
    private Long userId;
    private Long terminalId;
    private Long appResourceId;
    private String modelSource;
    private String modelVersion;
    private String inputDigest;
    private String inputPayload;
    private LocalDateTime requestTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(Long terminalId) {
        this.terminalId = terminalId;
    }

    public Long getAppResourceId() {
        return appResourceId;
    }

    public void setAppResourceId(Long appResourceId) {
        this.appResourceId = appResourceId;
    }

    public String getModelSource() {
        return modelSource;
    }

    public void setModelSource(String modelSource) {
        this.modelSource = modelSource;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public String getInputDigest() {
        return inputDigest;
    }

    public void setInputDigest(String inputDigest) {
        this.inputDigest = inputDigest;
    }

    public String getInputPayload() {
        return inputPayload;
    }

    public void setInputPayload(String inputPayload) {
        this.inputPayload = inputPayload;
    }

    public LocalDateTime getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(LocalDateTime requestTime) {
        this.requestTime = requestTime;
    }
}
