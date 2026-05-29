package com.hmiot.hmiot_backend.module.device.vo;

public record IotDeviceHealthResponse(boolean healthy, String targetSystem, String modelVersion) {
}
