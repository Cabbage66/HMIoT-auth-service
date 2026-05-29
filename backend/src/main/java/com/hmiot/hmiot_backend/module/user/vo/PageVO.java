package com.hmiot.hmiot_backend.module.user.vo;

import java.util.List;

public record PageVO<T>(long current, long size, long total, List<T> records) {
}
