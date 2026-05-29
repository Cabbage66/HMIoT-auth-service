package com.hmiot.hmiot_backend.module.policy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("auth_policy")
public class AuthPolicy {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String policyCode;
    private String policyName;
    private String sceneType;
    private Long appResourceId;
    private String userScope;
    private String deviceScope;
    private String authChain;
    private Integer minTrustLevel;
    private Integer sessionTtlSeconds;
    private String actionOnFail;
    private String status;
    private Integer versionNo;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPolicyCode() {
        return policyCode;
    }

    public void setPolicyCode(String policyCode) {
        this.policyCode = policyCode;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public String getSceneType() {
        return sceneType;
    }

    public void setSceneType(String sceneType) {
        this.sceneType = sceneType;
    }

    public Long getAppResourceId() {
        return appResourceId;
    }

    public void setAppResourceId(Long appResourceId) {
        this.appResourceId = appResourceId;
    }

    public String getUserScope() {
        return userScope;
    }

    public void setUserScope(String userScope) {
        this.userScope = userScope;
    }

    public String getDeviceScope() {
        return deviceScope;
    }

    public void setDeviceScope(String deviceScope) {
        this.deviceScope = deviceScope;
    }

    public String getAuthChain() {
        return authChain;
    }

    public void setAuthChain(String authChain) {
        this.authChain = authChain;
    }

    public Integer getMinTrustLevel() {
        return minTrustLevel;
    }

    public void setMinTrustLevel(Integer minTrustLevel) {
        this.minTrustLevel = minTrustLevel;
    }

    public Integer getSessionTtlSeconds() {
        return sessionTtlSeconds;
    }

    public void setSessionTtlSeconds(Integer sessionTtlSeconds) {
        this.sessionTtlSeconds = sessionTtlSeconds;
    }

    public String getActionOnFail() {
        return actionOnFail;
    }

    public void setActionOnFail(String actionOnFail) {
        this.actionOnFail = actionOnFail;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
