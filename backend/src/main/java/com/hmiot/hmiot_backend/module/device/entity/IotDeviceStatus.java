package com.hmiot.hmiot_backend.module.device.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("iot_device_status")
public class IotDeviceStatus {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long deviceEntityId;
    private String connectStatus;
    private String trustStatus;
    private LocalDateTime lastReportAt;
    private String lastPayload;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDeviceEntityId() {
        return deviceEntityId;
    }

    public void setDeviceEntityId(Long deviceEntityId) {
        this.deviceEntityId = deviceEntityId;
    }

    public String getConnectStatus() {
        return connectStatus;
    }

    public void setConnectStatus(String connectStatus) {
        this.connectStatus = connectStatus;
    }

    public String getTrustStatus() {
        return trustStatus;
    }

    public void setTrustStatus(String trustStatus) {
        this.trustStatus = trustStatus;
    }

    public LocalDateTime getLastReportAt() {
        return lastReportAt;
    }

    public void setLastReportAt(LocalDateTime lastReportAt) {
        this.lastReportAt = lastReportAt;
    }

    public String getLastPayload() {
        return lastPayload;
    }

    public void setLastPayload(String lastPayload) {
        this.lastPayload = lastPayload;
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
