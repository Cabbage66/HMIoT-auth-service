package com.hmiot.hmiot_backend.module.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmiot.hmiot_backend.module.device.entity.IotDeviceStatus;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IotDeviceStatusMapper extends BaseMapper<IotDeviceStatus> {
}
