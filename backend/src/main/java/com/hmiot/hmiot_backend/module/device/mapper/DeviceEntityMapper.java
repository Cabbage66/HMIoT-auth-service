package com.hmiot.hmiot_backend.module.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmiot.hmiot_backend.module.device.entity.DeviceEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DeviceEntityMapper extends BaseMapper<DeviceEntity> {
}
